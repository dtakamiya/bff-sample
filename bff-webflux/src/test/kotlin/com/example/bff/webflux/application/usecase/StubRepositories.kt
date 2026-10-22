package com.example.bff.webflux.application.usecase

import com.example.bff.webflux.domain.model.Order
import com.example.bff.webflux.domain.model.Product
import com.example.bff.webflux.domain.model.ProductId
import com.example.bff.webflux.domain.model.User
import com.example.bff.webflux.domain.model.UserId
import com.example.bff.webflux.domain.repository.OrderRepository
import com.example.bff.webflux.domain.repository.ProductRepository
import com.example.bff.webflux.domain.repository.UserRepository
import reactor.core.publisher.Mono
import java.util.concurrent.CopyOnWriteArrayList

/*
 * ユースケースのテスト用に、ドメインのポートを手書きで差し替える Stub。
 * 振る舞いは呼び出し側がラムダで渡す。value class を引数に取るため、Mockito ではなく手書きにしている。
 */

class StubUserRepository(
    private val behavior: (UserId) -> Mono<User>,
) : UserRepository {
    override fun findById(id: UserId): Mono<User> = behavior(id)
}

class StubOrderRepository(
    private val behavior: (UserId) -> Mono<List<Order>>,
) : OrderRepository {
    override fun findByUserId(userId: UserId): Mono<List<Order>> = behavior(userId)
}

/** 商品の取得要求を記録する Stub。同じ商品を何回取りに行ったかを検証するために使う。 */
class StubProductRepository(
    private val behavior: (ProductId) -> Mono<Product>,
) : ProductRepository {

    private val requested = CopyOnWriteArrayList<ProductId>()

    /** これまでに取得を要求された商品 ID（要求の順）。 */
    val requestedIds: List<ProductId>
        get() = requested.toList()

    override fun findById(id: ProductId): Mono<Product> {
        requested += id
        return behavior(id)
    }
}
