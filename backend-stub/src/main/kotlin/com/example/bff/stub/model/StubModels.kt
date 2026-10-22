package com.example.bff.stub.model

/**
 * `GET /users/{id}` の応答。
 *
 * プロパティ名がそのまま JSON のキーになる。各 BFF の下流モデル（DownstreamModels.kt）と名前を揃えておく必要がある。
 * [tier] は現状いつも [com.example.bff.stub.support.USER_TIER]（固定値）。
 */
data class User(
    val id: String,
    val name: String,
    val tier: String,
)

/**
 * 注文 1 件（`GET /users/{id}/orders` の要素）。
 *
 * [productId] は BFF が `GET /products/{productId}` を呼ぶための参照キーになる。
 * BFF は `quantity` と商品の価格を掛けて合計金額を計算する（README の `totalAmount`）。
 */
data class Order(
    val orderId: String,
    val productId: String,
    val quantity: Int,
)

/**
 * 商品 1 件（`GET /products/{id}` の応答）。
 *
 * [price] の単位はコード上に定義がなく、固定値の整数をそのまま返す。
 */
data class Product(
    val id: String,
    val name: String,
    val price: Int,
)
