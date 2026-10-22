package com.example.bff.webclient.application.dto

/*
 * ダッシュボードのユースケース結果（アプリケーション層の出力）。
 * ドメイン型（値オブジェクト）を外へ出さず、プリミティブに展開した形にする。
 * 画面向けの JSON の形は presentation が決める（この型は HTTP の概念を持たない）。
 */

/** ダッシュボードの結果全体。 */
data class DashboardResult(
    val user: DashboardUserResult,
    val orders: List<DashboardOrderResult>,
    /** 全注文の「商品単価 × 数量」の合計。 */
    val totalAmount: Long,
)

/** ユーザーの結果。 */
data class DashboardUserResult(
    val id: String,
    val name: String,
    val tier: String,
)

/** 注文 1 件の結果。商品の詳細を埋め込んだ形。 */
data class DashboardOrderResult(
    val orderId: String,
    val quantity: Int,
    val product: DashboardProductResult,
)

/** 商品の結果。 */
data class DashboardProductResult(
    val id: String,
    val name: String,
    val price: Int,
)
