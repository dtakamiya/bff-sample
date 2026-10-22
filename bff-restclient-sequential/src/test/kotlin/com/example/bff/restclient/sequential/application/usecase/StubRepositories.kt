package com.example.bff.restclient.sequential.application.usecase

import com.example.bff.restclient.sequential.domain.model.Order
import com.example.bff.restclient.sequential.domain.model.Product
import com.example.bff.restclient.sequential.domain.model.ProductId
import com.example.bff.restclient.sequential.domain.model.User
import com.example.bff.restclient.sequential.domain.model.UserId
import com.example.bff.restclient.sequential.domain.repository.OrderRepository
import com.example.bff.restclient.sequential.domain.repository.ProductRepository
import com.example.bff.restclient.sequential.domain.repository.UserRepository

/*
 * ユースケースのテスト用に、ドメインのポートを手書きで差し替える Stub（テスト専用）。
 * 振る舞いは呼び出し側がラムダで渡す。どの順で呼ばれたかを [calls] に記録し、逐次の順序を検証できるようにしている。
 */

/** user の取得を記録して、指定の振る舞いを返す Stub。 */
class StubUserRepository(
    private val calls: MutableList<String>,
    private val behavior: (UserId) -> User,
) : UserRepository {
    override fun findById(id: UserId): User {
        calls += "user"
        return behavior(id)
    }
}

/** orders の取得を記録して、指定の振る舞いを返す Stub。 */
class StubOrderRepository(
    private val calls: MutableList<String>,
    private val behavior: (UserId) -> List<Order>,
) : OrderRepository {
    override fun findByUserId(userId: UserId): List<Order> {
        calls += "orders"
        return behavior(userId)
    }
}

/** product の取得を「product:<id>」の形で記録して、指定の振る舞いを返す Stub。 */
class StubProductRepository(
    private val calls: MutableList<String>,
    private val behavior: (ProductId) -> Product,
) : ProductRepository {
    override fun findById(id: ProductId): Product {
        calls += "product:${id.value}"
        return behavior(id)
    }
}
