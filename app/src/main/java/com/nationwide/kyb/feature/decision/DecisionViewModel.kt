package com.nationwide.kyb.feature.decision

import androidx.lifecycle.viewModelScope
import com.nationwide.kyb.core.ui.BaseViewModel
import com.nationwide.kyb.core.utils.Logger
import com.nationwide.kyb.domain.model.RiskBand
import com.nationwide.kyb.domain.model.UiState
import com.nationwide.kyb.domain.repository.KybRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for RM Decision tab
 * Manages override state and comments
 */
class DecisionViewModel(
    private val repository: KybRepository,
    private val customerId: String,
    private val correlationId: String
) : BaseViewModel() {
    
    private val _uiState = MutableStateFlow<UiState<com.nationwide.kyb.domain.model.KybData>>(UiState.Loading)
    val uiState: StateFlow<UiState<com.nationwide.kyb.domain.model.KybData>> = _uiState.asStateFlow()
    
    private val _rmOverride = MutableStateFlow<RiskBand?>(null)
    val rmOverride: StateFlow<RiskBand?> = _rmOverride.asStateFlow()
    
    private val _rmComments = MutableStateFlow<String>("")
    val rmComments: StateFlow<String> = _rmComments.asStateFlow()
    
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
                } else {
                    _uiState.value = UiState.Error("Failed to load KYB data")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun updateRmOverride(riskBand: RiskBand) {
        _rmOverride.value = riskBand
        
        Logger.logEvent(
            eventName = "RM_OVERRIDE_UPDATED",
            correlationId = correlationId,
            customerId = customerId,
            screenName = "RMDecision",
            additionalData = mapOf("override" to riskBand.name)
        )
    }
    
    fun updateRmComments(comments: String) {
        _rmComments.value = comments
    }
}
