package com.example.bff.stub.model

data class User(
    val id: String,
    val name: String,
    val tier: String,
)

data class Order(
    val orderId: String,
    val productId: String,
    val quantity: Int,
)

data class Product(
    val id: String,
    val name: String,
    val price: Int,
)
