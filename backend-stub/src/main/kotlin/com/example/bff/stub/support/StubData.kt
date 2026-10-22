package com.example.bff.stub.support

import com.example.bff.stub.model.Order

const val USER_TIER = "gold"
const val PRODUCT_PRICE = 1200

val STUB_ORDERS: List<Order> = listOf(
    Order(orderId = "o1", productId = "p1", quantity = 2),
    Order(orderId = "o2", productId = "p2", quantity = 1),
    Order(orderId = "o3", productId = "p3", quantity = 5),
)
