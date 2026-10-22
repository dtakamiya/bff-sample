package com.example.bff.restclient.sequential.presentation

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

private const val DASHBOARD_PATH = "/api/dashboard/{userId}"
private const val BLANK_USER_ID = " "
private const val TEST_TIMEOUT_MS = 300L

/**
 * 空白のみのパス ID（`/api/dashboard/%20`）は、下流を呼ぶ前に 404（user not found と同じ形式）になることを確かめる。
 * 下流はフェイクを立てるだけで、ここでは呼ばれないはず（呼ばれると 200 になるので、テストが失敗して気づける）。
 */
@SpringBootTest
@AutoConfigureMockMvc
class BlankUserIdApiTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `returns 404 in the user not found format when the path user id is blank`() {
        mockMvc.get(DASHBOARD_PATH, BLANK_USER_ID).andExpect {
            status { isNotFound() }
            jsonPath("$.error") { value("user not found: $BLANK_USER_ID") }
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
