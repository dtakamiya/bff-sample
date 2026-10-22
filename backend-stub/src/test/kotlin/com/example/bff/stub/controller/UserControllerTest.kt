package com.example.bff.stub.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

private const val OVERRIDE_DELAY_MS = 100L
private const val NANOS_PER_MILLI = 1_000_000L

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `GET users id returns user with tier gold`() {
        mockMvc.get("/users/u1").andExpect {
            status { isOk() }
            jsonPath("$.id") { value("u1") }
            jsonPath("$.name") { value("User u1") }
            jsonPath("$.tier") { value("gold") }
        }
    }

    @Test
    fun `GET users unknown id returns 404`() {
        mockMvc.get("/users/unknown").andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `GET users id with fail true returns 500`() {
        mockMvc.get("/users/u1") {
            param("fail", "true")
        }.andExpect {
            status { isInternalServerError() }
        }
    }

    @Test
    fun `GET users id orders returns fixed orders`() {
        mockMvc.get("/users/u1/orders").andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(3) }
            jsonPath("$[0].orderId") { value("o1") }
            jsonPath("$[0].productId") { value("p1") }
            jsonPath("$[0].quantity") { value(2) }
        }
    }

    @Test
    fun `GET users unknown orders returns 404`() {
        mockMvc.get("/users/unknown/orders").andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `GET users id orders with fail true returns 500`() {
        mockMvc.get("/users/u1/orders") {
            param("fail", "true")
        }.andExpect {
            status { isInternalServerError() }
        }
    }

    @Test
    fun `delayMs parameter adds at least the requested latency`() {
        val startedAtNanos = System.nanoTime()

        mockMvc.get("/users/u1") {
            param("delayMs", OVERRIDE_DELAY_MS.toString())
        }.andExpect {
            status { isOk() }
        }

        val elapsedMs = (System.nanoTime() - startedAtNanos) / NANOS_PER_MILLI
        assertThat(elapsedMs).isGreaterThanOrEqualTo(OVERRIDE_DELAY_MS)
    }

    @Test
    fun `negative delayMs returns 400`() {
        mockMvc.get("/users/u1") {
            param("delayMs", "-1")
        }.andExpect {
            status { isBadRequest() }
        }
    }
}
