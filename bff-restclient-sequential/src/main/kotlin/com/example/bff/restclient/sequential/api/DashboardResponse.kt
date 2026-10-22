package com.example.bff.restclient.sequential.api

data class DashboardResponse(
    val user: UserSummary,
    val orders: List<OrderSummary>,
    val totalAmount: Long,
)

data class UserSummary(
    val id: String,
    val name: String,
    val tier: String,
)

data class OrderSummary(
    val orderId: String,
    val quantity: Int,
    val product: ProductSummary,
)

data class ProductSummary(
    val id: String,
    val name: String,
    val price: Int,
)
