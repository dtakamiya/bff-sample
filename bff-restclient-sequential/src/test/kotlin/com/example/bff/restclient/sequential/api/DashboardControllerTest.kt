package com.example.bff.restclient.sequential.api

import com.example.bff.restclient.sequential.FAKE_ORDERS_FAILURE_USER_ID
import com.example.bff.restclient.sequential.FAKE_PRODUCT_PRICE
import com.example.bff.restclient.sequential.FAKE_SLOW_USER_ID
import com.example.bff.restclient.sequential.FAKE_UNKNOWN_USER_ID
import com.example.bff.restclient.sequential.FakeDownstreamServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

private const val TEST_TIMEOUT_MS = 300L
private const val DASHBOARD_PATH = "/api/dashboard"
private const val HAPPY_USER_ID = "u1"
private const val EXPECTED_TOTAL_AMOUNT = 3600L

@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `aggregates user, orders and products into dashboard with total amount`() {
        mockMvc.get("$DASHBOARD_PATH/$HAPPY_USER_ID").andExpect {
            status { isOk() }
            jsonPath("$.user.id") { value(HAPPY_USER_ID) }
            jsonPath("$.user.tier") { value("gold") }
            jsonPath("$.orders.length()") { value(2) }
            jsonPath("$.orders[0].orderId") { value("o1") }
            jsonPath("$.orders[0].quantity") { value(2) }
            jsonPath("$.orders[0].product.id") { value("p1") }
            jsonPath("$.orders[0].product.price") { value(FAKE_PRODUCT_PRICE) }
            jsonPath("$.totalAmount") { value(EXPECTED_TOTAL_AMOUNT) }
        }
    }

    @Test
    fun `returns 404 when the downstream user is not found`() {
        mockMvc.get("$DASHBOARD_PATH/$FAKE_UNKNOWN_USER_ID").andExpect {
            status { isNotFound() }
            jsonPath("$.error") { value("user not found: $FAKE_UNKNOWN_USER_ID") }
        }
    }

    @Test
    fun `returns 502 when the downstream orders endpoint fails`() {
        mockMvc.get("$DASHBOARD_PATH/$FAKE_ORDERS_FAILURE_USER_ID").andExpect {
            status { isBadGateway() }
            jsonPath("$.error") { value("downstream service unavailable") }
        }
    }

    @Test
    fun `returns 502 when a downstream call exceeds the configured timeout`() {
        mockMvc.get("$DASHBOARD_PATH/$FAKE_SLOW_USER_ID").andExpect {
            status { isBadGateway() }
            jsonPath("$.error") { value("downstream service unavailable") }
        }
    }

    companion object {
        private val fakeDownstream = FakeDownstreamServer()

        @JvmStatic
        @DynamicPropertySource
        fun downstreamProperties(registry: DynamicPropertyRegistry) {
            registry.add("downstream.base-url") { fakeDownstream.baseUrl }
            registry.add("downstream.timeout-ms") { TEST_TIMEOUT_MS }
        }

        @JvmStatic
        @AfterAll
        fun stopFakeDownstream() {
            fakeDownstream.stop()
        }
    }
}
