package com.example.bff.stub.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `GET products id returns product with price 1200`() {
        mockMvc.get("/products/p1").andExpect {
            status { isOk() }
            jsonPath("$.id") { value("p1") }
            jsonPath("$.name") { value("Product p1") }
            jsonPath("$.price") { value(1200) }
        }
    }

    @Test
    fun `GET products unknown id returns 404`() {
        mockMvc.get("/products/unknown").andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `GET products id with fail true returns 500`() {
        mockMvc.get("/products/p1") {
            param("fail", "true")
        }.andExpect {
            status { isInternalServerError() }
        }
    }
}
