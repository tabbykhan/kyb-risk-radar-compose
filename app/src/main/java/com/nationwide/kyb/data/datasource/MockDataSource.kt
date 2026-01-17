package com.nationwide.kyb.data.datasource

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.FieldNamingPolicy
import com.nationwide.kyb.core.utils.Logger
import com.nationwide.kyb.data.repository.RiskBandDeserializer
import com.nationwide.kyb.domain.model.KybData
import com.nationwide.kyb.domain.model.RiskBand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * Mock data source that loads JSON from assets
 */
class MockDataSource(private val assetsInputStream: InputStream) {
    private val gson: Gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(RiskBand::class.java, RiskBandDeserializer())
        .create()
    
    // Cache the JSON string to avoid re-reading the stream
    private var cachedJsonString: String? = null
    
    private suspend fun getJsonString(): String = withContext(Dispatchers.IO) {
        if (cachedJsonString == null) {
            cachedJsonString = assetsInputStream.bufferedReader().use { it.readText() }
        }
        cachedJsonString ?: ""
    }
    
    suspend fun loadKybData(customerId: String, correlationId: String): KybData? = withContext(Dispatchers.IO) {
        try {
            val jsonString = getJsonString()
            if (jsonString.isEmpty()) {
                Logger.logError(
                    eventName = "KYB_DATA_LOAD_FAILED",
                    error = Exception("Empty JSON string"),
                    correlationId = correlationId,
                    customerId = customerId
                )
                return@withContext null
            }
            
            val kybData = gson.fromJson(jsonString, KybData::class.java)
            
            // Log the data load
            Logger.logEvent(
                eventName = "KYB_DATA_LOADED",
                correlationId = correlationId,
                customerId = customerId,
                additionalData = mapOf(
                    "riskBand" to kybData.riskAssessment.riskBand.name,
                    "score" to kybData.riskAssessment.score.toString()
                )
            )
            
            kybData
        } catch (e: Exception) {
            Logger.logError(
                eventName = "KYB_DATA_LOAD_FAILED",
                error = e,
                correlationId = correlationId,
                customerId = customerId
            )
            null
        }
    }
}
