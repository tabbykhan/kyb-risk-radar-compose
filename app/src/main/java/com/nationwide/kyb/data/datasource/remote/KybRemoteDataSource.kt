package com.nationwide.kyb.data.datasource.remote

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.nationwide.kyb.core.utils.Logger
import com.nationwide.kyb.data.repository.RiskBandDeserializer
import com.nationwide.kyb.domain.model.KybRunResult
import com.nationwide.kyb.domain.model.RiskBand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

/**
 * Retrofit API service interface for KYB operations
 */
interface KybApiService {
    @GET("/kyb/mcp/run/{customerId}")
    suspend fun runKybCheck(
        @Path("customerId") customerId: String,
        @Header("correlation-id") correlationId: String
    ): KybRunResult
}

/**
 * Remote data source that handles all API calls
 * - Creates and configures Retrofit client
 * - Makes API requests with correlation-id header
 * - Maps HTTP responses to domain models
 * - Never exposes exceptions; always returns Result type
 */
class KybRemoteDataSource(baseUrl: String = "http://10.0.2.2:8080") {
    private val gson: Gson = GsonBuilder()
        .setFieldNamingPolicy(com.google.gson.FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(RiskBand::class.java, RiskBandDeserializer())
        .create()

    private val apiService: KybApiService

    init {
        // Configure OkHttp with logging interceptor
        val httpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

        // Create Retrofit instance
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        apiService = retrofit.create(KybApiService::class.java)
    }

    /**
     * Start Risk Scan via API
     * @param customerId The customer ID to run KYB for
     * @param correlationId The correlation ID for tracing
     * @return Result containing KybRunResult or error message
     */
    suspend fun runKybCheck(customerId: String, correlationId: String): Result<KybRunResult> =
        withContext(Dispatchers.IO) {
            try {
                Logger.logEvent(
                    eventName = "API_KYB_RUN_REQUEST",
                    correlationId = correlationId,
                    customerId = customerId,
                    screenName = "RemoteDataSource"
                )

                val result = apiService.runKybCheck(customerId, correlationId)

                Logger.logEvent(
                    eventName = "API_KYB_RUN_SUCCESS",
                    correlationId = correlationId,
                    customerId = customerId,
                    screenName = "RemoteDataSource",
                    additionalData = mapOf(
                        "riskBand" to result.riskAssessment.riskBand.name,
                        "score" to result.riskAssessment.score.toString()
                    )
                )

                Result.success(result)
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Unknown error occurred"
                Logger.logError(
                    eventName = "API_KYB_RUN_FAILED",
                    error = e,
                    correlationId = correlationId,
                    customerId = customerId,
                    screenName = "RemoteDataSource"
                )
                Result.failure(e)
            }
        }
}
