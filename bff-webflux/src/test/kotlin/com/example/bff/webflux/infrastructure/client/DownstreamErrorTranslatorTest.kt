package com.example.bff.webflux.infrastructure.client

import com.example.bff.webflux.domain.exception.DownstreamFailureException
import com.example.bff.webflux.domain.exception.UserNotFoundException
import com.example.bff.webflux.domain.exception.toDomainFailure
import com.example.bff.webflux.domain.model.UserId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.concurrent.TimeoutException

/**
 * 下流の例外からドメイン例外への変換（ACL, infrastructure 層）の単体テスト。
 * ユーザー取得は 404 を UserNotFound にし、注文・商品取得は 404 も含めて DownstreamFailure にする規則を確認する。
 */
class DownstreamErrorTranslatorTest {

    private val userId = UserId("u1")

    @Test
    fun `maps a downstream 404 for the user to UserNotFoundException`() {
        // Arrange
        val error = httpError(HttpStatus.NOT_FOUND)

        // Act
        val mapped = error.toUserFailure(userId)

        // Assert
        assertInstanceOf(UserNotFoundException::class.java, mapped)
        assertEquals("user not found: u1", mapped.message)
    }

    @Test
    fun `maps a downstream 500 for the user to DownstreamFailureException`() {
        // Arrange
        val error = httpError(HttpStatus.INTERNAL_SERVER_ERROR)

        // Act
        val mapped = error.toUserFailure(userId)

        // Assert
        assertInstanceOf(DownstreamFailureException::class.java, mapped)
    }

    @Test
    fun `maps a timeout for the user to DownstreamFailureException`() {
        // Arrange
        val error = TimeoutException("slow")

        // Act
        val mapped = error.toUserFailure(userId)

        // Assert
        assertInstanceOf(DownstreamFailureException::class.java, mapped)
    }

    @Test
    fun `maps a validation failure of a downstream id to DownstreamFailureException`() {
        // Arrange
        val error = IllegalArgumentException("UserId must not be blank")

        // Act
        val mapped = error.toUserFailure(userId)

        // Assert
        assertInstanceOf(DownstreamFailureException::class.java, mapped)
    }

    @Test
    fun `maps a downstream 404 for orders to DownstreamFailureException instead of a not found`() {
        // Arrange
        val error = httpError(HttpStatus.NOT_FOUND)

        // Act
        val mapped = error.toDomainFailure()

        // Assert
        assertInstanceOf(DownstreamFailureException::class.java, mapped)
    }

    @Test
    fun `keeps an existing domain exception instead of wrapping it again`() {
        // Arrange
        val original = DownstreamFailureException(RuntimeException("cause"))

        // Act
        val mapped = original.toDomainFailure()

        // Assert
        assertSame(original, mapped)
    }

    private fun httpError(status: HttpStatus): WebClientResponseException =
        WebClientResponseException.create(status.value(), status.reasonPhrase, HttpHeaders.EMPTY, ByteArray(0), null, null)
}
