package com.example.bff.restclient.infrastructure.client

import com.example.bff.restclient.domain.exception.DownstreamException
import com.example.bff.restclient.domain.exception.UserNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException

private const val USER_ID = "u1"
private const val USER_DESCRIPTION = "GET /users/{id} (u1)"
private const val ORDERS_DESCRIPTION = "GET /users/{id}/orders (u1)"

/**
 * 下流の例外からドメイン例外への変換（ACL のエラー変換, infrastructure 層）の単体テスト。
 * ユーザー取得は 404 を UserNotFound にし、注文・商品取得は 404 も含めて DownstreamException にする規則を確認する。
 */
class DownstreamErrorTranslatorTest {

    @Test
    fun `maps a downstream 404 for the user to UserNotFoundException keeping the original error as cause`() {
        // Arrange
        val notFound = clientError(HttpStatus.NOT_FOUND)
        assertInstanceOf(HttpClientErrorException.NotFound::class.java, notFound)

        // Act
        val error = assertThrows<UserNotFoundException> {
            userCall<String>(USER_ID, USER_DESCRIPTION) { throw notFound }
        }

        // Assert
        assertEquals(USER_ID, error.userId)
        assertEquals("user not found: u1", error.message)
        assertEquals(notFound, error.cause)
    }

    @Test
    fun `maps a downstream 500 for the user to DownstreamException`() {
        // Act & Assert
        assertThrows<DownstreamException> {
            userCall<String>(USER_ID, USER_DESCRIPTION) { throw serverError() }
        }
    }

    @Test
    fun `maps a downstream 404 for orders to DownstreamException instead of a not found`() {
        // Act & Assert
        assertThrows<DownstreamException> {
            downstreamCall<List<String>>(ORDERS_DESCRIPTION) { throw clientError(HttpStatus.NOT_FOUND) }
        }
    }

    @Test
    fun `keeps the downstream call description in the DownstreamException message`() {
        // Act
        val error = assertThrows<DownstreamException> {
            downstreamCall<String>(USER_DESCRIPTION) { throw ResourceAccessException("I/O error") }
        }

        // Assert
        assertEquals("downstream call failed: GET /users/{id} (u1)", error.message)
    }

    @Test
    fun `maps a timeout or connection failure to DownstreamException`() {
        // Act & Assert
        assertThrows<DownstreamException> {
            userCall<String>(USER_ID, USER_DESCRIPTION) { throw ResourceAccessException("read timed out") }
        }
    }

    @Test
    fun `maps an empty response body to DownstreamException`() {
        // Act
        val error = assertThrows<DownstreamException> {
            downstreamCall<String>(ORDERS_DESCRIPTION) { null }
        }

        // Assert
        assertEquals("empty response body from GET /users/{id}/orders (u1)", error.message)
    }

    @Test
    fun `maps a validation failure of a downstream id to DownstreamException`() {
        // Act & Assert
        assertThrows<DownstreamException> {
            downstreamCall<String>(USER_DESCRIPTION) { throw IllegalArgumentException("UserId must not be blank") }
        }
    }

    @Test
    fun `lets an unexpected runtime error pass through without being converted`() {
        // Act & Assert: 広く捕まえると意図しない例外まで 502 に潰れるため、対象外の例外はそのまま上げる。
        assertThrows<IllegalStateException> {
            downstreamCall<String>(USER_DESCRIPTION) { throw IllegalStateException("unexpected") }
        }
    }

    private fun clientError(status: HttpStatus): HttpClientErrorException =
        HttpClientErrorException.create(status, status.reasonPhrase, HttpHeaders.EMPTY, ByteArray(0), null)

    private fun serverError(): HttpServerErrorException =
        HttpServerErrorException.create(
            HttpStatus.INTERNAL_SERVER_ERROR,
            HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase,
            HttpHeaders.EMPTY,
            ByteArray(0),
            null,
        )
}
