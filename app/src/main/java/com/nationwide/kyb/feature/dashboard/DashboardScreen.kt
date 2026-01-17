package com.nationwide.kyb.feature.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nationwide.kyb.core.utils.Logger
import com.nationwide.kyb.domain.model.RecentKybCheck
import com.nationwide.kyb.domain.model.RiskBand
import com.nationwide.kyb.domain.model.UiState
import com.nationwide.kyb.domain.model.WorkflowStep
import com.nationwide.kyb.ui.theme.PrimaryNavy

/**
 * Dashboard Screen
 * - Customer selection dropdown
 * - Run KYB Check button
 * - KYB workflow card with animated steps
 * - Recent KYB checks list
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToCustomerDetail: (String, String) -> Unit, // customerId, correlationId
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedCustomerId by viewModel.selectedCustomerId.collectAsStateWithLifecycle()
    val recentChecks by viewModel.recentChecks.collectAsStateWithLifecycle()
    val workflowState by viewModel.workflowState.collectAsStateWithLifecycle()
    val correlationId by viewModel.currentCorrelationId.collectAsStateWithLifecycle()
    val isRunButtonHidden by viewModel.isRunButtonHidden.collectAsStateWithLifecycle()
    
    // Navigate to detail when workflow completes
    LaunchedEffect(workflowState) {
        if (workflowState is WorkflowState.Completed) {
            val completedState = workflowState as WorkflowState.Completed
            selectedCustomerId?.let { customerId ->
                onNavigateToCustomerDetail(customerId, completedState.correlationId)
                viewModel.resetWorkflow()
            }
        }
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        // App Bar
        TopAppBar(
            title = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "KYB Dashboard",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            navigationIcon = {
                Image(
                    painter = painterResource(id = com.nationwide.kyb.R.drawable.nationwide_logo),
                    contentDescription = "Nationwide Logo",
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .size(40.dp, 40.dp),
                    contentScale = ContentScale.Fit
                )
            },
            actions = {
                Text(
                    text = "Hi, User",
                    color = Color.White,
                    modifier = Modifier.padding(end = 16.dp),
                    fontSize = 14.sp
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = PrimaryNavy
            )
        )
        
        when (val state = uiState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Error: ${state.message}")
                }
            }
            is UiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Page Header
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Ongoing KYB Early-Risk Radar",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryNavy
                            )
                            Text(
                                text = "Single view of KYB risk and recommended actions for business customers.",
                                fontSize = 16.sp,
                                color = Color(0xFF6A7A89)
                            )
                        }
                    }
                    
                    // Select Customer Section
                    item {
                        CustomerSelectionSection(
                            availableCustomers = state.data.availableCustomers,
                            customerNameMap = state.data.customerNameMap,
                            selectedCustomerId = selectedCustomerId,
                            isFirstTime = state.data.isFirstTime,
                            onCustomerSelected = { customerId ->
                                viewModel.selectCustomer(customerId)
                            }
                        )
                    }
                    
                    // Run KYB Check Button or Loader
                    item {
                        if (!isRunButtonHidden) {
                            Button(
                                onClick = { viewModel.runKybCheck() },
                                enabled = state.data.isRunButtonEnabled && workflowState is WorkflowState.Idle,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state.data.isRunButtonEnabled) PrimaryNavy else Color(0xFFECEFF1),
                                    contentColor = if (state.data.isRunButtonEnabled) Color.White else Color.Gray,
                                    disabledContainerColor = Color(0xFFECEFF1),
                                    disabledContentColor = Color.Gray
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Run Ongoing KYB Check")
                            }
                        } else {
                            // Show loader at same position
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = PrimaryNavy
                                )
                            }
                        }
                    }
                    
                    // KYB Workflow Card (hidden by default, only show after Run KYB click)
                    if (workflowState !is WorkflowState.Idle) {
                        item {
                            KybWorkflowCard(workflowState = workflowState)
                        }
                    }
                    
                    // Recent KYB Checks
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Recent KYB Checks",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryNavy
                                )
                                
                                if (recentChecks.isEmpty()) {
                                    Text(
                                        text = "No recent checks",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    recentChecks.take(10).forEach { check ->
                                        RecentCheckItem(
                                            recentCheck = check,
                                            onClick = { customerId ->
                                                correlationId?.let { corrId ->
                                                    onNavigateToCustomerDetail(customerId, corrId)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerSelectionSection(
    availableCustomers: List<String>,
    customerNameMap: Map<String, String>,
    selectedCustomerId: String?,
    isFirstTime: Boolean,
    onCustomerSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Select business customer",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryNavy
            )
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedCustomerId?.let { id ->
                        "${id} - ${customerNameMap[id] ?: ""}"
                    } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text("Select a customer") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    availableCustomers.forEach { customerId ->
                        DropdownMenuItem(
                            text = { 
                                Text("${customerId} - ${customerNameMap[customerId] ?: ""}") 
                            },
                            onClick = {
                                onCustomerSelected(customerId)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KybWorkflowCard(workflowState: WorkflowState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFECEFF1) // Light grey
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "KYB AI Workflow",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = PrimaryNavy
            )
            
            when (workflowState) {
                is WorkflowState.Idle -> {
                    // Hidden by default
                }
                is WorkflowState.Running -> {
                    WorkflowStepIndicator(
                        completedSteps = workflowState.completedSteps,
                        allSteps = WorkflowStep.values().toList()
                    )
                }
                is WorkflowState.FetchingResults -> {
                    WorkflowStepIndicator(
                        completedSteps = workflowState.completedSteps,
                        allSteps = WorkflowStep.values().toList()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Fetching results...",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                is WorkflowState.Completed -> {
                    WorkflowStepIndicator(
                        completedSteps = WorkflowStep.values().toList(),
                        allSteps = WorkflowStep.values().toList()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Workflow completed successfully!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is WorkflowState.Error -> {
                    Text(
                        text = "Error: ${workflowState.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkflowStepIndicator(
    completedSteps: List<WorkflowStep>,
    allSteps: List<WorkflowStep>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        allSteps.forEach { step ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bullet point in grey
                Text(
                    text = "•",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(16.dp)
                )
                
                // Step text - turns green when completed, italic style
                val isCompleted = completedSteps.contains(step)
                Text(
                    text = step.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = if (isCompleted) {
                        Color(0xFF4CAF50) // Green
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

@Composable
private fun RecentCheckItem(
    recentCheck: RecentKybCheck,
    onClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${recentCheck.customerId} • ${recentCheck.customerName}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        
        // Colored circle for risk indicator
        Text(
            text = "●",
            color = when (recentCheck.riskBand) {
                RiskBand.RED -> Color(0xFFF44336)
                RiskBand.AMBER -> Color(0xFFFF9800)
                RiskBand.GREEN -> Color(0xFF4CAF50)
            },
            fontSize = 16.sp
        )
    }
}
