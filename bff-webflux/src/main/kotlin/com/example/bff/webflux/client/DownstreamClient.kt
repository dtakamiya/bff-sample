package com.example.bff.webflux.client

import com.example.bff.webflux.config.DownstreamProperties
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * 下流 API への薄い WebClient ラッパー。
 *
 * - 戻り値はすべて Mono のまま返し、ここでは購読しない（subscribe も block もしない）。
 *   HTTP リクエストは購読された時点で初めて送られる（遅延実行）。並列化の組み立ては DashboardService が行う。
 * - 各呼び出しに timeout を掛けている。これは 1 回の下流呼び出しごとの上限で、
 *   画面 1 回分（集約リクエスト全体）の締切ではない。全体の上限は設けていない。
 * - 非 2xx は WebClientResponseException、タイムアウトは TimeoutException として下流に流す。
 *   ここでは変換せず、判定は DashboardService が行う。
 */
@Component
class DownstreamClient(
    private val webClient: WebClient,
    properties: DownstreamProperties,
) {
    private val timeout: Duration = Duration.ofMillis(properties.timeoutMs)

    /**
     * 利用者を取得する。
     *
     * Mono.timeout は、期限までに値も完了も届かないと TimeoutException を流し、
     * 上流の購読（HTTP 交換）もキャンセルする。待ちっぱなしの接続を残さないためにこの指定がある。
     */
    fun fetchUser(userId: String): Mono<UserResponse> =
        webClient.get()
            .uri(USER_PATH, userId)
            .retrieve()
            .bodyToMono<UserResponse>()
            .timeout(timeout)

    /**
     * 利用者の注文一覧を取得する。
     * 下流が本文なしで応答すると Mono は値なしで完了する。DashboardService の settle は空を Outcome に
     * 変換しないため、zip も値なしで終わり、後段の flatMap は実行されない（注意: 現状は空応答を
     * 明示的なエラーにしていない）。
     */
    fun fetchOrders(userId: String): Mono<List<OrderResponse>> =
        webClient.get()
            .uri(USER_ORDERS_PATH, userId)
            .retrieve()
            .bodyToMono<List<OrderResponse>>()
            .timeout(timeout)

    /** 商品 1 件を取得する。ID ごとに 1 回ずつ呼ばれる（重複排除は呼び出し側で行う）。 */
    fun fetchProduct(productId: String): Mono<ProductResponse> =
        webClient.get()
            .uri(PRODUCT_PATH, productId)
            .retrieve()
            .bodyToMono<ProductResponse>()
            .timeout(timeout)

    private companion object {
        // {id} は URI テンプレート変数で、uri() の引数が展開される（文字列を連結しているわけではない）。
        const val USER_PATH = "/users/{id}"
        const val USER_ORDERS_PATH = "/users/{id}/orders"
        const val PRODUCT_PATH = "/products/{id}"
    }
}
