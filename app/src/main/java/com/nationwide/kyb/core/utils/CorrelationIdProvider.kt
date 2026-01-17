package com.nationwide.kyb.core.utils

import java.util.UUID

/**
 * Provider for generating correlation IDs (UUIDs) for tracing requests
 */
object CorrelationIdProvider {
    /**
     * Generate a new correlation ID (UUID)
     */
    fun generate(): String = UUID.randomUUID().toString()
}
