package com.nationwide.kyb.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nationwide.kyb.core.di.AppModule
import com.nationwide.kyb.core.utils.Logger
import com.nationwide.kyb.data.datasource.MockDataSource
import com.nationwide.kyb.data.local.DataStoreManager
import com.nationwide.kyb.data.repository.KybRepositoryImpl
import com.nationwide.kyb.domain.repository.KybRepository
import com.nationwide.kyb.feature.customerdetail.CustomerDetailScreen
import com.nationwide.kyb.feature.dashboard.DashboardScreen
import com.nationwide.kyb.feature.dashboard.DashboardViewModel
import com.nationwide.kyb.feature.decision.AuditViewModel
import com.nationwide.kyb.feature.decision.DecisionViewModel
import com.nationwide.kyb.feature.riskactions.RiskActionsViewModel
import com.nationwide.kyb.feature.summary.SummaryViewModel
import java.io.InputStream

/**
 * Navigation setup for the app
 */
@Composable
fun KybNavigation(
    navController: NavHostController,
    assetsInputStream: InputStream,
    context: android.content.Context
) {
    // Initialize dependencies
    val dataStoreManager: DataStoreManager = AppModule.provideDataStoreManager(context)
    val mockDataSource = MockDataSource(assetsInputStream)
    val repository: KybRepository = KybRepositoryImpl(mockDataSource, dataStoreManager)
    
    NavHost(
        navController = navController,
        startDestination = NavRoute.Dashboard.route
    ) {
        composable(NavRoute.Dashboard.route) {
            val viewModel = DashboardViewModel(repository, dataStoreManager)
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToCustomerDetail = { customerId, correlationId ->
                    val route = NavRoute.CustomerDetail(customerId, correlationId)
                        .createRoute(customerId, correlationId)
                    navController.navigate(route) {
                        Logger.logEvent(
                            eventName = "NAVIGATION_CUSTOMER_DETAIL",
                            correlationId = correlationId,
                            customerId = customerId,
                            screenName = "Navigation"
                        )
                    }
                }
            )
        }
        
        composable(
            route = NavRoute.CustomerDetail("", "").route,
            arguments = listOf(
                navArgument(NavRoute.CustomerDetail.CUSTOMER_ID_ARG) { type = NavType.StringType },
                navArgument(NavRoute.CustomerDetail.CORRELATION_ID_ARG) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString(NavRoute.CustomerDetail.CUSTOMER_ID_ARG) ?: ""
            val correlationId = backStackEntry.arguments?.getString(NavRoute.CustomerDetail.CORRELATION_ID_ARG) ?: ""
            
            // Recreate repository with fresh input stream (in production, use better state management)
            val freshInputStream = context.assets.open("data.json")
            val freshMockDataSource = MockDataSource(freshInputStream)
            val freshRepository: KybRepository = KybRepositoryImpl(freshMockDataSource, dataStoreManager)
            
            val summaryViewModel = SummaryViewModel(freshRepository, customerId, correlationId)
            val riskActionsViewModel = RiskActionsViewModel(freshRepository, customerId, correlationId)
            val decisionViewModel = DecisionViewModel(freshRepository, customerId, correlationId)
            val auditViewModel = AuditViewModel(freshRepository, customerId, correlationId)
            
            CustomerDetailScreen(
                customerId = customerId,
                correlationId = correlationId,
                summaryViewModel = summaryViewModel,
                riskActionsViewModel = riskActionsViewModel,
                decisionViewModel = decisionViewModel,
                auditViewModel = auditViewModel,
                onBack = {
                    navController.popBackStack()
                    Logger.logEvent(
                        eventName = "NAVIGATION_BACK",
                        correlationId = correlationId,
                        customerId = customerId,
                        screenName = "Navigation"
                    )
                }
            )
        }
    }
}
