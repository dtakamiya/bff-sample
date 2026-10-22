package com.example.bff.restclient.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** 値オブジェクト（UserId / ProductId / OrderId）の入力検証（domain 層）を確認する単体テスト。 */
class IdentifiersTest {

    @Test
    fun `keeps the raw value of a non blank user id`() {
        // Arrange
        val raw = "u1"

        // Act
        val id = UserId(raw)

        // Assert
        assertEquals(raw, id.value)
    }

    @Test
    fun `rejects an empty user id`() {
        // Act & Assert
        assertThrows<IllegalArgumentException> { UserId("") }
    }

    @Test
    fun `rejects a user id that consists only of whitespace`() {
        // Act & Assert
        assertThrows<IllegalArgumentException> { UserId(" ") }
    }

    @Test
    fun `rejects a product id that consists only of whitespace`() {
        // Act & Assert
        assertThrows<IllegalArgumentException> { ProductId("   ") }
    }

    @Test
    fun `rejects a blank order id`() {
        // Act & Assert
        assertThrows<IllegalArgumentException> { OrderId("\t") }
    }
}
