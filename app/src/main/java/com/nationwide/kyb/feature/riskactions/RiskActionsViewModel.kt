package com.nationwide.kyb.feature.riskactions

import androidx.lifecycle.viewModelScope
import com.nationwide.kyb.core.ui.BaseViewModel
import com.nationwide.kyb.core.utils.Logger
import com.nationwide.kyb.domain.model.KybData
import com.nationwide.kyb.domain.model.UiState
import com.nationwide.kyb.domain.repository.KybRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Risk & Actions tab
 */
class RiskActionsViewModel(
    private val repository: KybRepository,
    private val customerId: String,
    private val correlationId: String
) : BaseViewModel() {

    private val _uiState =
        MutableStateFlow<UiState<KybData>>(UiState.Loading)
    val uiState: StateFlow<UiState<KybData>> = _uiState.asStateFlow()

    init {
        loadKybData()
    }

    private fun loadKybData() {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading

                val result = repository.getCachedKybResult()

                if (result != null) {
                    _uiState.value = UiState.Success(
                        KybData(
                            auditTrail = result.auditTrail,
                            transactionInsights = result.transactionInsights,
                            recommendedActions = result.recommendedActions,
                            kybNote = result.kybNote,
                            riskAssessment = result.riskAssessment,
                            entityProfile = result.entityProfile,
                            groupContext = result.groupContext,
                            journeyType = result.journeyType,
                            partySummary = result.partySummary,
                            organizationStructure = result.organizationStructure,
                            companiesHouse = result.companiesHouse,
                            sentimentAnalysis = result.sentimentAnalysis
                        )
                    )
                } else {
                    _uiState.value = UiState.Error("KYB data not available")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
