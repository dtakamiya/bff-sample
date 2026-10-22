package com.example.bff.webflux.service

import com.example.bff.webflux.client.DownstreamClient
import com.example.bff.webflux.client.OrderResponse
import com.example.bff.webflux.client.ProductResponse
import com.example.bff.webflux.client.UserResponse
import com.example.bff.webflux.model.DashboardOrder
import com.example.bff.webflux.model.DashboardProduct
import com.example.bff.webflux.model.DashboardResponse
import com.example.bff.webflux.model.DashboardUser
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class DashboardService(
    private val downstream: DownstreamClient,
) {
    /**
     * Fetches user and orders in parallel, then product details in parallel.
     * The user's 404 is checked before any other failure so the outcome does not depend on call ordering.
     */
    fun fetchDashboard(userId: String): Mono<DashboardResponse> =
        Mono.zip(
            settle(downstream.fetchUser(userId)),
            settle(downstream.fetchOrders(userId)),
        ) { userOutcome, ordersOutcome -> userOutcome to ordersOutcome }
            .flatMap { (userOutcome, ordersOutcome) ->
                val user = userOutcome.userOrThrow(userId)
                val orders = ordersOutcome.valueOrDownstreamFailure()
                fetchProducts(orders).map { products -> assemble(user, orders, products) }
            }

    private fun fetchProducts(orders: List<OrderResponse>): Mono<Map<String, ProductResponse>> =
        Flux.fromIterable(orders.map { it.productId }.distinct())
            .flatMap { productId -> downstream.fetchProduct(productId).map { productId to it } }
            .collectMap({ it.first }, { it.second })
            .onErrorMap { DownstreamFailureException(it) }

    private fun assemble(
        user: UserResponse,
        orders: List<OrderResponse>,
        products: Map<String, ProductResponse>,
    ): DashboardResponse {
        val dashboardOrders = orders.map { order ->
            val product = products.getValue(order.productId)
            DashboardOrder(
                orderId = order.orderId,
                quantity = order.quantity,
                product = DashboardProduct(id = product.id, name = product.name, price = product.price),
            )
        }
        return DashboardResponse(
            user = DashboardUser(id = user.id, name = user.name, tier = user.tier),
            orders = dashboardOrders,
            totalAmount = dashboardOrders.sumOf { it.product.price.toLong() * it.quantity },
        )
    }

    private sealed interface Outcome<out T> {
        data class Success<out T>(val value: T) : Outcome<T>
        data class Failure(val cause: Throwable) : Outcome<Nothing>
    }

    private fun <T : Any> settle(call: Mono<T>): Mono<Outcome<T>> =
        call.map<Outcome<T>> { Outcome.Success(it) }
            .onErrorResume { Mono.just(Outcome.Failure(it)) }

    private fun Outcome<UserResponse>.userOrThrow(userId: String): UserResponse = when (this) {
        is Outcome.Success -> value
        is Outcome.Failure -> throw if (cause.isNotFound()) {
            UserNotFoundException(userId)
        } else {
            DownstreamFailureException(cause)
        }
    }

    private fun <T : Any> Outcome<T>.valueOrDownstreamFailure(): T = when (this) {
        is Outcome.Success -> value
        is Outcome.Failure -> throw DownstreamFailureException(cause)
    }

    private fun Throwable.isNotFound(): Boolean =
        this is WebClientResponseException && statusCode.value() == HttpStatus.NOT_FOUND.value()
}
