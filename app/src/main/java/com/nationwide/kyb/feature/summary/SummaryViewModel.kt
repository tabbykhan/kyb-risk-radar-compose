package com.nationwide.kyb.feature.summary

import androidx.lifecycle.viewModelScope
import com.nationwide.kyb.core.ui.BaseViewModel
import com.nationwide.kyb.core.utils.Logger
import com.nationwide.kyb.domain.model.KybRunResult
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

    private val _uiState =
        MutableStateFlow<UiState<KybRunResult>>(UiState.Loading)

    val uiState = _uiState.asStateFlow()

    init {
        loadSummary()
    }

    private fun loadSummary() {
        viewModelScope.launch {

            val result = repository.getCachedKybResult()

            if (result != null) {
                _uiState.value = UiState.Success(result)

                Logger.logEvent(
                    eventName = "SUMMARY_LOADED",
                    correlationId = correlationId,
                    customerId = customerId
                )
            } else {
                _uiState.value = UiState.Error("KYB result not available")
            }
        }
    }
}

