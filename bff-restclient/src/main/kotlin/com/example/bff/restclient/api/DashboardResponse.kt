package com.example.bff.restclient.api

/**
 * 画面向けに整形したダッシュボードの応答。
 *
 * 下流の DTO（client パッケージ）をそのまま返さず、必要な項目だけに絞った公開用の型。
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

/** 応答に含めるユーザー情報。下流の [com.example.bff.restclient.client.DownstreamUser] から必要項目だけを抜き出す。 */
data class UserSummary(
    val id: String,
    val name: String,
    val tier: String,
)

/** 注文 1 件。商品情報は注文ごとに埋め込む（注文の取得後に、商品を 2 段目で引いた結果）。 */
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
