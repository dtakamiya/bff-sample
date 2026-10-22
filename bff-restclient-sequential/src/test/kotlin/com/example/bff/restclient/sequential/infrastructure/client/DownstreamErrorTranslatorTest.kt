package com.example.bff.restclient.sequential.infrastructure.client

import com.example.bff.restclient.sequential.domain.exception.DownstreamException
import com.example.bff.restclient.sequential.domain.exception.UserNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResourceAccessException

/**
 * 下流の例外からドメイン例外への変換（ACL, infrastructure 層）の単体テスト。
 * ユーザー取得は 404 を UserNotFound にし、注文・商品取得は 404 も含めて DownstreamException にする規則を確認する。
 */
class DownstreamErrorTranslatorTest {

    private val userId = "u1"

    @Test
    fun `maps a downstream 404 for the user to UserNotFoundException`() {
        // Arrange
        val error = httpError(HttpStatus.NOT_FOUND)

        // Act
        val mapped = error.toUserFailure(userId, "user(u1)")

        // Assert
        assertInstanceOf(UserNotFoundException::class.java, mapped)
        assertEquals("user not found: u1", mapped.message)
    }

    @Test
    fun `maps a downstream 500 for the user to DownstreamException`() {
        // Arrange
        val error = httpError(HttpStatus.INTERNAL_SERVER_ERROR)

        // Act
        val mapped = error.toUserFailure(userId, "user(u1)")

        // Assert
        assertInstanceOf(DownstreamException::class.java, mapped)
    }

    @Test
    fun `maps a timeout for the user to DownstreamException`() {
        // Arrange
        val error = ResourceAccessException("read timed out")

        // Act
        val mapped = error.toUserFailure(userId, "user(u1)")

        // Assert
        assertInstanceOf(DownstreamException::class.java, mapped)
    }

    @Test
    fun `maps a validation failure of a downstream id to DownstreamException`() {
        // Arrange
        val error = IllegalArgumentException("UserId must not be blank")

        // Act
        val mapped = error.toUserFailure(userId, "user(u1)")

        // Assert
        assertInstanceOf(DownstreamException::class.java, mapped)
    }

    @Test
    fun `maps a downstream 404 for orders or products to DownstreamException instead of a not found`() {
        // Arrange
        val error = httpError(HttpStatus.NOT_FOUND)

        // Act
        val mapped = error.toDomainFailure("product(p1)")

        // Assert
        assertInstanceOf(DownstreamException::class.java, mapped)
        assertEquals("downstream call failed: product(p1)", mapped.message)
    }

    @Test
    fun `keeps an existing domain exception instead of wrapping it again`() {
        // Arrange
        val original = DownstreamException("cause")

        // Act
        val mapped = original.toDomainFailure("orders(u1)")

        // Assert
        assertSame(original, mapped)
    }

    @Test
    fun `translatingErrors rethrows the failure as DownstreamException`() {
        // Arrange
        val failing = { throw ResourceAccessException("read timed out") }

        // Act & Assert
        assertThrows<DownstreamException> { translatingErrors("orders(u1)", failing) }
    }

    @Test
    fun `requireBody turns a null body into DownstreamException`() {
        // Arrange
        val body: String? = null

        // Act & Assert
        assertThrows<DownstreamException> { body.requireBody("orders(u1)") }
    }

    private fun httpError(status: HttpStatus): HttpClientErrorException =
        HttpClientErrorException.create(status, status.reasonPhrase, HttpHeaders.EMPTY, ByteArray(0), null)
}
