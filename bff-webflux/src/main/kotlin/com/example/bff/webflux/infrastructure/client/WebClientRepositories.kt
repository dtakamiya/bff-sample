package com.example.bff.webflux.infrastructure.client

import com.example.bff.webflux.domain.exception.toDomainFailure
import com.example.bff.webflux.domain.model.Order
import com.example.bff.webflux.domain.model.Product
import com.example.bff.webflux.domain.model.ProductId
import com.example.bff.webflux.domain.model.User
import com.example.bff.webflux.domain.model.UserId
import com.example.bff.webflux.domain.repository.OrderRepository
import com.example.bff.webflux.domain.repository.ProductRepository
import com.example.bff.webflux.domain.repository.UserRepository
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

/*
 * ドメインのリポジトリ（ポート）の下流実装（アダプタ）。infrastructure 層。
 * DownstreamClient で取得し、ACL で変換した結果を返す。失敗は DomainException に変換して流す。
 * 3 つのアダプタは、失敗の変換規則が異なる点に注意（DownstreamErrorTranslator を参照）。
 */

/** ユーザーの取得。下流 404 は UserNotFoundException、それ以外の失敗は DownstreamFailureException。 */
@Component
class WebClientUserRepository(
    private val downstream: DownstreamClient,
) : UserRepository {

    override fun findById(id: UserId): Mono<User> =
        downstream.fetchUser(id.value)
            // 変換（ACL）で値オブジェクトの検証に失敗した場合も、この onErrorMap で 502 側に入る。
            .map { response -> response.toDomain() }
            .onErrorMap { error -> error.toUserFailure(id) }
}

/** 注文一覧の取得。失敗は（404 を含め）DownstreamFailureException。注文 0 件は空リスト。 */
@Component
class WebClientOrderRepository(
    private val downstream: DownstreamClient,
) : OrderRepository {

    override fun findByUserId(userId: UserId): Mono<List<Order>> =
        downstream.fetchOrders(userId.value)
            .map { responses -> responses.map { response -> response.toDomain() } }
            .onErrorMap { error -> error.toDomainFailure() }
}

/** 商品の取得。失敗は（404 を含め）DownstreamFailureException に集約する。 */
@Component
class WebClientProductRepository(
    private val downstream: DownstreamClient,
) : ProductRepository {

    override fun findById(id: ProductId): Mono<Product> =
        downstream.fetchProduct(id.value)
            .map { response -> response.toDomain() }
            .onErrorMap { error -> error.toDomainFailure() }
}
