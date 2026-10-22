package com.example.bff.restclient.sequential.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** Dashboard 集約の合計計算と不変条件（domain 層）を確認する単体テスト。 */
class DashboardTest {

    private val alice = User(id = UserId("u1"), name = "Alice", tier = "gold")
    private val book = Product(id = ProductId("p1"), name = "Book", price = 1200)
    private val pen = Product(id = ProductId("p2"), name = "Pen", price = 300)
    private val productsById = mapOf(book.id to book, pen.id to pen)

    @Test
    fun `total amount sums unit price times quantity over every order line`() {
        // Arrange
        val orders = listOf(
            Order(id = OrderId("o1"), productId = book.id, quantity = 2),
            Order(id = OrderId("o2"), productId = pen.id, quantity = 5),
        )

        // Act
        val dashboard = Dashboard.assemble(alice, orders, productsById)

        // Assert
        assertEquals(2 * 1200L + 5 * 300L, dashboard.totalAmount)
    }

    @Test
    fun `widens each line to Long before multiplying so a large price does not overflow`() {
        // Arrange
        val expensive = Product(id = ProductId("p9"), name = "Gold", price = Int.MAX_VALUE)
        val orders = listOf(Order(id = OrderId("o9"), productId = expensive.id, quantity = 2))

        // Act
        val dashboard = Dashboard.assemble(alice, orders, mapOf(expensive.id to expensive))

        // Assert
        assertEquals(2L * Int.MAX_VALUE, dashboard.totalAmount)
    }

    @Test
    fun `keeps the order lines in the same order as the given orders`() {
        // Arrange
        val orders = listOf(
            Order(id = OrderId("o2"), productId = pen.id, quantity = 1),
            Order(id = OrderId("o1"), productId = book.id, quantity = 1),
        )

        // Act
        val dashboard = Dashboard.assemble(alice, orders, productsById)

        // Assert
        assertEquals(listOf("o2", "o1"), dashboard.orderLines.map { it.order.id.value })
    }

    @Test
    fun `totals zero and has no lines when there are no orders`() {
        // Act
        val dashboard = Dashboard.assemble(alice, emptyList(), productsById)

        // Assert
        assertEquals(0L, dashboard.totalAmount)
        assertEquals(emptyList<OrderLine>(), dashboard.orderLines)
    }

    @Test
    fun `fails with IllegalStateException when an order refers to a product that is missing`() {
        // Arrange
        val orders = listOf(Order(id = OrderId("o1"), productId = ProductId("unknown"), quantity = 1))

        // Act & Assert
        assertThrows<IllegalStateException> { Dashboard.assemble(alice, orders, productsById) }
    }
}
