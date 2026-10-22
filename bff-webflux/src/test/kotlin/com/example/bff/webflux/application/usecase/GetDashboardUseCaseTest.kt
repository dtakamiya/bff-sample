package com.example.bff.webflux.application.usecase

import com.example.bff.webflux.domain.exception.DownstreamFailureException
import com.example.bff.webflux.domain.exception.UserNotFoundException
import com.example.bff.webflux.domain.model.Order
import com.example.bff.webflux.domain.model.OrderId
import com.example.bff.webflux.domain.model.Product
import com.example.bff.webflux.domain.model.ProductId
import com.example.bff.webflux.domain.model.User
import com.example.bff.webflux.domain.model.UserId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import reactor.core.publisher.Mono

/**
 * GetDashboardUseCase（application 層）の単体テスト。下流の実装を使わず、ポートの Stub だけで並列化と失敗の優先順位を確認する。
 */
class GetDashboardUseCaseTest {

    private val userId = UserId("u1")
    private val alice = User(id = userId, name = "Alice", tier = "gold")
    private val book = Product(id = ProductId("p1"), name = "Book", price = 1200)
    private val pen = Product(id = ProductId("p2"), name = "Pen", price = 300)
    private val sampleOrders = listOf(
        Order(id = OrderId("o1"), productId = book.id, quantity = 2),
        Order(id = OrderId("o2"), productId = pen.id, quantity = 1),
        Order(id = OrderId("o3"), productId = book.id, quantity = 5),
    )

    @Test
    fun `returns the user, orders with product details and the total for an existing user`() {
        // Arrange
        val useCase = useCase()

        // Act
        val result = checkNotNull(useCase.execute(userId).block())

        // Assert
        assertEquals("u1", result.user.id)
        assertEquals(listOf("o1", "o2", "o3"), result.orders.map { it.orderId })
        assertEquals("Book", result.orders.first().product.name)
        assertEquals(2 * 1200L + 1 * 300L + 5 * 1200L, result.totalAmount)
    }

    @Test
    fun `reports UserNotFound even when the orders call also fails`() {
        // Arrange
        val useCase = useCase(
            users = { Mono.error(UserNotFoundException(userId.value)) },
            orders = { Mono.error(DownstreamFailureException(RuntimeException("orders down"))) },
        )

        // Act & Assert
        assertThrows<UserNotFoundException> { useCase.execute(userId).block() }
    }

    @Test
    fun `reports DownstreamFailure when the user is found but the orders call fails`() {
        // Arrange
        val useCase = useCase(
            orders = { Mono.error(DownstreamFailureException(RuntimeException("orders down"))) },
        )

        // Act & Assert
        assertThrows<DownstreamFailureException> { useCase.execute(userId).block() }
    }

    @Test
    fun `reports DownstreamFailure when the user call fails for a reason other than not found`() {
        // Arrange
        val useCase = useCase(
            users = { Mono.error(DownstreamFailureException(RuntimeException("500 from downstream"))) },
        )

        // Act & Assert
        assertThrows<DownstreamFailureException> { useCase.execute(userId).block() }
    }

    @Test
    fun `reports DownstreamFailure when a product call fails`() {
        // Arrange
        val useCase = useCase(
            product = { Mono.error(DownstreamFailureException(RuntimeException("product down"))) },
        )

        // Act & Assert
        assertThrows<DownstreamFailureException> { useCase.execute(userId).block() }
    }

    @Test
    fun `converts an unexpected repository error into DownstreamFailure`() {
        // Arrange
        val useCase = useCase(
            product = { Mono.error(IllegalStateException("unexpected")) },
        )

        // Act & Assert
        assertThrows<DownstreamFailureException> { useCase.execute(userId).block() }
    }

    @Test
    fun `fetches each distinct product only once even when several orders share it`() {
        // Arrange
        val products = StubProductRepository { id -> Mono.just(if (id == book.id) book else pen) }
        val useCase = useCase(productRepository = products)

        // Act
        useCase.execute(userId).block()

        // Assert
        assertEquals(listOf("p1", "p2"), products.requestedIds.map { it.value }.sorted())
    }

    @Test
    fun `fails with IllegalStateException when a product of an order is missing`() {
        // Arrange
        val useCase = useCase(product = { id -> if (id == pen.id) Mono.empty() else Mono.just(book) })

        // Act & Assert
        assertThrows<IllegalStateException> { useCase.execute(userId).block() }
    }

    private fun useCase(
        users: (UserId) -> Mono<User> = { Mono.just(alice) },
        orders: (UserId) -> Mono<List<Order>> = { Mono.just(sampleOrders) },
        product: (ProductId) -> Mono<Product> = { id -> Mono.just(if (id == book.id) book else pen) },
        productRepository: StubProductRepository = StubProductRepository(product),
    ): GetDashboardUseCase = GetDashboardUseCase(
        userRepository = StubUserRepository(users),
        orderRepository = StubOrderRepository(orders),
        productRepository = productRepository,
    )
}
