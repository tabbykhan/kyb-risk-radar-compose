package com.nationwide.kyb.domain.repository

import com.nationwide.kyb.domain.model.KybData
import com.nationwide.kyb.domain.model.RecentKybCheck

/**
 * Repository interface for KYB data operations
 */
interface KybRepository {
    suspend fun getKybData(customerId: String, correlationId: String): KybData?
    suspend fun getAllCustomers(): List<String>
    suspend fun saveRecentCheck(recentCheck: RecentKybCheck)
    suspend fun getRecentChecks(): List<RecentKybCheck>
}
