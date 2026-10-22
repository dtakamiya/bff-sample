package com.example.bff.restclient.infrastructure.client

import com.example.bff.restclient.domain.exception.DownstreamException
import com.example.bff.restclient.domain.exception.UserNotFoundException
import com.example.bff.restclient.domain.model.UserId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient

/*
 * リポジトリ（アダプタ）の単体テスト。DownstreamClient を差し替えて、
 * 「下流の応答 → 変換 → 例外の変換」が一続きで効くことを確認する（変換と例外変換の組み合わせ）。
 * DownstreamClient は kotlin-spring プラグインにより open のため、継承して応答を固定できる。
 */
class RestClientRepositoriesTest {

    @Test
    fun `reports an order with a blank product id from downstream as DownstreamException`() {
        // Arrange
        val downstream = object : DownstreamClient(RestClient.create()) {
            override fun fetchOrders(userId: String): List<DownstreamOrder>? =
                listOf(DownstreamOrder(orderId = "o1", productId = " ", quantity = 1))
        }
        val repository = RestClientOrderRepository(downstream)

        // Act & Assert: 値オブジェクトの検証失敗は ACL 側で 502 相当（DownstreamException）になる。
        assertThrows<DownstreamException> { repository.findByUserId(UserId("u1")) }
    }

    @Test
    fun `reports a downstream 404 for the user as UserNotFoundException through the adapter`() {
        // Arrange
        val downstream = object : DownstreamClient(RestClient.create()) {
            override fun fetchUser(userId: String): DownstreamUser? =
                throw HttpClientErrorException.create(
                    HttpStatus.NOT_FOUND,
                    HttpStatus.NOT_FOUND.reasonPhrase,
                    HttpHeaders.EMPTY,
                    ByteArray(0),
                    null,
                )
        }
        val repository = RestClientUserRepository(downstream)

        // Act & Assert
        assertThrows<UserNotFoundException> { repository.findById(UserId("u1")) }
    }
}
