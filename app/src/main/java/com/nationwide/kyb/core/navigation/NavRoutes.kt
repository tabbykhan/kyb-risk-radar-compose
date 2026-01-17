package com.nationwide.kyb.core.navigation

/**
 * Navigation routes for the app
 */
sealed class NavRoute(val route: String) {
    object Dashboard : NavRoute("dashboard")
    data class CustomerDetail(val customerId: String, val correlationId: String) : NavRoute("customer_detail/{customerId}/{correlationId}") {
        fun createRoute(customerId: String, correlationId: String): String {
            return "customer_detail/$customerId/$correlationId"
        }
        
        companion object {
            const val CUSTOMER_ID_ARG = "customerId"
            const val CORRELATION_ID_ARG = "correlationId"
        }
    }
}
