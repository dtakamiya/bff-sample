package com.example.bff.webclient

import com.example.bff.webclient.presentation.dto.DashboardResponse
import com.example.bff.webclient.presentation.dto.ErrorBody
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

private const val TEST_TIMEOUT_MS = 1000L
private const val USER_ID = "u1"
private const val EXPECTED_ORDER_COUNT = 3
private const val EXPECTED_TOTAL_AMOUNT = 9600L
private const val HTTP_OK = 200
private const val HTTP_NOT_FOUND = 404
private const val HTTP_BAD_GATEWAY = 502

@SpringBootTest(
    webEnvironment = RANDOM_PORT,
    properties = ["downstream.timeout-ms=$TEST_TIMEOUT_MS"],
)
class DashboardApiTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val http: HttpClient = HttpClient.newHttpClient()

    @Test
    fun `returns aggregated dashboard with total amount for an existing user`() {
        val response = get("/api/dashboard/$USER_ID")

        assertEquals(HTTP_OK, response.statusCode())
        val dashboard = objectMapper.readValue(response.body(), DashboardResponse::class.java)
        assertEquals(USER_ID, dashboard.user.id)
        assertEquals(EXPECTED_ORDER_COUNT, dashboard.orders.size)
        assertEquals(EXPECTED_TOTAL_AMOUNT, dashboard.totalAmount)
    }

    @Test
    fun `returns 404 when the downstream user does not exist even though orders also fail`() {
        val response = get("/api/dashboard/$UNKNOWN_ID")

        assertEquals(HTTP_NOT_FOUND, response.statusCode())
        assertEquals("user not found: $UNKNOWN_ID", errorOf(response))
    }

    @Test
    fun `returns 404 when the path user id is blank`() {
        val response = get("/api/dashboard/%20")

        assertEquals(HTTP_NOT_FOUND, response.statusCode())
        assertTrue(errorOf(response).startsWith("user not found"))
    }

    @Test
    fun `returns 502 when a downstream call answers with 500`() {
        val response = get("/api/dashboard/$BROKEN_ID")

        assertEquals(HTTP_BAD_GATEWAY, response.statusCode())
        assertEquals(DOWNSTREAM_ERROR_MESSAGE, errorOf(response))
    }

    @Test
    fun `returns 502 when a downstream call exceeds the timeout`() {
        val response = get("/api/dashboard/$SLOW_ID")

        assertEquals(HTTP_BAD_GATEWAY, response.statusCode())
        assertEquals(DOWNSTREAM_ERROR_MESSAGE, errorOf(response))
    }

    private fun get(path: String): HttpResponse<String> {
        val request = HttpRequest.newBuilder(URI.create("http://localhost:$port$path")).GET().build()
        return http.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun errorOf(response: HttpResponse<String>): String =
        objectMapper.readValue(response.body(), ErrorBody::class.java).error

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
