package com.nationwide.kyb.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nationwide.kyb.domain.model.RecentKybCheck
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore manager for local persistence
 * Handles:
 * - Selected customer ID
 * - Recent KYB checks list
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kyb_preferences")

class DataStoreManager(private val context: Context) {
    private val gson = Gson()
    
    companion object {
        private val SELECTED_CUSTOMER_ID_KEY = stringPreferencesKey("selected_customer_id")
        private val RECENT_CHECKS_KEY = stringPreferencesKey("recent_checks")
    }
    
    // Selected Customer ID
    val selectedCustomerId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[SELECTED_CUSTOMER_ID_KEY]
    }
    
    suspend fun saveSelectedCustomerId(customerId: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_CUSTOMER_ID_KEY] = customerId
        }
    }
    
    suspend fun clearSelectedCustomerId() {
        context.dataStore.edit { preferences ->
            preferences.remove(SELECTED_CUSTOMER_ID_KEY)
        }
    }
    
    // Recent Checks
    val recentChecks: Flow<List<RecentKybCheck>> = context.dataStore.data.map { preferences ->
        val json = preferences[RECENT_CHECKS_KEY]
        if (json.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                val type = object : TypeToken<List<RecentKybCheck>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    suspend fun saveRecentCheck(recentCheck: RecentKybCheck) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[RECENT_CHECKS_KEY]
            val currentList = if (currentJson.isNullOrBlank()) {
                emptyList<RecentKybCheck>()
            } else {
                try {
                    val type = object : TypeToken<List<RecentKybCheck>>() {}.type
                    gson.fromJson(currentJson, type) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            }
            
            // Add new check at the beginning and keep only last 10
            val updatedList = (listOf(recentCheck) + currentList).take(10)
            preferences[RECENT_CHECKS_KEY] = gson.toJson(updatedList)
        }
    }
    
    suspend fun clearRecentChecks() {
        context.dataStore.edit { preferences ->
            preferences.remove(RECENT_CHECKS_KEY)
        }
    }
}
