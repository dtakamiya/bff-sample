package com.example.bff.webflux.model

/**
 * ダッシュボードの応答（クライアント向け）。下流の形をそのまま出さず、画面に必要な形へ組み替えている。
 * totalAmount は各注文の「price × quantity」の合計。単位は下流の price に従う。
 */
data class DashboardResponse(
    val user: DashboardUser,
    val orders: List<DashboardOrder>,
    val totalAmount: Long,
)

/** 応答内の利用者情報。下流の UserResponse から必要な項目だけを写す。 */
data class DashboardUser(
    val id: String,
    val name: String,
    val tier: String,
)

/** 応答内の注文 1 件。商品の詳細をネストして持つ（画面が注文と商品を一度に描けるように）。 */
data class DashboardOrder(
    val orderId: String,
    val quantity: Int,
    val product: DashboardProduct,
)

/** 応答内の商品情報。 */
data class DashboardProduct(
    val id: String,
    val name: String,
    val price: Int,
)

/** エラー時の共通応答形 `{"error": "..."}`。BffExceptionHandler が返す。 */
data class ErrorBody(
    val error: String,
)
