package com.example.bff.restclient.infrastructure.client

import com.example.bff.restclient.domain.model.Order
import com.example.bff.restclient.domain.model.Product
import com.example.bff.restclient.domain.model.ProductId
import com.example.bff.restclient.domain.model.User
import com.example.bff.restclient.domain.model.UserId
import com.example.bff.restclient.domain.repository.OrderRepository
import com.example.bff.restclient.domain.repository.ProductRepository
import com.example.bff.restclient.domain.repository.UserRepository
import org.springframework.stereotype.Component

/*
 * ドメインのリポジトリ（ポート）の下流実装（アダプタ）。infrastructure 層。
 * DownstreamClient で取得し、ACL で変換した結果を返す。失敗は DomainException に変換して投げる。
 * 3 つのアダプタは失敗の変換規則が異なる（DownstreamErrorTranslator を参照）。
 */

/** ユーザーの取得。下流 404 は UserNotFoundException、それ以外の失敗は DownstreamException。 */
@Component
class RestClientUserRepository(
    private val downstream: DownstreamClient,
) : UserRepository {

    override fun findById(id: UserId): User =
        userCall(id.value, "GET ${DownstreamClient.USER_PATH} (${id.value})") {
            downstream.fetchUser(id.value)?.toDomain()
        }
}

/** 注文一覧の取得。失敗は（404 を含め）DownstreamException。注文 0 件は空リスト。 */
@Component
class RestClientOrderRepository(
    private val downstream: DownstreamClient,
) : OrderRepository {

    override fun findByUserId(userId: UserId): List<Order> =
        downstreamCall("GET ${DownstreamClient.USER_ORDERS_PATH} (${userId.value})") {
            downstream.fetchOrders(userId.value)?.map { it.toDomain() }
        }
}

/** 商品の取得。失敗は（404 を含め）DownstreamException に集約する。 */
@Component
class RestClientProductRepository(
    private val downstream: DownstreamClient,
) : ProductRepository {

    override fun findById(id: ProductId): Product =
        downstreamCall("GET ${DownstreamClient.PRODUCT_PATH} (${id.value})") {
            downstream.fetchProduct(id.value)?.toDomain()
        }
}
