package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.di.ServiceLocator

class OptimizationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("OptimizationWorker", "NetPulse background observation worker started")
        
        return try {
            val captured = ServiceLocator.getRepository(applicationContext).captureBackgroundObservation()
            Log.d("OptimizationWorker", if (captured) "Stored a valid cached/location observation" else "No valid cached/location observation available")
            Result.success()
        } catch (e: Exception) {
            Log.e("OptimizationWorker", "Background optimization failed", e)
            Result.retry()
        }
    }
}
