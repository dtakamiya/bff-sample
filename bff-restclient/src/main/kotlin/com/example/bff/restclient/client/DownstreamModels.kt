package com.example.bff.restclient.client

/**
 * 下流 API の応答を受けるための型（下流の仕様に合わせた形）。
 *
 * 公開 API 側の型（api パッケージ）とは分けて持つ。下流の JSON をそのまま受け、
 * 画面向けへの変換は [com.example.bff.restclient.service.DashboardService] で行う。
 * 非 null のフィールドが欠けた応答はデシリアライズに失敗し、最終的に 502 になる。
 */

/** `GET /users/{id}` の応答。 */
data class DownstreamUser(
    val id: String,
    val name: String,
    val tier: String,
)

/**
 * `GET /users/{id}/orders` の要素。
 * 商品の中身は含まず [productId] だけを持つ。商品情報は別途 `/products/{id}` を引き直す（2 段目の呼び出し）。
 */
data class DownstreamOrder(
    val orderId: String,
    val productId: String,
    val quantity: Int,
)

/** `GET /products/{id}` の応答。 */
data class DownstreamProduct(
    val id: String,
    val name: String,
    val price: Int,
)
