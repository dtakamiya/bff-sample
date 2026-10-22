package com.example.bff.webclient.client

import com.example.bff.webclient.config.DownstreamProperties
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * Thin WebClient wrapper for the downstream API. Every call is bounded by the configured timeout.
 * Non-2xx responses surface as WebClientResponseException; timeouts as TimeoutException.
 */
@Component
class DownstreamClient(
    private val webClient: WebClient,
    properties: DownstreamProperties,
) {
    private val timeout: Duration = Duration.ofMillis(properties.timeoutMs)

    fun fetchUser(userId: String): Mono<UserResponse> =
        webClient.get()
            .uri(USER_PATH, userId)
            .retrieve()
            .bodyToMono<UserResponse>()
            .timeout(timeout)

    fun fetchOrders(userId: String): Mono<List<OrderResponse>> =
        webClient.get()
            .uri(USER_ORDERS_PATH, userId)
            .retrieve()
            .bodyToMono<List<OrderResponse>>()
            .timeout(timeout)

    fun fetchProduct(productId: String): Mono<ProductResponse> =
        webClient.get()
            .uri(PRODUCT_PATH, productId)
            .retrieve()
            .bodyToMono<ProductResponse>()
            .timeout(timeout)

    private companion object {
        const val USER_PATH = "/users/{id}"
        const val USER_ORDERS_PATH = "/users/{id}/orders"
        const val PRODUCT_PATH = "/products/{id}"
    }
}
