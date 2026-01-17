package com.nationwide.kyb.data.repository

import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.nationwide.kyb.domain.model.RiskBand
import java.lang.reflect.Type

/**
 * Custom Gson deserializer for mapping JSON to domain models
 * Handles enum conversion and nested structures
 */
class RiskBandDeserializer : JsonDeserializer<RiskBand> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): RiskBand {
        val riskBandString = json?.asString ?: "GREEN"
        return try {
            RiskBand.valueOf(riskBandString)
        } catch (e: Exception) {
            RiskBand.GREEN
        }
    }
}

/**
 * Creates a Gson instance configured for KYB data
 */
fun createKybGson(): Gson {
    return Gson()
        .newBuilder()
        .registerTypeAdapter(RiskBand::class.java, RiskBandDeserializer())
        .create()
}
