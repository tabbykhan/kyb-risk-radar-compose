package com.nationwide.kyb.feature.summary

import androidx.lifecycle.viewModelScope
import com.nationwide.kyb.core.ui.BaseViewModel
import com.nationwide.kyb.core.utils.Logger
import com.nationwide.kyb.domain.model.UiState
import com.nationwide.kyb.domain.repository.KybRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Customer Detail Summary tab
 */
class SummaryViewModel(
    private val repository: KybRepository,
    private val customerId: String,
    private val correlationId: String
) : BaseViewModel() {
    
    private val _uiState = MutableStateFlow<UiState<com.nationwide.kyb.domain.model.KybData>>(UiState.Loading)
    val uiState: StateFlow<UiState<com.nationwide.kyb.domain.model.KybData>> = _uiState.asStateFlow()
    
    init {
        loadKybData()
    }
    
    private fun loadKybData() {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                
                val kybData = repository.getKybData(customerId, correlationId)
                
                if (kybData != null) {
                    _uiState.value = UiState.Success(kybData)
                    
                    Logger.logEvent(
                        eventName = "CUSTOMER_DETAIL_LOADED",
                        correlationId = correlationId,
                        customerId = customerId,
                        screenName = "CustomerDetail"
                    )
                } else {
                    _uiState.value = UiState.Error("Failed to load KYB data")
                    Logger.logError(
                        eventName = "CUSTOMER_DETAIL_LOAD_FAILED",
                        error = Exception("KYB data is null"),
                        correlationId = correlationId,
                        customerId = customerId,
                        screenName = "CustomerDetail"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
                Logger.logError(
                    eventName = "CUSTOMER_DETAIL_LOAD_ERROR",
                    error = e,
                    correlationId = correlationId,
                    customerId = customerId,
                    screenName = "CustomerDetail"
                )
            }
        }
    }
}
