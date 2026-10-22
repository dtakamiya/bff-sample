package com.example.bff.webflux.model

data class DashboardResponse(
    val user: DashboardUser,
    val orders: List<DashboardOrder>,
    val totalAmount: Long,
)

data class DashboardUser(
    val id: String,
    val name: String,
    val tier: String,
)

data class DashboardOrder(
    val orderId: String,
    val quantity: Int,
    val product: DashboardProduct,
)

data class DashboardProduct(
    val id: String,
    val name: String,
    val price: Int,
)

data class ErrorBody(
    val error: String,
)
