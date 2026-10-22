package com.example.bff.restclient.presentation

import com.example.bff.restclient.FakeDownstreamServer
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
 * 空白のみのユーザー ID（`/api/dashboard/%20`）は、ユーザー不在と同じ 404 になることを確認する回帰テスト。
 * 下流には問い合わせず、利用者の入力の時点で不在として扱う。
 */
@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerBlankUserIdTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `returns 404 in the same form as user not found when the path user id is whitespace only`() {
        // Arrange: パスの URL エンコードは MockMvc の URI 変数展開に任せ、二重エンコードを避ける。

        // Act & Assert
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
