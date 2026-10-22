package com.example.bff.webclient.infrastructure.client

import com.example.bff.webclient.infrastructure.config.DownstreamProperties
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * 下流 API を呼び出す薄い WebClient ラッパー（infrastructure 層）。
 *
 * - 戻り値はすべて下流の DTO の Mono のまま返し、ここでは購読しない。HTTP リクエストは購読された時点で初めて飛ぶ（遅延実行）。
 *   いつ・どの順で呼ぶかは呼び出し側（GetDashboardUseCase、経由するリポジトリアダプタ）が決める。
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
     */
    fun fetchUser(userId: String): Mono<UserResponse> =
        webClient.get()
            .uri(USER_PATH, userId)
            .retrieve()
            .bodyToMono<UserResponse>()
            // timeout は購読から「値または完了」が来るまでの上限。ボディの読み取りも含む。超過時は上流の購読が解除される。
            .timeout(timeout)

    /**
     * ユーザーの注文一覧を取得する。注文が 0 件の場合は空リストを返す。
     * 下流が本文なしで応答すると Mono は空（値も error もない）で完了する点に注意。
     */
    fun fetchOrders(userId: String): Mono<List<OrderResponse>> =
        webClient.get()
            .uri(USER_ORDERS_PATH, userId)
            .retrieve()
            .bodyToMono<List<OrderResponse>>()
            .timeout(timeout)

    /**
     * 商品詳細を取得する。商品の 404 も含めて、ここでは例外のまま流す。
     * ProductRepository の実装（アダプタ）が一律に DownstreamFailureException（502）へまとめる。
     */
    fun fetchProduct(productId: String): Mono<ProductResponse> =
        webClient.get()
            .uri(PRODUCT_PATH, productId)
            .retrieve()
            .bodyToMono<ProductResponse>()
            .timeout(timeout)

    private companion object {
        // パスはURIテンプレート。{id} は uri() の引数で埋まる。ホスト部分は WebClientConfig の baseUrl が担う。
        const val USER_PATH = "/users/{id}"
        const val USER_ORDERS_PATH = "/users/{id}/orders"
        const val PRODUCT_PATH = "/products/{id}"
    }
}
