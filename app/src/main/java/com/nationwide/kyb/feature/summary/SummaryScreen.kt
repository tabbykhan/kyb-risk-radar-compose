package com.nationwide.kyb.feature.summary

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nationwide.kyb.core.utils.Logger
import com.nationwide.kyb.core.utils.PdfUtils
import com.nationwide.kyb.domain.model.RiskBand
import com.nationwide.kyb.domain.model.UiState
import kotlinx.coroutines.launch

/**
 * Summary Tab Screen
 * - Entity profile
 * - Risk band & score
 * - KYB snapshot summary
 * - Supporting metrics
 * - Organization structure placeholder
 * - Download KYB Copilot Report CTA
 */
@Composable
fun SummaryScreen(
    viewModel: SummaryViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
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
                // Entity Profile
                item {
                    EntityProfileCard(entityProfile = kybData.entityProfile)
                }
                
                // Risk Band & Score
                item {
                    RiskScoreCard(riskAssessment = kybData.riskAssessment)
                }
                
                // KYB Snapshot Summary
                item {
                    KybSnapshotCard(kybNote = kybData.kybNote)
                }
                
                // Supporting Metrics
                item {
                    SupportingMetricsCard(
                        supportingMetrics = kybData.transactionInsights.supportingMetrics,
                        summary = kybData.transactionInsights.summary
                    )
                }
                
                // Organization Structure Placeholder
                item {
                    OrganizationStructureCard(
                        organizationStructureUrl = kybData.organizationStructure
                    )
                }
                
                // Download PDF Button
                item {
                    Button(
                        onClick = {
                            scope.launch {
                                PdfUtils.copyAndOpenPdfFromAssets(context)
                                Logger.logEvent(
                                    eventName = "PDF_DOWNLOAD_REQUESTED",
                                    screenName = "Summary"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Download KYB Copilot Report")
                    }
                }
            }
        }
    }
}

@Composable
private fun EntityProfileCard(entityProfile: com.nationwide.kyb.domain.model.EntityProfile) {
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
                text = "Entity Profile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            InfoRow("Customer ID", entityProfile.customerId)
            InfoRow("Legal Name", entityProfile.legalName)
            InfoRow("Sector", entityProfile.sector)
            InfoRow("Country of Incorporation", entityProfile.countryOfIncorporation)
            InfoRow("Primary Operating Country", entityProfile.primaryOperatingCountry)
            InfoRow("Onboarding Date", entityProfile.onboardingDate)
            InfoRow("KYB Status", entityProfile.kybStatus)
            InfoRow("Turnover Band", entityProfile.turnoverBandInr)
            InfoRow("Last Review Date", entityProfile.kybLastReviewDate)
            InfoRow("Last Review Outcome", entityProfile.kybLastReviewOutcome)
            InfoRow("Journey Type", entityProfile.journeyType)
        }
    }
}

@Composable
private fun RiskScoreCard(riskAssessment: com.nationwide.kyb.domain.model.RiskAssessment) {
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
                text = "Risk Assessment",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Risk Band", style = MaterialTheme.typography.bodyMedium)
                    RiskBadge(riskBand = riskAssessment.riskBand)
                }
                Column {
                    Text("Score", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "${riskAssessment.score}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Base Score: ${riskAssessment.scoreBreakdown.baseScore}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (riskAssessment.scoreBreakdown.triggerImpacts.isNotEmpty()) {
                Text(
                    text = "Trigger Impacts:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                riskAssessment.scoreBreakdown.triggerImpacts.forEach { impact ->
                    Text(
                        text = "  ${impact.code}: +${impact.delta}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun KybSnapshotCard(kybNote: String) {
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
                text = "KYB Snapshot Summary",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = kybNote,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SupportingMetricsCard(
    supportingMetrics: com.nationwide.kyb.domain.model.SupportingMetrics,
    summary: String
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
                text = "Supporting Metrics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium
            )
            Divider()
            InfoRow("Latest Period", supportingMetrics.latestPeriod)
            InfoRow("High Risk Country Share", "${supportingMetrics.highRiskCountrySharePct}%")
            InfoRow("International Outward Change", "${supportingMetrics.intlOutwardChangePct}%")
            InfoRow("Cash Deposit Ratio", "${supportingMetrics.cashDepositRatioPct}%")
            InfoRow("Period Covered", "${supportingMetrics.periodCoveredMonths} months")
        }
    }
}

@Composable
private fun OrganizationStructureCard(organizationStructureUrl: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Organization Structure",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Placeholder: Organization structure visualization",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "URL: $organizationStructureUrl",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RiskBadge(riskBand: RiskBand) {
    val (color, text) = when (riskBand) {
        RiskBand.RED -> MaterialTheme.colorScheme.error to "RED"
        RiskBand.AMBER -> Color(0xFFFF9800) to "AMBER"
        RiskBand.GREEN -> Color(0xFF4CAF50) to "GREEN"
    }
    
    Surface(
        color = color,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}
