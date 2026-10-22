package com.example.bff.webflux.service

import com.example.bff.webflux.client.DownstreamClient
import com.example.bff.webflux.client.OrderResponse
import com.example.bff.webflux.client.ProductResponse
import com.example.bff.webflux.client.UserResponse
import com.example.bff.webflux.model.DashboardOrder
import com.example.bff.webflux.model.DashboardProduct
import com.example.bff.webflux.model.DashboardResponse
import com.example.bff.webflux.model.DashboardUser
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * ダッシュボード用の集約ロジック（WebFlux 版）。
 *
 * 下流の呼び出しはすべて Mono のまま組み立て、最後にコントローラが返した Mono を WebFlux が購読する。
 * ここには block() がなく、並列化は Reactor の演算子（zip / flatMap）だけで表現している。
 * bff-webclient は同じ集約を block() で同期化しているので、比較するときはそちらと対応づけて読むとよい。
 */
@Service
class DashboardService(
    private val downstream: DownstreamClient,
) {
    /**
     * 2 段階で集約する。
     *
     * 1. 段階 1: user と orders を並列に呼ぶ。orders の結果が要らなければ product の ID を集められないため、
     *    この 2 つは互いに独立している。
     * 2. 段階 2: orders に含まれる productId ごとに product を並列に呼ぶ。
     *    product の ID は orders が返して初めて分かるので、段階 1 より後に実行する必要がある。
     *
     * 失敗の扱い: 段階 1 の両方を settle で「成功か失敗か」の値に変えてから zip する。
     * 素の zip は最初のエラーで終了して、もう片方の購読をキャンセルする（fail-fast）。
     * その場合、orders が 500 や タイムアウトで先に失敗すると、user の 404 が届く前に 502 になり、
     * 結果がどちらの応答が先に来たかに依存してしまう。settle で両方の結末を待ってから判定すれば、
     * 利用者が存在しない（user が 404）場合は必ず 404 を返せる。
     * トレードオフ: 失敗パスでは遅い方の呼び出しを待つ。成功パスでは体感差はない。
     *
     * user が失敗していれば、段階 2（product）は実行されない（flatMap の中で先に投げるため）。
     */
    fun fetchDashboard(userId: String): Mono<DashboardResponse> =
        Mono.zip(
            settle(downstream.fetchUser(userId)),
            settle(downstream.fetchOrders(userId)),
        ) { userOutcome, ordersOutcome -> userOutcome to ordersOutcome }
            .flatMap { (userOutcome, ordersOutcome) ->
                // ここで投げた例外は Reactor が onError シグナルに変換する（スタックを巻き戻す例外ではない）。
                // そのシグナルが BffExceptionHandler まで届き、HTTP ステータスに変換される。
                val user = userOutcome.userOrThrow(userId)
                val orders = ordersOutcome.valueOrDownstreamFailure()
                fetchProducts(orders).map { products -> assemble(user, orders, products) }
            }

    /**
     * 注文に含まれる商品を並列に取得し、productId をキーにした Map にまとめる。
     *
     * - distinct: 同じ商品を複数の注文が参照していても、下流へは 1 回だけ問い合わせる。
     * - flatMap は完了順に結果を流すので、Map の順序には意味がない。
     *   順序は assemble が元の orders の並びをたどって保つ。
     * - キーには要求した productId を使い、応答の id フィールドには依存しない
     *   （応答側の id 表記に左右されず、後段の getValue が必ず引けるようにするため）。
     * - settle を使わない: ここの失敗はすべて同じ 502 になるので、優先順位を決める必要がない。
     *   1 件でも失敗したら fail-fast で残りの呼び出しをキャンセルしてよい。
     * - 注文が 0 件のとき collectMap は空の Map を流すので、合計は 0 になる（Mono が空で終わるわけではない）。
     * - onErrorMap: 商品の 404 も含めて DownstreamFailureException（502）に統一する。
     *   404 を利用者不在と解釈するのは段階 1 の user だけ。
     */
    private fun fetchProducts(orders: List<OrderResponse>): Mono<Map<String, ProductResponse>> =
        Flux.fromIterable(orders.map { it.productId }.distinct())
            .flatMap { productId -> downstream.fetchProduct(productId).map { productId to it } }
            .collectMap({ it.first }, { it.second })
            .onErrorMap { DownstreamFailureException(it) }

    /**
     * 画面向けの形へ組み替える。注文の並びは orders の元の順序を保つ。
     *
     * products.getValue は、fetchProducts が orders の全 productId を集めた後にだけ呼ばれる前提で
     * 欠けないはず。もし欠けた場合は BFF 内部の不変条件違反なので、
     * ExceptionHandler の対象外（NoSuchElementException）として 500 になる。
     */
    private fun assemble(
        user: UserResponse,
        orders: List<OrderResponse>,
        products: Map<String, ProductResponse>,
    ): DashboardResponse {
        val dashboardOrders = orders.map { order ->
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
            // price は Int のまま掛けると大きな数量で溢れるため、先に Long へ広げてから掛けて足す。
            totalAmount = dashboardOrders.sumOf { it.product.price.toLong() * it.quantity },
        )
    }

    /**
     * 下流呼び出しの結末を値として表す。成功は Success、失敗は Failure(原因例外)。
     * Failure は Outcome<Nothing> なので、どの型 T の Outcome としても使える（共変）。
     */
    private sealed interface Outcome<out T> {
        data class Success<out T>(val value: T) : Outcome<T>
        data class Failure(val cause: Throwable) : Outcome<Nothing>
    }

    /**
     * Mono の結末（値・エラー）を Outcome に変換し、エラーで終わらない Mono にする。
     * これにより Mono.zip は両方が完了するまで待つ。エラーの判定は呼び出し側に任せる。
     */
    private fun <T : Any> settle(call: Mono<T>): Mono<Outcome<T>> =
        call.map<Outcome<T>> { Outcome.Success(it) }
            .onErrorResume { Mono.just(Outcome.Failure(it)) }

    /**
     * 利用者の結果を判定する。利用者が 404 のときだけ UserNotFoundException（404）にする。
     * それ以外の失敗（5xx・タイムアウト・接続失敗など）は DownstreamFailureException（502）にする。
     */
    private fun Outcome<UserResponse>.userOrThrow(userId: String): UserResponse = when (this) {
        is Outcome.Success -> value
        is Outcome.Failure -> throw if (cause.isNotFound()) {
            UserNotFoundException(userId)
        } else {
            DownstreamFailureException(cause)
        }
    }

    /**
     * 注文の結果を判定する。利用者の存在は userOrThrow で確認済みなので、
     * orders の 404 も含めて失敗はすべて下流の異常（502）として扱う。
     */
    private fun <T : Any> Outcome<T>.valueOrDownstreamFailure(): T = when (this) {
        is Outcome.Success -> value
        is Outcome.Failure -> throw DownstreamFailureException(cause)
    }

    /** WebClient の応答が 404 かどうか。タイムアウトや接続失敗など、応答が無い失敗は false。 */
    private fun Throwable.isNotFound(): Boolean =
        this is WebClientResponseException && statusCode.value() == HttpStatus.NOT_FOUND.value()
}
