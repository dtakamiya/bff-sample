package com.example.bff.webclient.domain.model

/**
 * ダッシュボードの集約ルート。ユーザーと、その注文（商品情報を添えた明細）をまとめる。
 *
 * 不変条件: すべての注文について、対応する商品が存在すること（assemble で検査する）。
 * 合計金額は明細から導出するため、外から不整合な値を渡せない。
 */
class Dashboard private constructor(
    val user: User,
    val orderLines: List<OrderLine>,
) {
    /** 全明細の金額合計。Long なのは合計値が Int に収まらない可能性を考慮したため。 */
    val totalAmount: Long = orderLines.sumOf { it.amount }

    companion object {
        /**
         * 取得済みのユーザー・注文・商品から、ダッシュボードを組み立てる。
         *
         * 注文に対応する商品が products に無い場合は IllegalStateException を投げる。
         * ドメイン例外ではないため BffExceptionHandler の対象外となり、HTTP 500 になる（商品の欠落は下流の不整合）。
         */
        fun assemble(user: User, orders: List<Order>, products: Map<ProductId, Product>): Dashboard {
            val lines = orders.map { order ->
                val product = products[order.productId]
                    ?: throw IllegalStateException("product missing for order ${order.id.value}: ${order.productId.value}")
                OrderLine(order = order, product = product)
            }
            return Dashboard(user = user, orderLines = lines)
        }
    }
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
