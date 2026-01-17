package com.nationwide.kyb.feature.decision

import androidx.lifecycle.viewModelScope
import com.nationwide.kyb.core.ui.BaseViewModel
import com.nationwide.kyb.domain.model.UiState
import com.nationwide.kyb.domain.repository.KybRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Audit (JSON) tab
 */
class AuditViewModel(
    private val repository: KybRepository,
    private val customerId: String,
    private val correlationId: String
) : BaseViewModel() {
    
    private val _uiState = MutableStateFlow<UiState<String>>(UiState.Loading)
    val uiState: StateFlow<UiState<String>> = _uiState.asStateFlow()
    
    init {
        loadKybDataJson()
    }
    
    private fun loadKybDataJson() {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                
                val kybData = repository.getKybData(customerId, correlationId)
                
                if (kybData != null) {
                    // Convert to JSON string (pretty printed)
                    val json = com.google.gson.GsonBuilder()
                        .setPrettyPrinting()
                        .create()
                        .toJson(kybData)
                    
                    _uiState.value = UiState.Success(json)
                } else {
                    _uiState.value = UiState.Error("Failed to load KYB data")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
