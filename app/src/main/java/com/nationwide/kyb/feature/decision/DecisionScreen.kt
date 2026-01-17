package com.nationwide.kyb.feature.decision

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nationwide.kyb.domain.model.RiskBand
import com.nationwide.kyb.domain.model.UiState

/**
 * RM Decision Tab Screen
 * - AI score
 * - Key drivers
 * - RM override dropdown (RED / AMBER / GREEN)
 * - RM comments input
 */
@Composable
fun DecisionScreen(
    viewModel: DecisionViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val rmOverride by viewModel.rmOverride.collectAsStateWithLifecycle()
    val rmComments by viewModel.rmComments.collectAsStateWithLifecycle()
    
    when (val state = uiState) {
        is UiState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is UiState.Error -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Error: ${state.message}")
            }
        }
        is UiState.Success -> {
            val kybData = state.data
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // AI Score
                item {
                    AiScoreCard(riskAssessment = kybData.riskAssessment)
                }
                
                // Key Drivers
                item {
                    KeyDriversCard(
                        overallReasoning = kybData.riskAssessment.overallReasoning,
                        triggerImpacts = kybData.riskAssessment.scoreBreakdown.triggerImpacts
                    )
                }
                
                // RM Override
                item {
                    RmOverrideCard(
                        currentRiskBand = kybData.riskAssessment.riskBand,
                        rmOverride = rmOverride,
                        onOverrideSelected = { riskBand ->
                            viewModel.updateRmOverride(riskBand)
                        }
                    )
                }
                
                // RM Comments
                item {
                    RmCommentsCard(
                        comments = rmComments,
                        onCommentsChanged = { comments ->
                            viewModel.updateRmComments(comments)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AiScoreCard(riskAssessment: com.nationwide.kyb.domain.model.RiskAssessment) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "AI Score",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${riskAssessment.score}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Risk Band: ${riskAssessment.riskBand.name}",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun KeyDriversCard(
    overallReasoning: String,
    triggerImpacts: List<com.nationwide.kyb.domain.model.TriggerImpact>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Key Drivers",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = overallReasoning,
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (triggerImpacts.isNotEmpty()) {
                Divider()
                triggerImpacts.forEach { impact ->
                    Text(
                        text = "• ${impact.code}: +${impact.delta} points",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RmOverrideCard(
    currentRiskBand: RiskBand,
    rmOverride: RiskBand?,
    onOverrideSelected: (RiskBand) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "RM Override",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Current AI Risk Band: ${currentRiskBand.name}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = rmOverride?.name ?: "Select Override",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("RM Override") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    RiskBand.values().forEach { riskBand ->
                        DropdownMenuItem(
                            text = { Text(riskBand.name) },
                            onClick = {
                                onOverrideSelected(riskBand)
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
private fun RmCommentsCard(
    comments: String,
    onCommentsChanged: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "RM Comments",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = comments,
                onValueChange = onCommentsChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                placeholder = { Text("Enter comments...") },
                maxLines = 10
            )
        }
    }
}
