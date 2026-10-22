package com.example.bff.restclient.sequential.service

import com.example.bff.restclient.sequential.api.DashboardResponse
import com.example.bff.restclient.sequential.api.OrderSummary
import com.example.bff.restclient.sequential.api.ProductSummary
import com.example.bff.restclient.sequential.api.UserSummary
import com.example.bff.restclient.sequential.client.DownstreamClient
import com.example.bff.restclient.sequential.client.DownstreamOrder
import com.example.bff.restclient.sequential.client.DownstreamProduct
import com.example.bff.restclient.sequential.client.DownstreamUser
import org.springframework.stereotype.Service

/**
 * ダッシュボードの集約処理（逐次版）。
 *
 * 呼び出し順は次のとおり。
 * 1. user（存在確認を兼ねる）
 * 2. orders
 * 3. 注文ごとに product（注文数 N に対して N 回）
 *
 * 待ち時間の見積もり: 各呼び出しが timeout まで待った場合の最悪値は (2 + N) × timeout。
 * 並列版は 1 と 2 を同時に、3 も並べて投げるので、待ちは約 2 段分で済む。
 * この版は足し算になる代わりに、スレッド・Future・例外の再包装が要らず、最も読みやすい。
 * 「注文が増えると遅くなる」ことが設計上の前提なので、件数の上限が無い用途には向かない。
 */
@Service
class DashboardService(
    private val downstreamClient: DownstreamClient,
) {
    /**
     * user・orders・商品を順に取得して集約する。
     *
     * - user が 404 の場合、orders と商品は一切呼ばれず、[com.example.bff.restclient.sequential.client.UserNotFoundException] がそのまま上がる。
     * - 注文が 0 件なら商品は 1 回も呼ばれず、`totalAmount` は 0 になる。
     * - 下流エラー（502 相当）は途中の呼び出しで例外になり、残りは実行されない（部分的な結果は返さない）。
     */
    fun buildDashboard(userId: String): DashboardResponse {
        // user を最初に取る。存在しない user で orders・商品を呼ぶ無駄を避けるため。
        // 並列版は user と orders を同時に投げるので、user 404 と orders 失敗が重なりうる。
        // 並列版は user を先に待って 404 を優先させている。逐次版は呼び出し順そのものでこれと同じ結果になる。
        val user = downstreamClient.fetchUser(userId)
        val orders = downstreamClient.fetchOrders(userId)

        // 商品は注文ごとに 1 件ずつ取得する。同じ productId が複数の注文にあっても重複排除はしない（N+1 の形）。
        val orderSummaries = orders.map { order ->
            toOrderSummary(order, downstreamClient.fetchProduct(order.productId))
        }

        return DashboardResponse(
            user = toUserSummary(user),
            orders = orderSummaries,
            // 数量も価格も Int。積を Int のまま計算すると溢れうるため、Long に上げてから掛ける。
            totalAmount = orderSummaries.sumOf { it.product.price.toLong() * it.quantity },
        )
    }

    private fun toUserSummary(user: DownstreamUser): UserSummary =
        UserSummary(id = user.id, name = user.name, tier = user.tier)

    /** 注文と商品を 1 つの [OrderSummary] にまとめる。下流の productId はレスポンスに含めず、商品の中身を埋め込む。 */
    private fun toOrderSummary(order: DownstreamOrder, product: DownstreamProduct): OrderSummary =
        OrderSummary(
            orderId = order.orderId,
            quantity = order.quantity,
            product = ProductSummary(id = product.id, name = product.name, price = product.price),
        )
}
