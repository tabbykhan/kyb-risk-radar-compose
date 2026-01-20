package com.nationwide.kyb.domain.model

import com.google.gson.annotations.SerializedName

/**
 * Result model from KYB API /kyb/mcp/run/{customerId}
 * This is the API response structure
 */
data class KybRunResult(
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
