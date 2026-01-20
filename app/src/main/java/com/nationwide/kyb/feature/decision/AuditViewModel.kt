package com.nationwide.kyb.feature.decision

import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
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

    private val _uiState =
        MutableStateFlow<UiState<String>>(UiState.Loading)
    val uiState: StateFlow<UiState<String>> = _uiState.asStateFlow()

    init {
        loadAuditJson()
    }

    private fun loadAuditJson() {
        viewModelScope.launch {
            try {
                val result = repository.getCachedKybResult()

                if (result != null) {
                    _uiState.value =
                        UiState.Success(
                            GsonBuilder()
                                .setPrettyPrinting()
                                .create()
                                .toJson(result)
                        )
                } else {
                    _uiState.value = UiState.Error("No audit data found")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Audit load failed")
            }
        }
    }
}

