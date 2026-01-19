package com.nationwide.kyb.core.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nationwide.kyb.core.di.AppModule
import com.nationwide.kyb.feature.customerdetail.CustomerDetailScreen
import com.nationwide.kyb.feature.dashboard.DashboardScreen
import com.nationwide.kyb.feature.dashboard.DashboardViewModel
import com.nationwide.kyb.feature.decision.AuditViewModel
import com.nationwide.kyb.feature.decision.DecisionViewModel
import com.nationwide.kyb.feature.riskactions.RiskActionsViewModel
import com.nationwide.kyb.feature.summary.SummaryViewModel


/**
 * Navigation setup for the app
 */
@Composable
fun KybNavigation(
    navController: NavHostController,
    context: Context
) {
    val repository = remember {
        AppModule.provideKybRepository(context)
    }

    val dataStoreManager = remember {
        AppModule.provideDataStoreManager(context)
    }

    NavHost(
        navController = navController,
        startDestination = NavRoute.Dashboard.route
    ) {

        composable(NavRoute.Dashboard.route) {
            val viewModel = DashboardViewModel(repository, dataStoreManager)

            DashboardScreen(
                viewModel = viewModel,
                onNavigateToCustomerDetail = { customerId, correlationId ->
                    navController.navigate(
                        NavRoute.CustomerDetail(customerId, correlationId)
                            .createRoute(customerId, correlationId)
                    )
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

            val customerId =
                backStackEntry.arguments?.getString(NavRoute.CustomerDetail.CUSTOMER_ID_ARG) ?: ""

            val correlationId =
                backStackEntry.arguments?.getString(NavRoute.CustomerDetail.CORRELATION_ID_ARG) ?: ""

            CustomerDetailScreen(
                customerId = customerId,
                correlationId = correlationId,
                summaryViewModel = SummaryViewModel(repository, customerId, correlationId),
                riskActionsViewModel = RiskActionsViewModel(repository, customerId, correlationId),
                decisionViewModel = DecisionViewModel(repository, customerId, correlationId),
                auditViewModel = AuditViewModel(repository, customerId, correlationId),
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

