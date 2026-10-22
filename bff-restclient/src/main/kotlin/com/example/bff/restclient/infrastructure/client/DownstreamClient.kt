package com.example.bff.restclient.infrastructure.client

import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * 下流 API（user / orders / product）を RestClient で呼び出す生の窓口（infrastructure 層）。
 *
 * - 呼び出しは RestClient によるブロッキング。戻り値が返るまで呼び出しスレッドを占有する。
 *   このクラス自体は並列化しない。並列化は呼び出し元（application の GetDashboardUseCase）が専用の Executor で行う。
 * - 例外の変換はしない。4xx/5xx・接続失敗・タイムアウトは RestClientException のまま上がる。
 *   ドメイン例外への変換は DownstreamErrorTranslator.kt、DTO からドメイン型への変換は DownstreamMapper.kt が担う。
 * - 戻り値が null なのは「ボディが無い」場合。注文が 0 件のときは空配列 `[]` が返るのが正常。
 */
@Component
class DownstreamClient(
    private val restClient: RestClient,
) {
    /** `GET /users/{id}` の応答。存在しなければ下流は 404 を返し、RestClientException になる。 */
    fun fetchUser(userId: String): DownstreamUser? =
        restClient.get().uri(USER_PATH, userId).retrieve().body(DownstreamUser::class.java)

    /** `GET /users/{id}/orders` の応答。 */
    fun fetchOrders(userId: String): List<DownstreamOrder>? =
        restClient.get()
            .uri(USER_ORDERS_PATH, userId)
            .retrieve()
            // List<DownstreamOrder> は実行時に要素型が消える。匿名サブクラスで型情報を残し、Jackson が要素型を解決できるようにする。
            .body(object : ParameterizedTypeReference<List<DownstreamOrder>>() {})

    /** `GET /products/{id}` の応答。 */
    fun fetchProduct(productId: String): DownstreamProduct? =
        restClient.get().uri(PRODUCT_PATH, productId).retrieve().body(DownstreamProduct::class.java)

    companion object {
        // パスの形はテンプレートで固定し、ID は変数として渡す（文字列連結でパスを組み立てない）。
        // 下流の失敗メッセージ（ログ）にもこのテンプレートを使うため、公開している。
        const val USER_PATH = "/users/{id}"
        const val USER_ORDERS_PATH = "/users/{id}/orders"
        const val PRODUCT_PATH = "/products/{id}"
    }
}
