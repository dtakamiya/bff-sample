package com.example.bff.restclient.sequential.client

import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

private const val USER_PATH = "/users/{id}"
private const val USER_ORDERS_PATH = "/users/{id}/orders"
private const val PRODUCT_PATH = "/products/{id}"

@Component
class DownstreamClient(
    private val restClient: RestClient,
) {
    fun fetchUser(userId: String): DownstreamUser =
        downstreamCall("GET $USER_PATH ($userId)") {
            try {
                restClient.get().uri(USER_PATH, userId).retrieve().body(DownstreamUser::class.java)
            } catch (e: HttpClientErrorException.NotFound) {
                throw UserNotFoundException(userId, e)
            }
        }

    fun fetchOrders(userId: String): List<DownstreamOrder> =
        downstreamCall("GET $USER_ORDERS_PATH ($userId)") {
            restClient.get()
                .uri(USER_ORDERS_PATH, userId)
                .retrieve()
                .body(object : ParameterizedTypeReference<List<DownstreamOrder>>() {})
        }

    fun fetchProduct(productId: String): DownstreamProduct =
        downstreamCall("GET $PRODUCT_PATH ($productId)") {
            restClient.get().uri(PRODUCT_PATH, productId).retrieve().body(DownstreamProduct::class.java)
        }

    /**
     * Runs a downstream call and normalizes every failure into [DownstreamException].
     * [UserNotFoundException] is raised inside [call] and passes through unchanged.
     */
    private inline fun <T : Any> downstreamCall(description: String, call: () -> T?): T =
        try {
            call() ?: throw DownstreamException("empty response body from $description")
        } catch (e: RestClientException) {
            throw DownstreamException("downstream call failed: $description", e)
        }
}
