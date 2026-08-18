package com.example.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.data.local.WifiDatabase
import com.example.data.repository.WifiRepositoryImpl
import com.example.data.util.WifiManagerHelper
import com.example.domain.repository.WifiRepository
import com.example.domain.service.AnalyzerService
import com.example.domain.service.RoamingPredictionService
import com.example.domain.service.SpeedTestService
import com.example.domain.service.RecommendationEngine
import com.example.domain.service.HeatmapService
import com.example.worker.BackgroundObservationScheduler
import com.example.domain.service.NetworkJourneyService
import com.example.ui.viewmodel.WifiWiseViewModel

/**
 * ServiceLocator provides basic dependency injection for the WiFiWise application.
 * This guarantees stable compile times and eliminates annotation processing version conflicts
 * while keeping the project structure clean, modular, and loosely coupled.
 */
object ServiceLocator {
    private var database: WifiDatabase? = null
    private var wifiManagerHelper: WifiManagerHelper? = null
    private var repository: WifiRepository? = null

    fun getDatabase(context: Context): WifiDatabase {
        return database ?: synchronized(this) {
            val db = WifiDatabase.getDatabase(context.applicationContext)
            database = db
            db
        }
    }

    fun getRepository(context: Context): WifiRepository {
        return repository ?: synchronized(this) {
            val helper = wifiManagerHelper ?: WifiManagerHelper(context.applicationContext).also {
                wifiManagerHelper = it
            }
            val repo = WifiRepositoryImpl(
                wifiDao = getDatabase(context).wifiDao,
                wifiManagerHelper = helper,
                analyzerService = AnalyzerService(),
                roamingPredictionService = RoamingPredictionService(),
                recommendationEngine = RecommendationEngine(),
                heatmapService = HeatmapService(),
                backgroundObservationScheduler = BackgroundObservationScheduler(context.applicationContext)
            )
            repository = repo
            repo
        }
    }

    /**
     * Provide ViewModel Factory to compose proper MVVM injections safely.
     */
    fun provideViewModelFactory(context: Context): ViewModelProvider.Factory {
        return object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                if (modelClass.isAssignableFrom(WifiWiseViewModel::class.java)) {
                    val repo = getRepository(context)
                    return WifiWiseViewModel(repo, SpeedTestService(), NetworkJourneyService()) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
