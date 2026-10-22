package com.example.bff.webflux.client

/**
 * 下流 API `/users/{id}` の応答形。BFF の外（画面向け）には出さず、
 * model パッケージの DashboardUser に組み替えてから返す。
 * 下流の仕様変更がクライアント向けの形へ直接波及しないように分けている。
 */
data class UserResponse(
    val id: String,
    val name: String,
    val tier: String,
)

/** 下流 API `/users/{id}/orders` の 1 件分。productId で商品を引く。 */
data class OrderResponse(
    val orderId: String,
    val productId: String,
    val quantity: Int,
)

/** 下流 API `/products/{id}` の応答形。price は下流の単位のまま扱う（BFF では換算しない）。 */
data class ProductResponse(
    val id: String,
    val name: String,
    val price: Int,
)
