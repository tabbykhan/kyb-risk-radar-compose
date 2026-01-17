package com.nationwide.kyb.domain.model

import com.google.gson.annotations.SerializedName

/**
 * Domain models for KYB data
 * Based on mock JSON structure from /mock/data.json
 */

data class KybData(
    @SerializedName("_audit_trail")
    val auditTrail: AuditTrail,
    val transactionInsights: TransactionInsights,
    val recommendedActions: List<String>,
    val kybNote: String,
    val riskAssessment: RiskAssessment,
    val entityProfile: EntityProfile,
    val groupContext: String?,
    val journeyType: String,
    val partySummary: PartySummary,
    val organizationStructure: String,
    val companiesHouse: CompaniesHouse,
    val sentimentAnalysis: SentimentAnalysis
)

data class AuditTrail(
    val agentsCalled: List<String>,
    val customerId: String,
    val timestamp: String
)

data class TransactionInsights(
    val summary: String,
    val candidateTriggers: List<String>,
    val supportingMetrics: SupportingMetrics
)

data class SupportingMetrics(
    val latestPeriod: String,
    val highRiskCountrySharePct: Int,
    val intlOutwardChangePct: Int,
    val cashDepositRatioPct: Int,
    val periodCoveredMonths: Int
)

data class RiskAssessment(
    val score: Int,
    val riskBand: RiskBand,
    val scoreBreakdown: ScoreBreakdown,
    val triggersFired: List<TriggerFired>,
    val overallReasoning: String,
    val journeyType: String
)

enum class RiskBand {
    RED, AMBER, GREEN
}

data class ScoreBreakdown(
    val triggerImpacts: List<TriggerImpact>,
    val baseScore: Int
)

data class TriggerImpact(
    val code: String,
    val delta: Int
)

data class TriggerFired(
    val severity: String,
    val reason: String,
    val code: String
)

data class EntityProfile(
    val customerId: String,
    val legalName: String,
    val sector: String,
    val countryOfIncorporation: String,
    val primaryOperatingCountry: String,
    val onboardingDate: String,
    val kybStatus: String,
    val turnoverBandInr: String,
    val kybLastReviewDate: String,
    val kybLastReviewOutcome: String,
    val journeyType: String
)

data class PartySummary(
    val parties: List<Party>,
    val keyObservations: String
)

data class Party(
    val role: String,
    val partyId: String,
    val riskLabel: String,
    val name: String,
    val keyFlags: List<String>
)

data class CompaniesHouse(
    val customerId: String,
    val error: String?,
    val message: String?
)

data class SentimentAnalysis(
    val analyzedTweetCount: Int,
    val sentimentSummary: SentimentSummary,
    val topic: String,
    val customerId: String,
    val status: String
)

data class SentimentSummary(
    val positive: Int,
    val neutral: Int,
    val negative: Int,
    val total: Int
)

// Simplified models for UI

data class RecentKybCheck(
    val customerId: String,
    val customerName: String,
    val riskBand: RiskBand,
    val timestamp: Long,
    val correlationId: String
)

data class Customer(
    val customerId: String,
    val legalName: String
)
