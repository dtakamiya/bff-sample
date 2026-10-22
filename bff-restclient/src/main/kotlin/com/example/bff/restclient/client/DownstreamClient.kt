package com.example.bff.restclient.client

import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

// パスの形はテンプレートで固定し、ID は変数として渡す（文字列連結でパスを組み立てない）。
private const val USER_PATH = "/users/{id}"
private const val USER_ORDERS_PATH = "/users/{id}/orders"
private const val PRODUCT_PATH = "/products/{id}"

/**
 * 下流 API（user / orders / product）を呼び出すクライアント。
 *
 * - 呼び出しは RestClient によるブロッキング。戻り値が返るまで呼び出しスレッドを占有する。
 *   このクラス自体は並列化しない。並列化は呼び出し元（[com.example.bff.restclient.service.DashboardService]）が
 *   専用の ExecutorService で行う。
 * - 下流の失敗は [DownstreamException] に統一する。ただしユーザーの 404 だけは
 *   [UserNotFoundException] として区別する。「ユーザーが存在しない」と「下流が壊れている」は
 *   利用者への応答（404 と 502）が違うため。
 */
@Component
class DownstreamClient(
    private val restClient: RestClient,
) {
    /**
     * ユーザーを取得する。下流の 404 だけを [UserNotFoundException] に変換する。
     * それ以外（5xx・接続失敗・タイムアウト・空ボディ）は [downstreamCall] 経由で [DownstreamException] になる。
     */
    fun fetchUser(userId: String): DownstreamUser =
        downstreamCall("GET $USER_PATH ($userId)") {
            try {
                restClient.get().uri(USER_PATH, userId).retrieve().body(DownstreamUser::class.java)
            } catch (e: HttpClientErrorException.NotFound) {
                // retrieve() は 4xx/5xx を例外にする。404 だけはここで横取りし、ユーザー不在として扱う。
                throw UserNotFoundException(userId, e)
            }
        }

    /**
     * 注文一覧を取得する。注文が 0 件のときは空配列 `[]` が返るのが正常。
     * 「ボディ自体が無い」場合だけ異常扱いになる。
     */
    fun fetchOrders(userId: String): List<DownstreamOrder> =
        downstreamCall("GET $USER_ORDERS_PATH ($userId)") {
            restClient.get()
                .uri(USER_ORDERS_PATH, userId)
                .retrieve()
                // List<DownstreamOrder> は実行時に要素型が消える。匿名サブクラスで型情報を残し、Jackson が要素型を解決できるようにする。
                .body(object : ParameterizedTypeReference<List<DownstreamOrder>>() {})
        }

    /**
     * 商品を取得する。商品の 404 も [DownstreamException]（最終的に 502）になる。
     * 商品不在はユーザー不在とは別扱いにしない、という現状の方針。
     */
    fun fetchProduct(productId: String): DownstreamProduct =
        downstreamCall("GET $PRODUCT_PATH ($productId)") {
            restClient.get().uri(PRODUCT_PATH, productId).retrieve().body(DownstreamProduct::class.java)
        }

    /**
     * 下流呼び出しを実行し、失敗を [DownstreamException] に正規化する。
     *
     * - 戻り値が null（空ボディ）も異常とみなす。
     * - 捕まえるのは [RestClientException] だけ（4xx/5xx・接続失敗・タイムアウト・JSON 変換失敗がここに来る）。
     *   [UserNotFoundException] は RestClientException ではないので、そのまま上に通り抜ける。
     *   広く Throwable を捕まえると、意図した例外まで 502 に潰れてしまう。
     */
    private inline fun <T : Any> downstreamCall(description: String, call: () -> T?): T =
        try {
            call() ?: throw DownstreamException("empty response body from $description")
        } catch (e: RestClientException) {
            throw DownstreamException("downstream call failed: $description", e)
        }
}
