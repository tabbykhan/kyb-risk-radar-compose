package com.nationwide.kyb.core.utils

import android.util.Log

/**
 * Structured logger for KYB app
 * Key-value based logging with eventName, correlationId, customerId, screenName
 * No logging from Composables - only from ViewModels, Repositories, UseCases
 */
object Logger {
    private const val TAG = "KYB_APP"
    
    /**
     * Log a structured event
     * @param eventName Name of the event (e.g., "CUSTOMER_SELECTED", "KYB_RUN_STARTED")
     * @param correlationId Optional correlation ID for tracing
     * @param customerId Optional customer ID
     * @param screenName Optional screen name where event occurred
     * @param additionalData Optional key-value pairs for additional context
     */
    fun logEvent(
        eventName: String,
        correlationId: String? = null,
        customerId: String? = null,
        screenName: String? = null,
        additionalData: Map<String, String>? = null
    ) {
        val logData = buildString {
            append("eventName=$eventName")
            correlationId?.let { append(", correlationId=$it") }
            customerId?.let { append(", customerId=$it") }
            screenName?.let { append(", screenName=$it") }
            additionalData?.forEach { (key, value) ->
                append(", $key=$value")
            }
        }
        Log.d(TAG, logData)
    }
    
   fun logError(
    eventName: String,
    error: Throwable,
    correlationId: String? = null,
    customerId: String? = null,
    screenName: String? = null,
    additionalData: Map<String, String>? = null
) {
    val logData = buildString {
        append("eventName=$eventName")
        correlationId?.let { append(", correlationId=$it") }
        customerId?.let { append(", customerId=$it") }
        screenName?.let { append(", screenName=$it") }
        additionalData?.forEach { (key, value) ->
            append(", $key=$value")
        }
        append(", error=${error.message}")
    }
    Log.e(TAG, logData, error)
}
}
