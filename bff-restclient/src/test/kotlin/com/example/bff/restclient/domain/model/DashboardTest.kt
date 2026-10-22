package com.example.bff.restclient.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Dashboard 集約の合計計算（domain 層）を確認する単体テスト。 */
class DashboardTest {

    private val alice = User(id = UserId("u1"), name = "Alice", tier = "gold")
    private val book = Product(id = ProductId("p1"), name = "Book", price = 1200)
    private val pen = Product(id = ProductId("p2"), name = "Pen", price = 300)

    @Test
    fun `total amount sums unit price times quantity over every order line`() {
        // Arrange
        val lines = listOf(
            OrderLine(order = Order(id = OrderId("o1"), productId = book.id, quantity = 2), product = book),
            OrderLine(order = Order(id = OrderId("o2"), productId = pen.id, quantity = 5), product = pen),
        )

        // Act
        val dashboard = Dashboard(user = alice, orderLines = lines)

        // Assert
        assertEquals(2 * 1200L + 5 * 300L, dashboard.totalAmount)
    }

    @Test
    fun `widens each line to Long before multiplying so a large price does not overflow`() {
        // Arrange
        val expensive = Product(id = ProductId("p9"), name = "Gold", price = Int.MAX_VALUE)
        val line = OrderLine(order = Order(id = OrderId("o9"), productId = expensive.id, quantity = 2), product = expensive)

        // Act
        val dashboard = Dashboard(user = alice, orderLines = listOf(line))

        // Assert
        assertEquals(2L * Int.MAX_VALUE, dashboard.totalAmount)
    }

    @Test
    fun `total amount is zero when the user has no orders`() {
        // Act
        val dashboard = Dashboard(user = alice, orderLines = emptyList())

        // Assert
        assertEquals(0L, dashboard.totalAmount)
    }
}
