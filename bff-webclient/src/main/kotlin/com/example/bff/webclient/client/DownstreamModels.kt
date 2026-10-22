package com.example.bff.webclient.client

data class UserResponse(
    val id: String,
    val name: String,
    val tier: String,
)

data class OrderResponse(
    val orderId: String,
    val productId: String,
    val quantity: Int,
)

data class ProductResponse(
    val id: String,
    val name: String,
    val price: Int,
)
