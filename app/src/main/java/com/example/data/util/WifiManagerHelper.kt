package com.example.data.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import android.location.LocationManager
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.domain.model.WifiNetwork
import java.net.InetAddress
import java.nio.ByteOrder

/**
 * Helper class that encapsulates Android WifiManager and ConnectivityManager operations.
 * Provides a clean API for retrieving hardware-level WiFi information.
 */
class WifiManagerHelper(private val context: Context) {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @Volatile
    private var callbackWifiInfo: WifiInfo? = null

    @Volatile
    private var callbackWifiNetwork: Network? = null

    private val wifiNetworkCallback: ConnectivityManager.NetworkCallback? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    val wifiInfo = networkCapabilities.transportInfo as? WifiInfo
                    if (wifiInfo != null) {
                        callbackWifiNetwork = network
                        callbackWifiInfo = wifiInfo
                        debug(
                            "Location-aware WifiInfo updated: ssid=${wifiInfo.ssid}, " +
                                "bssid=${wifiInfo.bssid}"
                        )
                    }
                }

                override fun onLost(network: Network) {
                    if (network == callbackWifiNetwork) {
                        callbackWifiNetwork = null
                        callbackWifiInfo = null
                        debug("Location-aware WiFi network lost")
                    }
                }
            }
        } else {
            null
        }

    init {
        wifiNetworkCallback?.let { callback ->
            val wifiRequest = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()

            try {
                connectivityManager.registerNetworkCallback(wifiRequest, callback)
            } catch (exception: SecurityException) {
                Log.w(
                    TAG,
                    "Unable to register location-aware WiFi callback; using fallback retrieval",
                    exception
                )
            }
        }
    }

    /**
     * Retrieves information about the currently connected WiFi network.
     * Note: This method requires ACCESS_FINE_LOCATION and ACCESS_WIFI_STATE permissions.
     * On Android 13+, it may also require NEARBY_WIFI_DEVICES.
     *
     * @return A WifiNetwork object representing the current connection, or null if not connected.
     */
    fun getConnectedWifiInfo(): WifiNetwork? {
        val wifiInfo = getCurrentWifiInfo()
        if (wifiInfo == null) {
            Log.w(TAG, "getConnectedWifiInfo() returning null: getCurrentWifiInfo() returned null")
            return null
        }

        debug("wifiInfo.ssid=${wifiInfo.ssid}")
        debug("wifiInfo.bssid=${wifiInfo.bssid}")
        debug("wifiInfo.rssi=${wifiInfo.rssi}")
        debug("wifiInfo.frequency=${wifiInfo.frequency}")
        debug("wifiInfo.linkSpeed=${wifiInfo.linkSpeed}")
        
        val reportedSsid = wifiInfo.ssid?.replace("\"", "")
        val ssid = if (
            reportedSsid.isNullOrBlank() ||
            reportedSsid == "<unknown ssid>" ||
            reportedSsid == "0x"
        ) {
            Log.w(
                TAG,
                "SSID is unavailable or redacted (ssid=$reportedSsid); using placeholder"
            )
            UNKNOWN_NETWORK_SSID
        } else {
            reportedSsid
        }

        val bssid = wifiInfo.bssid ?: "00:00:00:00:00:00"
        val rssi = wifiInfo.rssi
        val freq = wifiInfo.frequency
        val linkSpeed = wifiInfo.linkSpeed

        val network = WifiNetwork(
            ssid = ssid,
            bssid = bssid,
            rssi = rssi,
            frequency = freq,
            channel = getChannelFromFrequency(freq),
            securityType = getSecurityType(wifiInfo),
            isConnected = true,
            estimatedSpeedMbps = linkSpeed
        )
        debug("getConnectedWifiInfo() returning connected network: $network")
        return network
    }

    /**
     * Retrieves the local IP address assigned to the WiFi interface.
     * 
     * @return String representation of the IP address (e.g., "192.168.1.5")
     */
    fun getIpAddress(): String {
        @Suppress("DEPRECATION")
        val ipInt = wifiManager.connectionInfo.ipAddress
        if (ipInt == 0) return "0.0.0.0"
        
        return try {
            val addr = if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                Integer.reverseBytes(ipInt)
            } else {
                ipInt
            }
            val ipByteArray = byteArrayOf(
                (addr shr 24).toByte(),
                (addr shr 16).toByte(),
                (addr shr 8).toByte(),
                addr.toByte()
            )
            InetAddress.getByAddress(ipByteArray).hostAddress ?: "0.0.0.0"
        } catch (e: Exception) {
            "0.0.0.0"
        }
    }

    /** Returns only an already available system location; it never starts active location tracking. */
    fun getLastKnownLocation(): Pair<Double, Double>? {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return null
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isLocationEnabled) return null
        return try {
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            providers.asSequence().mapNotNull { provider -> locationManager.getLastKnownLocation(provider) }.maxByOrNull { it.time }?.let { it.latitude to it.longitude }
        } catch (_: SecurityException) { null }
    }

    /**
     * Returns the latest nearby access points discovered by the Android WiFi subsystem.
     */
    fun scanNearbyNetworks(): List<WifiNetwork> {
        val connectedNetwork = getConnectedWifiInfo()
        val connectedBssid = connectedNetwork?.bssid
            ?.takeUnless { it == REDACTED_BSSID || it == UNKNOWN_BSSID }

        return try {
            @Suppress("DEPRECATION")
            wifiManager.scanResults
                .map { scanResult ->
                    @Suppress("DEPRECATION")
                    val ssid = scanResult.SSID.ifBlank { HIDDEN_NETWORK_SSID }
                    val bssid = scanResult.BSSID ?: UNKNOWN_BSSID
                    // A redacted BSSID cannot identify one AP among several APs sharing an SSID.
                    // Leave the scan result unmarked rather than falsely marking every match connected.
                    val isConnected = connectedBssid?.let { bssid.equals(it, ignoreCase = true) } == true

                    WifiNetwork(
                        ssid = ssid,
                        bssid = bssid,
                        rssi = scanResult.level,
                        frequency = scanResult.frequency,
                        channel = getChannelFromFrequency(scanResult.frequency),
                        securityType = getSecurityType(scanResult),
                        isConnected = isConnected,
                        estimatedSpeedMbps = getEstimatedSpeed(scanResult.level)
                    )
                }
                .sortedWith(
                    compareByDescending<WifiNetwork> { it.isConnected }
                        .thenByDescending { it.rssi }
                )
                .also { networks ->
                    debug("scanNearbyNetworks() returned ${networks.size} networks")
                }
        } catch (exception: SecurityException) {
            Log.e(TAG, "scanNearbyNetworks() failed: WiFi scan permission unavailable", exception)
            throw exception
        }
    }

    private fun getCurrentWifiInfo(): WifiInfo? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            callbackWifiInfo?.let { wifiInfo ->
                debug("Using location-aware WifiInfo from NetworkCallback")
                return wifiInfo
            }

            debug("Location-aware WifiInfo not available; using synchronous fallback")
            val network = connectivityManager.activeNetwork
            debug("activeNetwork=$network")

            val capabilities = connectivityManager.getNetworkCapabilities(network)
            debug("NetworkCapabilities=$capabilities")

            val hasWifiTransport =
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            debug("hasTransport(TRANSPORT_WIFI)=$hasWifiTransport")

            val transportInfo = capabilities?.transportInfo
            debug("transportInfo=$transportInfo")

            if (hasWifiTransport) {
                val wifiInfo = transportInfo as? WifiInfo
                if (wifiInfo == null) {
                    Log.w(
                        TAG,
                        "getCurrentWifiInfo() returning null: WiFi transportInfo is not WifiInfo"
                    )
                }
                wifiInfo
            } else {
                Log.w(
                    TAG,
                    "getCurrentWifiInfo() returning null: active network does not have WiFi transport"
                )
                null
            }
        } else {
            @Suppress("DEPRECATION")
            wifiManager.connectionInfo.also { wifiInfo ->
                debug("Legacy wifiManager.connectionInfo=$wifiInfo")
            }
        }
    }

    private fun getSecurityType(info: WifiInfo): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            when (info.currentSecurityType) {
                WifiInfo.SECURITY_TYPE_OPEN -> "None"
                WifiInfo.SECURITY_TYPE_WEP -> "WEP"
                WifiInfo.SECURITY_TYPE_PSK -> "WPA2-PSK"
                WifiInfo.SECURITY_TYPE_EAP -> "WPA-Enterprise"
                WifiInfo.SECURITY_TYPE_SAE -> "WPA3-SAE"
                WifiInfo.SECURITY_TYPE_OWE -> "Enhanced Open"
                WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE -> "WPA3-Enterprise"
                else -> "Unknown"
            }
        } else {
            // Note: On older versions, security type isn't directly exposed via WifiInfo.
            // In the full implementation, we'd cross-reference BSSID with ScanResults.
            "Unknown"
        }
    }

    private fun getSecurityType(scanResult: ScanResult): String {
        val capabilities = scanResult.capabilities.uppercase()
        return when {
            "SUITE_B_192" in capabilities -> "WPA3-Enterprise"
            "EAP" in capabilities -> "WPA-Enterprise"
            "SAE" in capabilities -> "WPA3-SAE"
            "OWE" in capabilities -> "Enhanced Open"
            "WEP" in capabilities -> "WEP"
            "PSK" in capabilities -> "WPA2-PSK"
            capabilities.isBlank() || "ESS" in capabilities -> "None"
            else -> "Unknown"
        }
    }

    private fun getChannelFromFrequency(freq: Int): Int {
        return when {
            freq == 2484 -> 14
            freq in 2412..2472 -> (freq - 2412) / 5 + 1
            freq in 5180..5825 -> (freq - 5000) / 5
            freq in 5955..7115 -> (freq - 5950) / 5
            else -> 0
        }
    }

    private fun getEstimatedSpeed(rssi: Int): Int {
        return when {
            rssi >= -50 -> 866
            rssi >= -60 -> 650
            rssi >= -70 -> 433
            rssi >= -80 -> 144
            else -> 24
        }
    }

    private companion object {
        const val TAG = "WifiManagerHelper"
        const val UNKNOWN_NETWORK_SSID = "Unknown Network"
        const val HIDDEN_NETWORK_SSID = "Hidden Network"
        const val UNKNOWN_BSSID = "00:00:00:00:00:00"
        const val REDACTED_BSSID = "02:00:00:00:00:00"
    }

    private fun debug(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
    }
}
