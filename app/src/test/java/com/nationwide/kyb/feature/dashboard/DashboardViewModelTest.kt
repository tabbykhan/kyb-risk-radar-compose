package com.nationwide.kyb.feature.dashboard

import com.nationwide.kyb.core.utils.CorrelationIdProvider
import com.nationwide.kyb.data.local.DataStoreManager
import com.nationwide.kyb.domain.model.RecentKybCheck
import com.nationwide.kyb.domain.model.RiskBand
import com.nationwide.kyb.domain.model.UiState
import com.nationwide.kyb.domain.model.WorkflowStep
import com.nationwide.kyb.domain.repository.KybRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockk.coEvery
import org.mockk.coVerify
import org.mockk.every
import org.mockk.mockk
import org.mockk.mockkObject

/**
 * Unit tests for DashboardViewModel
 */
class DashboardViewModelTest {
    
    private lateinit var repository: KybRepository
    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var viewModel: DashboardViewModel
    
    @Before
    fun setup() {
        repository = mockk()
        dataStoreManager = mockk()
        
        // Mock correlation ID provider
        mockkObject(CorrelationIdProvider)
        every { CorrelationIdProvider.generate() } returns "test-correlation-id"
    }
    
    @Test
    fun `initial state should load selected customer and recent checks`() = runTest {
        // Given
        val customerId = "CUST-0001"
        val recentChecks = listOf(
            RecentKybCheck(
                customerId = customerId,
                customerName = "Test Customer",
                riskBand = RiskBand.RED,
                timestamp = System.currentTimeMillis(),
                correlationId = "test-correlation-id"
            )
        )
        
        every { dataStoreManager.selectedCustomerId } returns flowOf(customerId)
        coEvery { repository.getRecentChecks() } returns recentChecks
        coEvery { repository.getAllCustomers() } returns listOf("CUST-0001", "CUST-0002")
        
        // When
        viewModel = DashboardViewModel(repository, dataStoreManager)
        
        // Wait for initial state to load
        kotlinx.coroutines.delay(100)
        
        // Then
        val uiState = viewModel.uiState.value
        assertTrue(uiState is UiState.Success)
        
        val successState = uiState as UiState.Success
        assertEquals(customerId, viewModel.selectedCustomerId.value)
        assertEquals(recentChecks, viewModel.recentChecks.value)
        assertFalse(successState.data.isFirstTime)
        assertTrue(successState.data.isRunButtonEnabled)
    }
    
    @Test
    fun `first time user should have empty state`() = runTest {
        // Given
        every { dataStoreManager.selectedCustomerId } returns flowOf(null)
        coEvery { repository.getRecentChecks() } returns emptyList()
        coEvery { repository.getAllCustomers() } returns listOf("CUST-0001", "CUST-0002")
        
        // When
        viewModel = DashboardViewModel(repository, dataStoreManager)
        
        // Wait for initial state to load
        kotlinx.coroutines.delay(100)
        
        // Then
        val uiState = viewModel.uiState.value
        assertTrue(uiState is UiState.Success)
        
        val successState = uiState as UiState.Success
        assertTrue(successState.data.isFirstTime)
        assertFalse(successState.data.isRunButtonEnabled)
        assertTrue(viewModel.recentChecks.value.isEmpty())
    }
    
    @Test
    fun `selecting customer should enable run button`() = runTest {
        // Given
        val customerId = "CUST-0001"
        every { dataStoreManager.selectedCustomerId } returns flowOf(null)
        coEvery { repository.getRecentChecks() } returns emptyList()
        coEvery { repository.getAllCustomers() } returns listOf("CUST-0001", "CUST-0002")
        coEvery { dataStoreManager.saveSelectedCustomerId(customerId) } returns Unit
        
        viewModel = DashboardViewModel(repository, dataStoreManager)
        kotlinx.coroutines.delay(100)
        
        // When
        viewModel.selectCustomer(customerId)
        kotlinx.coroutines.delay(100)
        
        // Then
        assertEquals(customerId, viewModel.selectedCustomerId.value)
        coVerify { dataStoreManager.saveSelectedCustomerId(customerId) }
    }
    
    @Test
    fun `workflow state should start as idle`() = runTest {
        // Given
        every { dataStoreManager.selectedCustomerId } returns flowOf(null)
        coEvery { repository.getRecentChecks() } returns emptyList()
        coEvery { repository.getAllCustomers() } returns listOf("CUST-0001")
        
        // When
        viewModel = DashboardViewModel(repository, dataStoreManager)
        kotlinx.coroutines.delay(100)
        
        // Then
        val workflowState = viewModel.workflowState.value
        assertTrue(workflowState is WorkflowState.Idle)
    }
    
    @Test
    fun `reset workflow should clear state`() = runTest {
        // Given
        val customerId = "CUST-0001"
        every { dataStoreManager.selectedCustomerId } returns flowOf(customerId)
        coEvery { repository.getRecentChecks() } returns emptyList()
        coEvery { repository.getAllCustomers() } returns listOf("CUST-0001")
        
        viewModel = DashboardViewModel(repository, dataStoreManager)
        kotlinx.coroutines.delay(100)
        
        // When
        viewModel.resetWorkflow()
        
        // Then
        val workflowState = viewModel.workflowState.value
        assertTrue(workflowState is WorkflowState.Idle)
        assertNull(viewModel.currentCorrelationId.value)
    }
}
