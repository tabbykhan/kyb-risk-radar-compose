package com.nationwide.kyb.data.repository

import com.nationwide.kyb.core.utils.Logger
import com.nationwide.kyb.data.datasource.MockDataSource
import com.nationwide.kyb.data.local.DataStoreManager
import com.nationwide.kyb.domain.model.KybData
import com.nationwide.kyb.domain.model.RecentKybCheck
import com.nationwide.kyb.domain.model.RiskBand
import com.nationwide.kyb.domain.repository.KybRepository
import kotlinx.coroutines.flow.first

/**
 * Implementation of KybRepository
 * Uses MockDataSource for JSON data and DataStoreManager for persistence
 */
class KybRepositoryImpl(
    private val mockDataSource: MockDataSource,
    private val dataStoreManager: DataStoreManager
) : KybRepository {
    
    override suspend fun getKybData(customerId: String, correlationId: String): KybData? {
        Logger.logEvent(
            eventName = "GET_KYB_DATA_REQUESTED",
            correlationId = correlationId,
            customerId = customerId,
            screenName = "Repository"
        )
        
        return mockDataSource.loadKybData(customerId, correlationId)
    }
    
    override suspend fun getAllCustomers(): List<String> {
        // For now, return hardcoded customer IDs
        // In real app, this would come from API/database
        return listOf("CUST-0001", "CUST-0002", "CUST-0003")
    }
    
    override suspend fun saveRecentCheck(recentCheck: RecentKybCheck) {
        Logger.logEvent(
            eventName = "SAVE_RECENT_CHECK",
            correlationId = recentCheck.correlationId,
            customerId = recentCheck.customerId,
            additionalData = mapOf("riskBand" to recentCheck.riskBand.name)
        )
        
        dataStoreManager.saveRecentCheck(recentCheck)
    }
    
    override suspend fun getRecentChecks(): List<RecentKybCheck> {
        return dataStoreManager.recentChecks.first()
    }
}
