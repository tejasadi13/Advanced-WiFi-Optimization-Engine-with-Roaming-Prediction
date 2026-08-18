package com.example.domain.service

import com.example.domain.model.SpeedTestPhase
import com.example.domain.model.SpeedTestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class SpeedTestService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .build()
) {
    suspend fun run(
        networkName: String,
        onUpdate: (SpeedTestPhase, Float, Double?, Double?, Double?, Double?) -> Unit
    ): SpeedTestResult = withContext(Dispatchers.IO) {
        val startedAt = System.nanoTime()
        onUpdate(SpeedTestPhase.CONNECTING, 0f, null, null, null, null)
        val pingSamples = measurePing(onUpdate)
        if (pingSamples.isEmpty()) error("Unable to measure network latency")
        val ping = pingSamples.average()
        val jitter = if (pingSamples.size > 1) {
            pingSamples.zipWithNext().map { abs(it.second - it.first) }.average()
        } else {
            0.0
        }

        val download = measureDownload(onUpdate)
        val upload = measureUpload(onUpdate)
        val duration = (System.nanoTime() - startedAt) / NANOS_PER_MILLISECOND
        val timestamp = System.currentTimeMillis()
        onUpdate(SpeedTestPhase.COMPLETED, 1f, download, upload, ping, jitter)
        SpeedTestResult(networkName, download, upload, ping, jitter, duration, timestamp)
    }

    private suspend fun measurePing(onUpdate: (SpeedTestPhase, Float, Double?, Double?, Double?, Double?) -> Unit): List<Double> {
        onUpdate(SpeedTestPhase.PING, 0f, null, null, null, null)
        val samples = mutableListOf<Double>()
        repeat(PING_SAMPLE_COUNT) { index ->
            val started = System.nanoTime()
            val request = Request.Builder()
                .url(PING_URL)
                .header("Cache-Control", "no-cache")
                .get()
                .build()
            executeCancellable(request).use { response ->
                if (!response.isSuccessful) error("Latency endpoint returned HTTP ${response.code}")
                response.body?.byteStream()?.use { it.readBytes() }
            }
            val elapsed = (System.nanoTime() - started) / NANOS_PER_MILLISECOND.toDouble()
            samples += elapsed
            onUpdate(SpeedTestPhase.PING, (index + 1f) / PING_SAMPLE_COUNT, null, null, samples.average(), null)
        }
        return samples
    }

    private suspend fun measureDownload(onUpdate: (SpeedTestPhase, Float, Double?, Double?, Double?, Double?) -> Unit): Double {
        val started = System.nanoTime()
        var bytesRead = 0L
        val request = Request.Builder()
            .url("$DOWNLOAD_URL?bytes=$TRANSFER_BYTES")
            .header("Cache-Control", "no-cache")
            .get()
            .build()
        executeCancellable(request).use { response ->
            if (!response.isSuccessful) error("Download endpoint returned HTTP ${response.code}")
            val body = response.body ?: error("Download response was empty")
            body.byteStream().use { input ->
                val buffer = ByteArray(BUFFER_SIZE)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    bytesRead += read
                    val progress = (bytesRead.toFloat() / TRANSFER_BYTES).coerceIn(0f, 1f)
                    onUpdate(SpeedTestPhase.DOWNLOAD, progress, bytesToMbps(bytesRead, started), null, null, null)
                }
            }
        }
        return bytesToMbps(bytesRead, started)
    }

    private suspend fun measureUpload(onUpdate: (SpeedTestPhase, Float, Double?, Double?, Double?, Double?) -> Unit): Double {
        val payload = ByteArray(UPLOAD_BYTES)
        val started = System.nanoTime()
        val request = Request.Builder()
            .url(UPLOAD_URL)
            .header("Cache-Control", "no-cache")
            .post(payload.toRequestBody("application/octet-stream".toMediaType()))
            .build()
        onUpdate(SpeedTestPhase.UPLOAD, 0f, null, null, null, null)
        executeCancellable(request).use { response ->
            if (!response.isSuccessful) error("Upload endpoint returned HTTP ${response.code}")
            response.body?.close()
        }
        val speed = bytesToMbps(UPLOAD_BYTES.toLong(), started)
        onUpdate(SpeedTestPhase.UPLOAD, 1f, null, speed, null, null)
        return speed
    }

    private fun bytesToMbps(bytes: Long, started: Long): Double {
        val elapsedSeconds = max((System.nanoTime() - started) / 1_000_000_000.0, MINIMUM_ELAPSED_SECONDS)
        return (bytes * 8.0 / elapsedSeconds / 1_000_000.0).coerceAtLeast(0.0)
    }

    private suspend fun executeCancellable(request: Request): Response = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, exception: java.io.IOException) {
                if (continuation.isActive) continuation.resumeWithException(exception)
            }

            override fun onResponse(call: Call, response: Response) {
                if (continuation.isActive) continuation.resume(response) else response.close()
            }
        })
    }

    private companion object {
        const val PING_SAMPLE_COUNT = 3
        const val TRANSFER_BYTES = 5_000_000
        const val UPLOAD_BYTES = 1_000_000
        const val BUFFER_SIZE = 16 * 1024
        const val MINIMUM_ELAPSED_SECONDS = 0.001
        const val NANOS_PER_MILLISECOND = 1_000_000L
        const val PING_URL = "https://speed.cloudflare.com/cdn-cgi/trace"
        const val DOWNLOAD_URL = "https://speed.cloudflare.com/__down"
        const val UPLOAD_URL = "https://speed.cloudflare.com/__up"
    }
}
