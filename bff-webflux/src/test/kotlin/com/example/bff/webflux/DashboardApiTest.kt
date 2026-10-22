package com.example.bff.webflux

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient

private const val TEST_TIMEOUT_MS = 1000L
private const val USER_ID = "u1"
private const val EXPECTED_ORDER_COUNT = 3
private const val EXPECTED_TOTAL_AMOUNT = 9600
private const val HTTP_BAD_GATEWAY = 502

@SpringBootTest(
    webEnvironment = RANDOM_PORT,
    properties = ["downstream.timeout-ms=$TEST_TIMEOUT_MS"],
)
class DashboardApiTest {

    @LocalServerPort
    private var port: Int = 0

    @Test
    fun `returns aggregated dashboard with total amount for an existing user`() {
        get("/api/dashboard/$USER_ID")
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.user.id").isEqualTo(USER_ID)
            .jsonPath("$.orders.length()").isEqualTo(EXPECTED_ORDER_COUNT)
            .jsonPath("$.totalAmount").isEqualTo(EXPECTED_TOTAL_AMOUNT)
    }

    @Test
    fun `returns 404 when the downstream user does not exist even though orders also fail`() {
        get("/api/dashboard/$UNKNOWN_ID")
            .expectStatus().isNotFound()
            .expectBody()
            .jsonPath("$.error").isEqualTo("user not found: $UNKNOWN_ID")
    }

    @Test
    fun `returns 502 when a downstream call answers with 500`() {
        get("/api/dashboard/$BROKEN_ID")
            .expectStatus().isEqualTo(HTTP_BAD_GATEWAY)
            .expectBody()
            .jsonPath("$.error").isEqualTo(DOWNSTREAM_ERROR_MESSAGE)
    }

    @Test
    fun `returns 502 when a downstream call exceeds the timeout`() {
        get("/api/dashboard/$SLOW_ID")
            .expectStatus().isEqualTo(HTTP_BAD_GATEWAY)
            .expectBody()
            .jsonPath("$.error").isEqualTo(DOWNSTREAM_ERROR_MESSAGE)
    }

    private fun get(path: String): WebTestClient.ResponseSpec =
        WebTestClient.bindToServer()
            .baseUrl("http://localhost:$port")
            .build()
            .get()
            .uri(path)
            .exchange()

    companion object {
        private const val DOWNSTREAM_ERROR_MESSAGE = "downstream service unavailable"

        private val downstream: FakeDownstream = FakeDownstream.start()

        @JvmStatic
        @DynamicPropertySource
        fun downstreamProperties(registry: DynamicPropertyRegistry) {
            registry.add("downstream.base-url") { downstream.baseUrl }
        }

        @JvmStatic
        @AfterAll
        fun stopDownstream() {
            downstream.close()
        }
    }
}
