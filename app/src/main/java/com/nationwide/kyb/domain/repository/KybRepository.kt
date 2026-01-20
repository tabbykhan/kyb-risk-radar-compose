package com.nationwide.kyb.domain.repository

import com.nationwide.kyb.domain.model.KybRunResult
import com.nationwide.kyb.domain.model.RecentKybCheck

/**
 * Repository interface for KYB data operations
 * - Handles all data layer logic (API, database, cache)
 * - Returns Result types (no null, no exceptions thrown)
 * - Handles all error mapping
 */
interface KybRepository {
    /**
     * Start Risk Scan for customer
     * @param customerId The customer ID
     * @param correlationId Correlation ID for tracing
     * @return Result containing KybRunResult or error
     */
    suspend fun runKybCheck(customerId: String, correlationId: String): Result<KybRunResult>
    suspend fun getAllCustomers(): List<String>
    suspend fun saveRecentCheck(recentCheck: RecentKybCheck)
    suspend fun getRecentChecks(): List<RecentKybCheck>
    suspend fun getCachedKybResult(): KybRunResult?
}
