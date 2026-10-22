package com.example.bff.restclient.sequential.application.usecase

import com.example.bff.restclient.sequential.domain.exception.DownstreamException
import com.example.bff.restclient.sequential.domain.exception.UserNotFoundException
import com.example.bff.restclient.sequential.domain.model.Order
import com.example.bff.restclient.sequential.domain.model.OrderId
import com.example.bff.restclient.sequential.domain.model.Product
import com.example.bff.restclient.sequential.domain.model.ProductId
import com.example.bff.restclient.sequential.domain.model.User
import com.example.bff.restclient.sequential.domain.model.UserId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * GetDashboardUseCase（application 層）の単体テスト（逐次版）。
 * 下流の実装を使わず、ポートの Stub だけで呼び出し順と打ち切りの振る舞いを確認する。
 */
class GetDashboardUseCaseTest {

    private val userId = "u1"
    private val alice = User(id = UserId(userId), name = "Alice", tier = "gold")
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
        val useCase = useCase(calls = mutableListOf())

        // Act
        val result = useCase.execute(userId)

        // Assert
        assertEquals("u1", result.user.id)
        assertEquals(listOf("o1", "o2", "o3"), result.orders.map { it.orderId })
        assertEquals("Book", result.orders.first().product.name)
        assertEquals(2 * 1200L + 1 * 300L + 5 * 1200L, result.totalAmount)
    }

    @Test
    fun `calls user, then orders, then one product per order in order, keeping duplicates`() {
        // Arrange
        val calls = mutableListOf<String>()
        val useCase = useCase(calls = calls)

        // Act
        useCase.execute(userId)

        // Assert
        assertEquals(
            listOf("user", "orders", "product:p1", "product:p2", "product:p1"),
            calls,
        )
    }

    @Test
    fun `stops at user not found without calling orders or products`() {
        // Arrange
        val calls = mutableListOf<String>()
        val useCase = useCase(
            calls = calls,
            users = { throw UserNotFoundException(it.value) },
        )

        // Act & Assert
        assertThrows<UserNotFoundException> { useCase.execute(userId) }
        assertEquals(listOf("user"), calls)
    }

    @Test
    fun `stops at an orders failure without calling any product`() {
        // Arrange
        val calls = mutableListOf<String>()
        val useCase = useCase(
            calls = calls,
            orders = { throw DownstreamException("orders down") },
        )

        // Act & Assert
        assertThrows<DownstreamException> { useCase.execute(userId) }
        assertEquals(listOf("user", "orders"), calls)
    }

    @Test
    fun `propagates a product failure as DownstreamException`() {
        // Arrange
        val useCase = useCase(
            calls = mutableListOf(),
            products = { throw DownstreamException("product down") },
        )

        // Act & Assert
        assertThrows<DownstreamException> { useCase.execute(userId) }
    }

    @Test
    fun `totals zero and calls no product when the user has no orders`() {
        // Arrange
        val calls = mutableListOf<String>()
        val useCase = useCase(calls = calls, orders = { emptyList() })

        // Act
        val result = useCase.execute(userId)

        // Assert
        assertEquals(0L, result.totalAmount)
        assertEquals(emptyList<String>(), result.orders.map { it.orderId })
        assertEquals(listOf("user", "orders"), calls)
    }

    @Test
    fun `treats a blank user id as not found without calling any repository`() {
        // Arrange
        val calls = mutableListOf<String>()
        val useCase = useCase(calls = calls)

        // Act
        val error = assertThrows<UserNotFoundException> { useCase.execute(" ") }

        // Assert
        assertEquals(" ", error.userId)
        assertEquals(emptyList<String>(), calls)
    }

    private fun useCase(
        calls: MutableList<String>,
        users: (UserId) -> User = { alice },
        orders: (UserId) -> List<Order> = { sampleOrders },
        products: (ProductId) -> Product = { id -> if (id == book.id) book else pen },
    ): GetDashboardUseCase = GetDashboardUseCase(
        userRepository = StubUserRepository(calls, users),
        orderRepository = StubOrderRepository(calls, orders),
        productRepository = StubProductRepository(calls, products),
    )
}
