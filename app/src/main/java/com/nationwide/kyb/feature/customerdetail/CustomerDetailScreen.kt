package com.nationwide.kyb.feature.customerdetail

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.nationwide.kyb.core.utils.Logger
import com.nationwide.kyb.feature.decision.AuditScreen
import com.nationwide.kyb.feature.decision.AuditViewModel
import com.nationwide.kyb.feature.decision.DecisionScreen
import com.nationwide.kyb.feature.decision.DecisionViewModel
import com.nationwide.kyb.feature.riskactions.RiskActionsScreen
import com.nationwide.kyb.feature.riskactions.RiskActionsViewModel
import com.nationwide.kyb.feature.summary.SummaryScreen
import com.nationwide.kyb.feature.summary.SummaryViewModel

/**
 * Customer Detail Screen with tabs:
 * - Summary
 * - Risk & Actions
 * - RM Decision
 * - Audit (JSON)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customerId: String,
    correlationId: String,
    summaryViewModel: SummaryViewModel,
    riskActionsViewModel: RiskActionsViewModel,
    decisionViewModel: DecisionViewModel,
    auditViewModel: AuditViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        Logger.logEvent(
            eventName = "CUSTOMER_DETAIL_SCREEN_VIEWED",
            correlationId = correlationId,
            customerId = customerId,
            screenName = "CustomerDetail"
        )
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        // Top bar
        TopAppBar(
            title = { Text("Customer Detail") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Text("←")
                }
            }
        )
        
        // Tabs
        TabRow(selectedTabIndex = selectedTabIndex) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("Summary") }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Risk & Actions") }
            )
            Tab(
                selected = selectedTabIndex == 2,
                onClick = { selectedTabIndex = 2 },
                text = { Text("RM Decision") }
            )
            Tab(
                selected = selectedTabIndex == 3,
                onClick = { selectedTabIndex = 3 },
                text = { Text("Audit (JSON)") }
            )
        }
        
        // Tab content
        when (selectedTabIndex) {
            0 -> SummaryScreen(
                viewModel = summaryViewModel,
                modifier = Modifier.fillMaxSize()
            )
            1 -> RiskActionsScreen(
                viewModel = riskActionsViewModel,
                modifier = Modifier.fillMaxSize()
            )
            2 -> DecisionScreen(
                viewModel = decisionViewModel,
                modifier = Modifier.fillMaxSize()
            )
            3 -> AuditScreen(
                viewModel = auditViewModel,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
