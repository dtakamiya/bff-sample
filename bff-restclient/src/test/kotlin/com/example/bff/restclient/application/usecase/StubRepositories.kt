package com.example.bff.restclient.application.usecase

import com.example.bff.restclient.domain.model.Order
import com.example.bff.restclient.domain.model.Product
import com.example.bff.restclient.domain.model.ProductId
import com.example.bff.restclient.domain.model.User
import com.example.bff.restclient.domain.model.UserId
import com.example.bff.restclient.domain.repository.OrderRepository
import com.example.bff.restclient.domain.repository.ProductRepository
import com.example.bff.restclient.domain.repository.UserRepository
import java.util.concurrent.CopyOnWriteArrayList

/*
 * ユースケースのテスト用に、ドメインのポートを手書きで差し替える Stub。
 * 振る舞いは呼び出し側がラムダで渡す。並列に呼ばれるため、記録は並行安全なコレクションにしている。
 */

class StubUserRepository(
    private val behavior: (UserId) -> User,
) : UserRepository {
    override fun findById(id: UserId): User = behavior(id)
}

class StubOrderRepository(
    private val behavior: (UserId) -> List<Order>,
) : OrderRepository {
    override fun findByUserId(userId: UserId): List<Order> = behavior(userId)
}

/** 商品の取得要求を記録する Stub。同じ商品を何回取りに行ったかを検証するために使う。 */
class StubProductRepository(
    private val behavior: (ProductId) -> Product,
) : ProductRepository {

    private val requested = CopyOnWriteArrayList<ProductId>()

    /** これまでに取得を要求された商品 ID（要求の順）。 */
    val requestedIds: List<ProductId>
        get() = requested.toList()

    override fun findById(id: ProductId): Product {
        requested += id
        return behavior(id)
    }
}
