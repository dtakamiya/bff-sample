package com.example.bff.webclient.service

import com.example.bff.webclient.client.DownstreamClient
import com.example.bff.webclient.client.OrderResponse
import com.example.bff.webclient.client.ProductResponse
import com.example.bff.webclient.client.UserResponse
import com.example.bff.webclient.model.DashboardOrder
import com.example.bff.webclient.model.DashboardProduct
import com.example.bff.webclient.model.DashboardResponse
import com.example.bff.webclient.model.DashboardUser
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * ダッシュボード画面向けのデータを、下流の 3 系統（ユーザー・注文・商品）から組み立てるサービス。
 *
 * 並列化の構造は 2 段構え。
 *  1. ユーザーと注文を Mono.zip で並列に取得し、両方が終わるまで待つ。
 *  2. 注文から商品 ID を集め、Flux.flatMap で商品詳細を並列に取得する。商品 ID が注文の結果に依存するため、1 段目の後に始まる。
 * よって所要時間はおおむね max(user, orders) + max(products)。各呼び出しは個別の timeout で打ち切られる。
 *
 * 失敗の扱いは次のとおり。
 *  - ユーザーが 404 → UserNotFoundException（HTTP 404）。他の失敗より優先して判定する。
 *  - それ以外の失敗（ユーザー・注文・商品のいずれか）→ DownstreamFailureException（HTTP 502）。
 */
@Service
class DashboardService(
    private val downstream: DownstreamClient,
) {
    /**
     * ダッシュボードを組み立てる。戻り値は Mono で、購読されて初めて下流呼び出しが始まる。
     *
     * - user と orders は settle で「成功か失敗か」を値に包んでから zip する。
     *   settle しないと Mono.zip は最初のエラーで残りをキャンセルする。その結果、どちらの失敗が返るかが
     *   到着順（タイミング）で変わってしまう。settle すると両方の完了を必ず待ち、判定は flatMap 内で決定的に行える。
     * - 副作用として、user が早く 404 になっても orders の完了（最大 timeout）は待つ。
     * - 注意: 空の Mono（下流が本文なしで応答した等）は Outcome にならず、そのまま空として流れる。
     *   Mono.zip は source のどれかが空だと値を出さずに空で完了するため、この Mono も空になる。
     *   コントローラで block() すると null になり、requireNotNull が IllegalArgumentException を投げ、
     *   BffExceptionHandler の対象外のため 500 になる。
     */
    fun fetchDashboard(userId: String): Mono<DashboardResponse> =
        Mono.zip(
            settle(downstream.fetchUser(userId)),
            settle(downstream.fetchOrders(userId)),
        ) { userOutcome, ordersOutcome -> userOutcome to ordersOutcome }
            .flatMap { (userOutcome, ordersOutcome) ->
                // ユーザーの失敗を最優先で判定する。ここで 404 なら orders の結果に関係なく 404 になる。
                val user = userOutcome.userOrThrow(userId)
                val orders = ordersOutcome.valueOrDownstreamFailure()
                // 2 段目。注文が確定した後に、商品をまとめて取得する。
                fetchProducts(orders).map { products -> assemble(user, orders, products) }
            }

    /**
     * 注文に含まれる商品を並列に取得し、productId をキーにしたマップにして返す。
     *
     * - distinct で同じ商品の重複取得を防ぐ（同じ商品が複数の注文にあっても 1 回だけ呼ぶ）。
     * - flatMap は各商品の呼び出しを並列に購読する。同時実行数は Reactor の既定値に任せている。
     * - キーはリクエストした productId を使う（下流の応答内容には依存しない）。
     * - collectMap は要素が 0 件でも空のマップで完了する。注文が無ければ、ここは空で完了する。
     * - 商品の失敗（タイムアウト・404 を含む任意のエラー）は一律 DownstreamFailureException になる。
     */
    private fun fetchProducts(orders: List<OrderResponse>): Mono<Map<String, ProductResponse>> =
        Flux.fromIterable(orders.map { it.productId }.distinct())
            .flatMap { productId -> downstream.fetchProduct(productId).map { productId to it } }
            .collectMap({ it.first }, { it.second })
            // このパイプラインで出たエラーだけを変換する。user/orders のエラーはここには届かない（前段で判定済み）。
            .onErrorMap { DownstreamFailureException(it) }

    /**
     * 取得済みの値を画面向けの形に組み立てる。
     *
     * 下流の失敗はすべて前段で判定済みなので、ここで例外を出さない前提。
     * 例外が出る唯一の経路: 注文の productId に対応する商品が products に無い場合（商品の応答が空だった等）。
     * その場合 getValue が NoSuchElementException を投げ、BffExceptionHandler の対象外のため 500 になる。
     */
    private fun assemble(
        user: UserResponse,
        orders: List<OrderResponse>,
        products: Map<String, ProductResponse>,
    ): DashboardResponse {
        val dashboardOrders = orders.map { order ->
            // 商品マップに無ければ NoSuchElementException（上記の注意点参照）。
            val product = products.getValue(order.productId)
            DashboardOrder(
                orderId = order.orderId,
                quantity = order.quantity,
                product = DashboardProduct(id = product.id, name = product.name, price = product.price),
            )
        }
        return DashboardResponse(
            user = DashboardUser(id = user.id, name = user.name, tier = user.tier),
            orders = dashboardOrders,
            // 先に Long へ上げてから数量を掛ける。Int 同士の掛け算はオーバーフローし得るため。
            // 注文ごとに数える（同じ商品を複数注文していれば、それぞれ合計に入る）。
            totalAmount = dashboardOrders.sumOf { it.product.price.toLong() * it.quantity },
        )
    }

    /**
     * 下流呼び出しの結果を「成功」か「失敗（例外を保持）」のどちらかの値として表す型。
     *
     * Mono のエラーを一度「値」として運ぶための箱。エラーを値にしてしまえば、Mono.zip が途中で打ち切られない。
     * 判定（どの失敗を優先するか）は、zip の後段で userOrThrow などを使って行う。
     * Failure は共変（out T）の Outcome<Nothing> なので、どの型の Outcome としても使える。
     */
    private sealed interface Outcome<out T> {
        data class Success<out T>(val value: T) : Outcome<T>
        data class Failure(val cause: Throwable) : Outcome<Nothing>
    }

    /**
     * Mono の成功・失敗を Outcome に包む。例外は onErrorResume で値に変換するため、エラーは外へ流れない。
     *
     * 注意: 空の Mono は成功にも失敗にもならず、空のまま流れる（map も onErrorResume も呼ばれない）。
     * そのため空応答は settle では救われず、上位の Mono.zip を空にする（fetchDashboard の注意点を参照）。
     */
    private fun <T : Any> settle(call: Mono<T>): Mono<Outcome<T>> =
        call.map<Outcome<T>> { Outcome.Success(it) }
            .onErrorResume { Mono.just(Outcome.Failure(it)) }

    /**
     * ユーザーの結果を判定する。成功なら値を返し、失敗なら例外を投げる。
     * 下流の 404 だけは UserNotFoundException（HTTP 404）、それ以外は DownstreamFailureException（HTTP 502）。
     * flatMap のラムダ内で投げた例外は、Mono のエラーシグナルとして流れる（スレッドを落とさない）。
     */
    private fun Outcome<UserResponse>.userOrThrow(userId: String): UserResponse = when (this) {
        is Outcome.Success -> value
        is Outcome.Failure -> throw if (cause.isNotFound()) {
            UserNotFoundException(userId)
        } else {
            DownstreamFailureException(cause)
        }
    }

    /** 注文の結果を判定する。成功なら値を返し、失敗は一律 DownstreamFailureException（HTTP 502）。 */
    private fun <T : Any> Outcome<T>.valueOrDownstreamFailure(): T = when (this) {
        is Outcome.Success -> value
        is Outcome.Failure -> throw DownstreamFailureException(cause)
    }

    /** 下流の HTTP 404 かどうか。タイムアウトや接続失敗など、HTTP 応答が無いエラーは false。 */
    private fun Throwable.isNotFound(): Boolean =
        this is WebClientResponseException && statusCode.value() == HttpStatus.NOT_FOUND.value()
}
