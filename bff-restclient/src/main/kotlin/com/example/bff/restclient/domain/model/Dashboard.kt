package com.example.bff.restclient.domain.model

/**
 * ダッシュボードの集約ルート。ユーザーと、その注文（商品情報を添えた明細）をまとめる。
 *
 * 明細は「注文と、その注文が指す商品」の組で作るため、商品が欠けた明細はこの型では表せない（型で不変条件を保証する）。
 * 合計金額は明細から導出するため、外から不整合な値を渡せない。
 */
class Dashboard(
    val user: User,
    val orderLines: List<OrderLine>,
) {
    /** 全明細の金額合計。Long なのは合計値が Int に収まらない可能性を考慮したため。 */
    val totalAmount: Long = orderLines.sumOf { it.amount }
}

/** ダッシュボードの明細 1 行。注文と、その注文が指す商品の組。 */
data class OrderLine(
    val order: Order,
    val product: Product,
) {
    /** 明細の金額（商品単価 × 数量）。先に Long へ上げてから掛ける。Int 同士の掛け算はオーバーフローし得るため。 */
    val amount: Long
        get() = product.price.toLong() * order.quantity
}
