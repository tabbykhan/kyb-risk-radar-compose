package com.nationwide.kyb.domain.model

/**
 * KYB Workflow steps for simulation
 */

enum class WorkflowStep(val displayName: String) {
    JOURNEY_CLASSIFIER("Journey Classifier"),
    ENTITY_PARTIES("Entity & Parties"),
    TRANSACTIONS_INSIGHTS("Transactions Insights"),
    COMPANIES_HOUSE("Companies House"),
    RISK_RULES("Risk & Rules"),
    KYB_SUMMARY_NOTE("KYB Summary Note")
}
