package com.example.domain.service

import com.example.domain.model.HeatmapObservation
import com.example.domain.model.HeatmapSummary
import com.example.domain.model.WifiNetwork

class HeatmapService {
    fun createObservation(network: WifiNetwork, latitude: Double, longitude: Double, timestamp: Long): HeatmapObservation =
        HeatmapObservation(timestamp = timestamp, latitude = latitude, longitude = longitude, ssid = network.ssid, bssid = network.bssid, rssi = network.rssi, frequency = network.frequency)

    fun summarize(observations: List<HeatmapObservation>, ssid: String? = null, bssid: String? = null): HeatmapSummary {
        val filtered = observations.filter { (ssid == null || it.ssid == ssid) && (bssid == null || it.bssid == bssid) }
        return HeatmapSummary(filtered, ssid, bssid, filtered.maxOfOrNull { it.rssi }, filtered.minOfOrNull { it.rssi }, filtered.takeIf { it.isNotEmpty() }?.map { it.rssi }?.average()?.toInt(), filtered.maxOfOrNull { it.timestamp })
    }

    fun shouldPersist(previous: HeatmapObservation?, next: HeatmapObservation): Boolean {
        if (previous == null) return true
        return previous.bssid != next.bssid || previous.latitude != next.latitude || previous.longitude != next.longitude || kotlin.math.abs(previous.rssi - next.rssi) >= 3 || next.timestamp - previous.timestamp >= 60_000
    }
}
