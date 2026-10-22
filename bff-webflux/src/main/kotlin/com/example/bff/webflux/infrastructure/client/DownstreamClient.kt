package com.example.bff.webflux.infrastructure.client

import com.example.bff.webflux.infrastructure.config.DownstreamProperties
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * 下流 API を呼び出す薄い WebClient ラッパー（infrastructure 層）。
 *
 * - 戻り値はすべて下流の DTO の Mono のまま返し、ここでは購読しない（subscribe も block もしない）。
 *   HTTP リクエストは購読された時点で初めて飛ぶ（遅延実行）。並列化の組み立ては GetDashboardUseCase が行う。
 * - timeout は「1 回の呼び出しごと」の上限。全体の予算ではない。
 * - 2xx 以外は WebClientResponseException、タイムアウトは TimeoutException としてエラーシグナルで流れる。
 *   ここでは例外を変換しない。ドメイン例外への変換は ACL（DownstreamErrorTranslator）が行う。
 */
@Component
class DownstreamClient(
    private val webClient: WebClient,
    properties: DownstreamProperties,
) {
    private val timeout: Duration = Duration.ofMillis(properties.timeoutMs)

    /**
     * ユーザー情報を取得する。ユーザーが存在しない場合は下流が 404 を返す（判定はアダプタ側の ACL が行う）。
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
     * ユーザーの注文一覧を取得する。注文が 0 件の場合は空リストを返す。
     * 下流が本文なしで応答すると Mono は値なしで完了する（GetDashboardUseCase の注意点を参照）。
     */
    fun fetchOrders(userId: String): Mono<List<OrderResponse>> =
        webClient.get()
            .uri(USER_ORDERS_PATH, userId)
            .retrieve()
            .bodyToMono<List<OrderResponse>>()
            .timeout(timeout)

    /** 商品詳細を 1 件取得する。商品の 404 も含めて例外のまま流し、ProductRepository の実装が 502 側へまとめる。 */
    fun fetchProduct(productId: String): Mono<ProductResponse> =
        webClient.get()
            .uri(PRODUCT_PATH, productId)
            .retrieve()
            .bodyToMono<ProductResponse>()
            .timeout(timeout)

    private companion object {
        // パスはURIテンプレート。{id} は uri() の引数で埋まる（文字列の連結ではない）。ホスト部分は WebClientConfig の baseUrl が担う。
        const val USER_PATH = "/users/{id}"
        const val USER_ORDERS_PATH = "/users/{id}/orders"
        const val PRODUCT_PATH = "/products/{id}"
    }
}
