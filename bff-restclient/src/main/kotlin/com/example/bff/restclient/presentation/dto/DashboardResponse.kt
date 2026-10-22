package com.example.bff.restclient.presentation.dto

import com.example.bff.restclient.application.dto.DashboardResult

/*
 * 画面（フロントエンド）へ返す JSON の形（presentation 層）。
 * 形（クラス名・フィールド名・順序）は変更しない。この BFF の外部契約なので、変更は画面側に影響する。
 * アプリケーションの結果 DTO（DashboardResult）からは toResponse() で写す。
 */

/**
 * 画面向けに整形したダッシュボードの応答。
 *
 * 下流の DTO（infrastructure）をそのまま返さず、必要な項目だけに絞った公開用の型。
 * 下流の項目追加や改名が、画面との契約に直接波及しないようにするため。
 *
 * [totalAmount] は各注文の「商品単価 × 数量」の合計。単位（円など）はコード上に定義がなく、下流仕様に依存する。
 * 合計だけ Long なのは、件数や金額が増えると Int の上限を超えうるため。
 */
data class DashboardResponse(
    val user: UserSummary,
    val orders: List<OrderSummary>,
    val totalAmount: Long,
)

/** 応答に含めるユーザー情報。 */
data class UserSummary(
    val id: String,
    val name: String,
    val tier: String,
)

/** 注文 1 件。商品情報は注文ごとに埋め込む。 */
data class OrderSummary(
    val orderId: String,
    val quantity: Int,
    val product: ProductSummary,
)

/** 商品情報。[price] は単価。 */
data class ProductSummary(
    val id: String,
    val name: String,
    val price: Int,
)

/** アプリケーションの結果を、画面向けの JSON 形へ写す。 */
fun DashboardResult.toResponse(): DashboardResponse =
    DashboardResponse(
        user = UserSummary(id = user.id, name = user.name, tier = user.tier),
        orders = orders.map { order ->
            OrderSummary(
                orderId = order.orderId,
                quantity = order.quantity,
                product = ProductSummary(
                    id = order.product.id,
                    name = order.product.name,
                    price = order.product.price,
                ),
            )
        },
        totalAmount = totalAmount,
    )
