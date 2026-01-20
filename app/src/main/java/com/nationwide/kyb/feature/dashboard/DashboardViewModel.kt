package com.nationwide.kyb.feature.dashboard

import androidx.lifecycle.viewModelScope
import com.nationwide.kyb.core.ui.BaseViewModel
import com.nationwide.kyb.core.utils.CorrelationIdProvider
import com.nationwide.kyb.core.utils.Logger
import com.nationwide.kyb.data.local.DataStoreManager
import com.nationwide.kyb.domain.model.RecentKybCheck
import com.nationwide.kyb.domain.model.RiskBand
import com.nationwide.kyb.domain.model.UiState
import com.nationwide.kyb.domain.model.WorkflowStep
import com.nationwide.kyb.domain.repository.KybRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ViewModel for Dashboard screen
 * Handles:
 * - Customer selection
 * - KYB workflow simulation
 * - Recent checks management
 *
 * IMPORTANT: API call triggers ONLY on "Start Risk Scan" button click
 * - NOT on init{}
 * - NOT on recomposition
 * - NOT on ViewModel recreation
 */
class DashboardViewModel(
    private val repository: KybRepository,
    private val dataStoreManager: DataStoreManager
) : BaseViewModel() {
    
    private val _uiState = MutableStateFlow<UiState<DashboardUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<DashboardUiState>> = _uiState.asStateFlow()
    
    private val _selectedCustomerId = MutableStateFlow<String?>(null)
    val selectedCustomerId: StateFlow<String?> = _selectedCustomerId.asStateFlow()
    
    private val _recentChecks = MutableStateFlow<List<RecentKybCheck>>(emptyList())
    val recentChecks: StateFlow<List<RecentKybCheck>> = _recentChecks.asStateFlow()
    
    private val _workflowState = MutableStateFlow<WorkflowState>(WorkflowState.Idle)
    val workflowState: StateFlow<WorkflowState> = _workflowState.asStateFlow()
    
    private val _currentCorrelationId = MutableStateFlow<String?>(null)
    val currentCorrelationId: StateFlow<String?> = _currentCorrelationId.asStateFlow()
    
    private val _isRunButtonHidden = MutableStateFlow(false)
    val isRunButtonHidden: StateFlow<Boolean> = _isRunButtonHidden.asStateFlow()
    
    // Customer ID to Name mapping (for dropdown display)
    private val customerNameMap = mapOf(
        "CUST-0001" to "ABC Exports Private Limited",
        "CUST-0002" to "ABC Exports Private Limited",
        "CUST-0003" to "ABC Exports Private Limited"
    )
    
    // Load initial state - does NOT trigger API
    init {
        loadInitialState()
    }
    
    private fun loadInitialState() {
        viewModelScope.launch {
            try {
                // Reset selected customer on dashboard entry (don't load from DataStore)
                _selectedCustomerId.value = null
                
                // Load recent checks (only this persists)
                val checks = repository.getRecentChecks()
                _recentChecks.value = checks
                
                // Get available customers
                val customers = repository.getAllCustomers()
                
                _uiState.value = UiState.Success(
                    DashboardUiState(
                        availableCustomers = customers,
                        isFirstTime = checks.isEmpty(),
                        isRunButtonEnabled = false,
                        customerNameMap = customerNameMap
                    )
                )
                
                Logger.logEvent(
                    eventName = "DASHBOARD_LOADED",
                    customerId = _selectedCustomerId.value,
                    screenName = "Dashboard"
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load dashboard")
                Logger.logError(
                    eventName = "DASHBOARD_LOAD_FAILED",
                    error = e,
                    screenName = "Dashboard"
                )
            }
        }
    }
    
    fun selectCustomer(customerId: String) {
        viewModelScope.launch {
            try {
                // Only set selected customer, don't persist until Run KYB is triggered
                _selectedCustomerId.value = customerId
                
                _uiState.value = (_uiState.value as? UiState.Success)?.let { successState ->
                    UiState.Success(
                        successState.data.copy(isRunButtonEnabled = true)
                    )
                } ?: _uiState.value
                
                Logger.logEvent(
                    eventName = "CUSTOMER_SELECTED",
                    customerId = customerId,
                    screenName = "Dashboard"
                )
            } catch (e: Exception) {
                Logger.logError(
                    eventName = "CUSTOMER_SELECTION_FAILED",
                    error = e,
                    customerId = customerId,
                    screenName = "Dashboard"
                )
            }
        }
    }
    
    /**
     * Start Risk Scan - triggered ONLY by user button click
     * This method:
     * 1. Generates correlation ID
     * 2. Hides button and shows loader
     * 3. Calls repository.runKybCheck() via API
     * 4. Updates workflow state with simulated steps
     * 5. On success, completes workflow
     * 6. Navigation happens in Composable via LaunchedEffect
     */
    fun runKybCheck() {
        val customerId = _selectedCustomerId.value ?: return
        
        viewModelScope.launch {
            try {
                // Hide button and show loader
                _isRunButtonHidden.value = true
                
                // Persist selected customer
                dataStoreManager.saveSelectedCustomerId(customerId)
                
                // Generate correlation ID
                val correlationId = CorrelationIdProvider.generate()
                _currentCorrelationId.value = correlationId
                
                Logger.logEvent(
                    eventName = "KYB_RUN_STARTED",
                    correlationId = correlationId,
                    customerId = customerId,
                    screenName = "Dashboard"
                )
                
                // Start workflow simulation (make workflow visible)
                _workflowState.value = WorkflowState.Running(emptyList())
                
                // Simulate workflow steps (every 2 seconds)
                val steps = WorkflowStep.values().toList()
                steps.forEachIndexed { index, step ->
                    kotlinx.coroutines.delay(2000) // 2 seconds delay
                    
                    val completedSteps = steps.take(index + 1)
                    
                    // After all steps complete, show "Fetching results..."
                    val allStepsCompleted = completedSteps.size == steps.size
                    _workflowState.value = if (allStepsCompleted) {
                        WorkflowState.FetchingResults(completedSteps)
                    } else {
                        WorkflowState.Running(completedSteps)
                    }
                    
                    Logger.logEvent(
                        eventName = "WORKFLOW_STEP_COMPLETED",
                        correlationId = correlationId,
                        customerId = customerId,
                        screenName = "Dashboard",
                        additionalData = mapOf("step" to step.name)
                    )
                }
                
                // Call repository API via runKybCheck (Result type)
                val result = repository.runKybCheck(customerId, correlationId)

                result.onSuccess { kybRunResult ->
                    // Save recent check
                    val recentCheck = RecentKybCheck(
                        customerId = customerId,
                        customerName = kybRunResult.entityProfile.legalName,
                        riskBand = kybRunResult.riskAssessment.riskBand,
                        timestamp = System.currentTimeMillis(),
                        correlationId = correlationId
                    )
                    
                    repository.saveRecentCheck(recentCheck)
                    dataStoreManager.saveKybResult(kybRunResult)
                    
                    // Update recent checks list
                    val updatedChecks = repository.getRecentChecks()
                    _recentChecks.value = updatedChecks
                    
                    // Complete workflow with data
                    _workflowState.value = WorkflowState.Completed(
                        correlationId = correlationId,
                        riskBand = kybRunResult.riskAssessment.riskBand
                    )

                    Logger.logEvent(
                        eventName = "KYB_RUN_COMPLETED",
                        correlationId = correlationId,
                        customerId = customerId,
                        screenName = "Dashboard",
                        additionalData = mapOf("riskBand" to kybRunResult.riskAssessment.riskBand.name)
                    )
                }

                result.onFailure { error ->
                    _workflowState.value = WorkflowState.Error(
                        error.message ?: "Failed to load KYB data"
                    )
                    Logger.logError(
                        eventName = "KYB_DATA_FETCH_FAILED",
                        error = error,
                        correlationId = correlationId,
                        customerId = customerId,
                        screenName = "Dashboard"
                    )
                }
            } catch (e: Exception) {
                _workflowState.value = WorkflowState.Error(e.message ?: "Unknown error")
                Logger.logError(
                    eventName = "KYB_RUN_FAILED",
                    error = e,
                    customerId = customerId,
                    screenName = "Dashboard"
                )
            }
        }
    }
    
    fun resetWorkflow() {
        _workflowState.value = WorkflowState.Idle
        _currentCorrelationId.value = null
        _isRunButtonHidden.value = false
    }
}

data class DashboardUiState(
    val availableCustomers: List<String>,
    val isFirstTime: Boolean,
    val isRunButtonEnabled: Boolean,
    val customerNameMap: Map<String, String>
)

sealed class WorkflowState {
    object Idle : WorkflowState()
    data class Running(val completedSteps: List<WorkflowStep>) : WorkflowState()
    data class FetchingResults(val completedSteps: List<WorkflowStep>) : WorkflowState()
    data class Completed(val correlationId: String, val riskBand: RiskBand) : WorkflowState()
    data class Error(val message: String) : WorkflowState()
}


