package com.example.domain.model

enum class RecommendationCategory { CONNECTIVITY, PERFORMANCE, ROAMING, SECURITY, OPTIMIZATION, CONGESTION }
enum class RecommendationPriority { CRITICAL, HIGH, MEDIUM, LOW }
enum class RecommendationSeverity { CRITICAL, WARNING, INFO, POSITIVE }

data class NetworkRecommendation(
    val id: String,
    val title: String,
    val description: String,
    val priority: RecommendationPriority,
    val severity: RecommendationSeverity,
    val category: RecommendationCategory,
    val confidence: Int,
    val timestamp: Long,
    val action: String,
    val expectedBenefit: String,
    val explanation: NetworkDecisionExplanation? = null
)
