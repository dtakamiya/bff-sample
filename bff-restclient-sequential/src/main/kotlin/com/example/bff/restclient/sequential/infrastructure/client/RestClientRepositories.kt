package com.example.bff.restclient.sequential.infrastructure.client

import com.example.bff.restclient.sequential.domain.model.Order
import com.example.bff.restclient.sequential.domain.model.Product
import com.example.bff.restclient.sequential.domain.model.ProductId
import com.example.bff.restclient.sequential.domain.model.User
import com.example.bff.restclient.sequential.domain.model.UserId
import com.example.bff.restclient.sequential.domain.repository.OrderRepository
import com.example.bff.restclient.sequential.domain.repository.ProductRepository
import com.example.bff.restclient.sequential.domain.repository.UserRepository
import org.springframework.stereotype.Component

/*
 * ドメインのリポジトリ（ポート）の下流実装（アダプタ）。infrastructure 層。
 * DownstreamClient で同期的に取得し、ACL で変換した結果を返す。失敗は DomainException として投げる。
 * 3 つのアダプタは、失敗の変換規則が異なる点に注意（DownstreamErrorTranslator を参照）。
 */

/** ユーザーの取得。下流 404 は UserNotFoundException、それ以外の失敗は DownstreamException。 */
@Component
class RestClientUserRepository(
    private val downstream: DownstreamClient,
) : UserRepository {

    override fun findById(id: UserId): User {
        val operation = "user(${id.value})"
        return translatingUserErrors(id.value, operation) {
            downstream.fetchUser(id.value).requireBody(operation).toDomain()
        }
    }
}

/** 注文一覧の取得。失敗は（404 を含め）DownstreamException。注文 0 件は空リスト。 */
@Component
class RestClientOrderRepository(
    private val downstream: DownstreamClient,
) : OrderRepository {

    override fun findByUserId(userId: UserId): List<Order> {
        val operation = "orders(${userId.value})"
        return translatingErrors(operation) {
            downstream.fetchOrders(userId.value).requireBody(operation).map { it.toDomain() }
        }
    }
}

/** 商品の取得。失敗は（404 を含め）DownstreamException に集約する。 */
@Component
class RestClientProductRepository(
    private val downstream: DownstreamClient,
) : ProductRepository {

    override fun findById(id: ProductId): Product {
        val operation = "product(${id.value})"
        return translatingErrors(operation) {
            downstream.fetchProduct(id.value).requireBody(operation).toDomain()
        }
    }
}
