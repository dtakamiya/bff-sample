package com.example.bff.restclient.sequential.client

import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

private const val USER_PATH = "/users/{id}"
private const val USER_ORDERS_PATH = "/users/{id}/orders"
private const val PRODUCT_PATH = "/products/{id}"

/**
 * 下流 3 エンドポイント（user / orders / product）への呼び出しをまとめる。
 *
 * 責務は「HTTP 呼び出し」「下流の JSON → 下流モデル」「例外の正規化」まで。
 * 集約（どの順で呼ぶか）は [com.example.bff.restclient.sequential.service.DashboardService] の役目。
 * このクラス自体は並列・逐次のどちらでも使える作りで、並列版と同じ形を保っている。
 * タイムアウトは [com.example.bff.restclient.sequential.config.DownstreamConfig] で RestClient に設定済み。
 */
@Component
class DownstreamClient(
    private val restClient: RestClient,
) {
    /**
     * ユーザーを 1 件取得する。
     * 404 だけは [UserNotFoundException] にして、ダッシュボードで「存在しない user」として扱えるようにする。
     * 他のエラーは [downstreamCall] 経由で [DownstreamException] になる。
     */
    fun fetchUser(userId: String): DownstreamUser =
        downstreamCall("GET $USER_PATH ($userId)") {
            try {
                restClient.get().uri(USER_PATH, userId).retrieve().body(DownstreamUser::class.java)
            } catch (e: HttpClientErrorException.NotFound) {
                // 404 は「user が存在しない」という業務上の結果なので、障害扱いの DownstreamException にはしない。
                throw UserNotFoundException(userId, e)
            }
        }

    /**
     * 注文一覧を取得する。
     * 下流は JSON 配列を返すので、ジェネリクスの型を残すため [ParameterizedTypeReference] を使う
     * （`List<DownstreamOrder>::class.java` だと要素型が消えてしまう）。
     * 404 はここでは特別扱いせず、404 も含めてすべて [DownstreamException] (502) になる。
     * user の存在は [fetchUser] で先に確認済みの前提。
     */
    fun fetchOrders(userId: String): List<DownstreamOrder> =
        downstreamCall("GET $USER_ORDERS_PATH ($userId)") {
            restClient.get()
                .uri(USER_ORDERS_PATH, userId)
                .retrieve()
                .body(object : ParameterizedTypeReference<List<DownstreamOrder>>() {})
        }

    /**
     * 商品を 1 件取得する。
     * ダッシュボードでは注文の数だけ呼ばれる（N+1 の形）。
     * 商品が 404 でも user の 404 とは区別し、[DownstreamException] (502) にする。
     */
    fun fetchProduct(productId: String): DownstreamProduct =
        downstreamCall("GET $PRODUCT_PATH ($productId)") {
            restClient.get().uri(PRODUCT_PATH, productId).retrieve().body(DownstreamProduct::class.java)
        }

    /**
     * 下流呼び出しを 1 つの例外型に寄せる共通処理。
     *
     * - [RestClientException] 系（4xx/5xx、接続失敗、タイムアウトの ResourceAccessException など）は
     *   [DownstreamException] に包み直す。原因は cause に残す。
     * - ボディが null の場合（空レスポンス）も [DownstreamException] にする。
     * - [UserNotFoundException] は [call] の中で投げられ、`RestClientException` ではないため素通しされる。
     *
     * inline にしているのは、呼び出し側の `try` ブロックの中で非ローカルな例外を扱うため。
     * 実行時のコストはほぼない。
     */
    private inline fun <T : Any> downstreamCall(description: String, call: () -> T?): T =
        try {
            call() ?: throw DownstreamException("empty response body from $description")
        } catch (e: RestClientException) {
            throw DownstreamException("downstream call failed: $description", e)
        }
}
