package com.example.bff.webclient.infrastructure.client

import com.example.bff.webclient.domain.model.Order
import com.example.bff.webclient.domain.model.OrderId
import com.example.bff.webclient.domain.model.Product
import com.example.bff.webclient.domain.model.ProductId
import com.example.bff.webclient.domain.model.User
import com.example.bff.webclient.domain.model.UserId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** 下流 DTO からドメイン型への変換（ACL, infrastructure 層）の単体テスト。 */
class DownstreamMapperTest {

    @Test
    fun `converts a downstream user into a domain user with a typed id`() {
        // Arrange
        val response = UserResponse(id = "u1", name = "Alice", tier = "gold")

        // Act
        val user = response.toDomain()

        // Assert
        assertEquals(User(id = UserId("u1"), name = "Alice", tier = "gold"), user)
    }

    @Test
    fun `converts a downstream order into a domain order with typed identifiers`() {
        // Arrange
        val response = OrderResponse(orderId = "o1", productId = "p1", quantity = 3)

        // Act
        val order = response.toDomain()

        // Assert
        assertEquals(Order(id = OrderId("o1"), productId = ProductId("p1"), quantity = 3), order)
    }

    @Test
    fun `converts a downstream product into a domain product keeping the price as given`() {
        // Arrange
        val response = ProductResponse(id = "p1", name = "Book", price = 1200)

        // Act
        val product = response.toDomain()

        // Assert
        assertEquals(Product(id = ProductId("p1"), name = "Book", price = 1200), product)
    }

    @Test
    fun `rejects a downstream user whose id is blank`() {
        // Arrange
        val response = UserResponse(id = " ", name = "Alice", tier = "gold")

        // Act & Assert
        assertThrows<IllegalArgumentException> { response.toDomain() }
    }
}
