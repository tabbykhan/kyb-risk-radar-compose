package com.nationwide.kyb.data.repository

import com.nationwide.kyb.core.utils.Logger
import com.nationwide.kyb.data.datasource.remote.KybRemoteDataSource
import com.nationwide.kyb.data.local.DataStoreManager
import com.nationwide.kyb.domain.model.KybRunResult
import com.nationwide.kyb.domain.model.RecentKybCheck
import com.nationwide.kyb.domain.repository.KybRepository
import kotlinx.coroutines.flow.first

/**
 * Implementation of KybRepository
 * Uses KybRemoteDataSource for API calls and DataStoreManager for persistence
 * All errors are handled here; repository never throws exceptions
 */
class KybRepositoryImpl(
    private val remoteDataSource: KybRemoteDataSource,
    private val dataStoreManager: DataStoreManager
) : KybRepository {

    private var cachedKybResult: KybRunResult? = null

    override suspend fun runKybCheck(customerId: String, correlationId: String): Result<KybRunResult> {

        val result = remoteDataSource.runKybCheck(customerId, correlationId)

        result.onSuccess {
            cachedKybResult = it // Cache the result on success
        }

        Logger.logEvent(
            eventName = "REPOSITORY_KYB_RUN_REQUESTED",
            correlationId = correlationId,
            customerId = customerId,
            screenName = "Repository"
        )
        
        // Call remote data source
        return result
    }
    
    override suspend fun getAllCustomers(): List<String> {
        return listOf("CUST-0001", "CUST-0002", "CUST-0003","CUST-0004")
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

    override suspend fun getCachedKybResult(): KybRunResult? {
        return dataStoreManager.getLastKybResult()
    }
}


