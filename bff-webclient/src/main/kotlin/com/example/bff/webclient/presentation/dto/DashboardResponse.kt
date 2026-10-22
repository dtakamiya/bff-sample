package com.example.bff.webclient.presentation.dto

import com.example.bff.webclient.application.dto.DashboardResult

/*
 * 画面（フロントエンド）へ返す JSON の形（presentation 層）。
 * 形（クラス名・フィールド名・順序）は変更しない。この BFF の外部契約なので、変更は画面側に影響する。
 * アプリケーションの結果 DTO（DashboardResult）からは toResponse() で写す。
 */

/** 画面向けの集約レスポンス。下流の 3 種類の応答（ユーザー・注文・商品）を 1 つの JSON にまとめた形。 */
data class DashboardResponse(
    val user: DashboardUser,
    val orders: List<DashboardOrder>,
    /** 全注文の「商品単価 × 数量」の合計。Long なのは合計値が Int に収まらない可能性を考慮したため。 */
    val totalAmount: Long,
)

/** 画面用のユーザー情報。 */
data class DashboardUser(
    val id: String,
    val name: String,
    val tier: String,
)

/** 注文 1 件に、商品の詳細を埋め込んだ形。 */
data class DashboardOrder(
    val orderId: String,
    val quantity: Int,
    val product: DashboardProduct,
)

/** 画面用の商品情報。 */
data class DashboardProduct(
    val id: String,
    val name: String,
    val price: Int,
)

/** アプリケーションの結果を、画面向けの JSON 形へ写す。 */
fun DashboardResult.toResponse(): DashboardResponse =
    DashboardResponse(
        user = DashboardUser(id = user.id, name = user.name, tier = user.tier),
        orders = orders.map { order ->
            DashboardOrder(
                orderId = order.orderId,
                quantity = order.quantity,
                product = DashboardProduct(
                    id = order.product.id,
                    name = order.product.name,
                    price = order.product.price,
                ),
            )
        },
        totalAmount = totalAmount,
    )
