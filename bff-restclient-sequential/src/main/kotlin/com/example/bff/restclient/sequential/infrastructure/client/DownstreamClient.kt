package com.example.bff.restclient.sequential.infrastructure.client

import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

private const val USER_PATH = "/users/{id}"
private const val USER_ORDERS_PATH = "/users/{id}/orders"
private const val PRODUCT_PATH = "/products/{id}"

/**
 * 下流 3 エンドポイント（user / orders / product）への HTTP 呼び出しだけを担う（infrastructure 層）。
 *
 * - 戻り値は下流の DTO をそのまま返す。下流が本文なしで応答すると null になる（判定は呼び出し側のアダプタが行う）。
 * - 4xx / 5xx・接続失敗・タイムアウトは RestClientException のまま上がる。ドメイン例外への変換は
 *   [DownstreamErrorTranslator] の役目で、ここでは行わない。
 * - タイムアウトは [com.example.bff.restclient.sequential.infrastructure.config.DownstreamConfig] で RestClient に設定済み。
 */
@Component
class DownstreamClient(
    private val restClient: RestClient,
) {
    /** ユーザーを 1 件取得する。 */
    fun fetchUser(userId: String): DownstreamUser? =
        restClient.get().uri(USER_PATH, userId).retrieve().body(DownstreamUser::class.java)

    /**
     * 注文一覧を取得する。
     * 下流は JSON 配列を返すので、ジェネリクスの型を残すため [ParameterizedTypeReference] を使う
     * （`List<DownstreamOrder>::class.java` だと要素型が消えてしまう）。
     */
    fun fetchOrders(userId: String): List<DownstreamOrder>? =
        restClient.get()
            .uri(USER_ORDERS_PATH, userId)
            .retrieve()
            .body(object : ParameterizedTypeReference<List<DownstreamOrder>>() {})

    /** 商品を 1 件取得する。ダッシュボードでは注文の数だけ呼ばれる（N+1 の形）。 */
    fun fetchProduct(productId: String): DownstreamProduct? =
        restClient.get().uri(PRODUCT_PATH, productId).retrieve().body(DownstreamProduct::class.java)
}
