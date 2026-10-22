package com.example.bff.restclient.client

data class DownstreamUser(
    val id: String,
    val name: String,
    val tier: String,
)

data class DownstreamOrder(
    val orderId: String,
    val productId: String,
    val quantity: Int,
)

data class DownstreamProduct(
    val id: String,
    val name: String,
    val price: Int,
)
