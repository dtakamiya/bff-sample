package com.example.bff.restclient.application.usecase

import com.example.bff.restclient.domain.exception.DownstreamException
import com.example.bff.restclient.domain.exception.UserNotFoundException
import com.example.bff.restclient.domain.model.Order
import com.example.bff.restclient.domain.model.OrderId
import com.example.bff.restclient.domain.model.Product
import com.example.bff.restclient.domain.model.ProductId
import com.example.bff.restclient.domain.model.User
import com.example.bff.restclient.domain.model.UserId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private const val POOL_SIZE = 4
private const val PEER_WAIT_SECONDS = 5L

/**
 * GetDashboardUseCase（application 層）の単体テスト。下流の実装を使わず、ポートの Stub だけで
 * 並列化・失敗の優先順位・商品取得の回数を確認する。
 */
class GetDashboardUseCaseTest {

    private val executor: ExecutorService = Executors.newFixedThreadPool(POOL_SIZE)

    private val alice = User(id = UserId("u1"), name = "Alice", tier = "gold")
    private val book = Product(id = ProductId("p1"), name = "Book", price = 1200)
    private val pen = Product(id = ProductId("p2"), name = "Pen", price = 300)
    private val sampleOrders = listOf(
        Order(id = OrderId("o1"), productId = book.id, quantity = 2),
        Order(id = OrderId("o2"), productId = pen.id, quantity = 1),
        Order(id = OrderId("o3"), productId = book.id, quantity = 5),
    )

    @AfterEach
    fun shutdownExecutor() {
        executor.shutdownNow()
    }

    @Test
    fun `returns the user, orders with product details and the total for an existing user`() {
        // Arrange
        val useCase = useCase()

        // Act
        val result = useCase.execute("u1")

        // Assert
        assertEquals("u1", result.user.id)
        assertEquals(listOf("o1", "o2", "o3"), result.orders.map { it.orderId })
        assertEquals("Book", result.orders.first().product.name)
        assertEquals(2 * 1200L + 1 * 300L + 5 * 1200L, result.totalAmount)
    }

    @Test
    fun `returns an empty order list and zero total when the user has no orders`() {
        // Arrange
        val products = StubProductRepository { error("no product should be fetched") }
        val useCase = useCase(orders = { emptyList() }, productRepository = products)

        // Act
        val result = useCase.execute("u1")

        // Assert
        assertEquals(emptyList<String>(), result.orders.map { it.orderId })
        assertEquals(0L, result.totalAmount)
        assertEquals(emptyList<ProductId>(), products.requestedIds)
    }

    @Test
    fun `starts the user and orders calls concurrently instead of one after the other`() {
        // Arrange: 両方が開始されるまで互いに待つ。直列なら相手が来ず、待ちが切れて失敗する。
        val bothStarted = CountDownLatch(2)
        val useCase = useCase(
            users = { bothStarted.arriveAndWaitForPeer(); alice },
            orders = { bothStarted.arriveAndWaitForPeer(); sampleOrders },
        )

        // Act
        val result = useCase.execute("u1")

        // Assert
        assertEquals("u1", result.user.id)
    }

    @Test
    fun `reports UserNotFound even when the orders call also fails`() {
        // Arrange
        val useCase = useCase(
            users = { throw UserNotFoundException("u1") },
            orders = { throw DownstreamException("orders down") },
        )

        // Act & Assert
        assertThrows<UserNotFoundException> { useCase.execute("u1") }
    }

    @Test
    fun `reports DownstreamException when the user is found but the orders call fails`() {
        // Arrange
        val useCase = useCase(orders = { throw DownstreamException("orders down") })

        // Act & Assert
        assertThrows<DownstreamException> { useCase.execute("u1") }
    }

    @Test
    fun `reports DownstreamException when the user call fails for a reason other than not found`() {
        // Arrange
        val useCase = useCase(users = { throw DownstreamException("500 from downstream") })

        // Act & Assert
        assertThrows<DownstreamException> { useCase.execute("u1") }
    }

    @Test
    fun `reports DownstreamException when a product call fails`() {
        // Arrange
        val useCase = useCase(product = { throw DownstreamException("product down") })

        // Act & Assert
        assertThrows<DownstreamException> { useCase.execute("u1") }
    }

    @Test
    fun `fetches a product once per order even when several orders share it`() {
        // Arrange: 注文 o1 と o3 は同じ商品 p1 を指す。重複排除はしない（現行の挙動を維持する）。
        val products = StubProductRepository { id -> if (id == book.id) book else pen }
        val useCase = useCase(productRepository = products)

        // Act
        useCase.execute("u1")

        // Assert
        assertEquals(listOf("p1", "p1", "p2"), products.requestedIds.map { it.value }.sorted())
    }

    @Test
    fun `reports UserNotFound for a whitespace only user id without calling any downstream repository`() {
        // Arrange: 呼ばれたら IllegalStateException になるので、呼ばれていれば UserNotFound にならず失敗する。
        val useCase = useCase(
            users = { error("user repository must not be called") },
            orders = { error("order repository must not be called") },
        )

        // Act & Assert
        val error = assertThrows<UserNotFoundException> { useCase.execute(" ") }
        assertEquals(" ", error.userId)
    }

    private fun CountDownLatch.arriveAndWaitForPeer() {
        countDown()
        check(await(PEER_WAIT_SECONDS, TimeUnit.SECONDS)) {
            "the other call never started: the calls ran one after the other"
        }
    }

    private fun useCase(
        users: (UserId) -> User = { alice },
        orders: (UserId) -> List<Order> = { sampleOrders },
        product: (ProductId) -> Product = { id -> if (id == book.id) book else pen },
        productRepository: StubProductRepository = StubProductRepository(product),
    ): GetDashboardUseCase = GetDashboardUseCase(
        userRepository = StubUserRepository(users),
        orderRepository = StubOrderRepository(orders),
        productRepository = productRepository,
        downstreamExecutor = executor,
    )
}
