package com.example.domain.model

enum class SpeedTestPhase {
    IDLE,
    CONNECTING,
    PING,
    DOWNLOAD,
    UPLOAD,
    COMPLETED,
    FAILED
}

data class SpeedTestResult(
    val networkName: String,
    val downloadMbps: Double,
    val uploadMbps: Double,
    val pingMs: Double,
    val jitterMs: Double,
    val durationMs: Long,
    val timestamp: Long
)

data class SpeedTestState(
    val phase: SpeedTestPhase = SpeedTestPhase.IDLE,
    val progress: Float = 0f,
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null,
    val pingMs: Double? = null,
    val jitterMs: Double? = null,
    val durationMs: Long? = null,
    val networkName: String? = null,
    val timestamp: Long? = null,
    val result: SpeedTestResult? = null,
    val errorMessage: String? = null
)
