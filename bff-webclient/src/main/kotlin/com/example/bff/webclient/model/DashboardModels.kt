package com.example.bff.webclient.model

/**
 * 画面（フロントエンド）へ返す集約レスポンス。
 * 下流の 3 種類の応答（ユーザー・注文・商品）を 1 つの JSON にまとめた形。この BFF の外部契約なので、変更は画面側に影響する。
 */
data class DashboardResponse(
    val user: DashboardUser,
    val orders: List<DashboardOrder>,
    /** 全注文の「商品単価 × 数量」の合計。Long なのは合計値が Int に収まらない可能性を考慮したため。 */
    val totalAmount: Long,
)

/** 画面用のユーザー情報。下流の UserResponse から必要な項目だけを写す。 */
data class DashboardUser(
    val id: String,
    val name: String,
    val tier: String,
)

/** 注文 1 件に、商品の詳細を埋め込んだ形。下流では productId だけだった商品情報を、画面向けに展開している。 */
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

/** エラー時のレスポンスボディ。BffExceptionHandler が `{"error": "..."}` の形で返す。 */
data class ErrorBody(
    val error: String,
)
