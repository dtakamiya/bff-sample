package com.example.bff.restclient.service

import com.example.bff.restclient.api.DashboardResponse
import com.example.bff.restclient.api.OrderSummary
import com.example.bff.restclient.api.ProductSummary
import com.example.bff.restclient.api.UserSummary
import com.example.bff.restclient.client.DownstreamClient
import com.example.bff.restclient.client.DownstreamOrder
import com.example.bff.restclient.client.DownstreamProduct
import com.example.bff.restclient.client.DownstreamUser
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutorService

/**
 * ユーザー・注文・商品を集約して [DashboardResponse] を組み立てるサービス。
 *
 * 並列化は 2 段構え。
 * 1. user と orders は互いに依存しないので、同時に投げる。
 * 2. product の ID は orders の結果からしか分からないので、orders が揃ってから注文件数分を同時に投げる。
 *
 * よって待ち時間は「段 1 の遅い方」＋「段 2 の遅い方」になる（プールの待ち時間を除く）。
 * 直列版（bff-restclient-sequential）と比べるときの中心になる箇所。
 */
@Service
class DashboardService(
    private val downstreamClient: DownstreamClient,
    private val downstreamExecutor: ExecutorService,
) {
    /**
     * 集約を実行する。
     *
     * user を orders より先に待つ。そのため user が 404 なら、orders の結果を待たずに例外が上がる。
     * ただし orders の呼び出しは既に投入済みで、キャンセルはしない。結果は捨てられるが、
     * 実行が終わるまでプールのスレッドは使われ続ける（404 時にも無駄な下流呼び出しが発生する点は認識しておく）。
     */
    fun buildDashboard(userId: String): DashboardResponse {
        // 段 1: 両方を先に投入してから待つ。投入と待ちの間に await を挟むと、直列になってしまう。
        val userFuture = supplyAsync { downstreamClient.fetchUser(userId) }
        val ordersFuture = supplyAsync { downstreamClient.fetchOrders(userId) }

        // user を先に待つ。ここで UserNotFound が出れば、下の await(ordersFuture) には到達しない。
        val user = await(userFuture)
        val orders = await(ordersFuture)

        // 段 2: 注文件数分の product 呼び出しを一斉に投入する。注文が 0 件なら呼び出しは発生しない。
        // 件数が downstream.parallelism を超えた分は、プールのキューで待つ。
        val productFutures = orders.map { order ->
            order to supplyAsync { downstreamClient.fetchProduct(order.productId) }
        }
        // map は入力順を保つので、注文の並びは orders のまま維持される。
        // 途中の product が失敗すると、その時点で例外が上がる。残りの product は走り続ける（キャンセルしない）。
        val orderSummaries = productFutures.map { (order, productFuture) ->
            toOrderSummary(order, await(productFuture))
        }

        return DashboardResponse(
            user = toUserSummary(user),
            orders = orderSummaries,
            // Int 同士の掛け算はオーバーフローしうるので、先に Long に広げてから掛ける。
            totalAmount = orderSummaries.sumOf { it.product.price.toLong() * it.quantity },
        )
    }

    /**
     * 専用の [downstreamExecutor] 上で非同期に実行する。
     * 既定の ForkJoinPool.commonPool を使わないのは、下流の待ち（I/O ブロッキング）で
     * 共有プールを占有しないため。そして同時実行数を `downstream.parallelism` で制御するため。
     */
    private fun <T> supplyAsync(call: () -> T): CompletableFuture<T> =
        CompletableFuture.supplyAsync(call, downstreamExecutor)

    /**
     * future の結果を待つ。
     *
     * [CompletableFuture.join] は失敗を [CompletionException] で包んで投げる。包んだままだと
     * GlobalExceptionHandler の例外型（UserNotFoundException / DownstreamException）に一致せず、想定外の 500 になる。
     * そこで元の例外（cause）を取り出して投げ直す。`DashboardControllerTest` の 404 ケースがこの経路を通る。
     * cause が無い場合は包みをそのまま投げる（通常は起きない防御的な分岐）。
     */
    private fun <T> await(future: CompletableFuture<T>): T =
        try {
            future.join()
        } catch (e: CompletionException) {
            throw e.cause ?: e
        }

    private fun toUserSummary(user: DownstreamUser): UserSummary =
        UserSummary(id = user.id, name = user.name, tier = user.tier)

    private fun toOrderSummary(order: DownstreamOrder, product: DownstreamProduct): OrderSummary =
        OrderSummary(
            orderId = order.orderId,
            quantity = order.quantity,
            product = ProductSummary(id = product.id, name = product.name, price = product.price),
        )
}
