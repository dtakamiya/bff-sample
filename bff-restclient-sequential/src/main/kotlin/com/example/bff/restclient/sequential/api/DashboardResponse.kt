package com.example.bff.restclient.sequential.api

/**
 * 画面に返す集約結果。下流の応答をそのまま返さず、画面に要る項目だけに絞っている。
 *
 * [totalAmount] は各注文の `product.price * quantity` の合計。
 * 価格の単位は下流の定義に従い、BFF では換算していない。
 * 合計が Int に収まらない可能性があるため Long で持つ。
 */
data class DashboardResponse(
    val user: UserSummary,
    val orders: List<OrderSummary>,
    val totalAmount: Long,
)

/** 下流 user から画面用に絞った項目。 */
data class UserSummary(
    val id: String,
    val name: String,
    val tier: String,
)

/**
 * 1 件の注文。注文ごとに商品情報を埋め込む。
 * 同じ商品が複数の注文に含まれていても、商品は注文ごとに別々に取得した結果を入れている（逐次版でも並列版でも同じ）。
 */
data class OrderSummary(
    val orderId: String,
    val quantity: Int,
    val product: ProductSummary,
)

/** 注文に埋め込む商品情報。 */
data class ProductSummary(
    val id: String,
    val name: String,
    val price: Int,
)
