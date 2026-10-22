package com.example.bff.restclient.service

import com.example.bff.restclient.api.DashboardResponse
import com.example.bff.restclient.api.OrderSummary
import com.example.bff.restclient.api.ProductSummary
import com.example.bff.restclient.api.UserSummary
import com.example.bff.restclient.client.DownstreamClient
import com.example.bff.restclient.client.DownstreamOrder
import com.example.bff.restclient.client.DownstreamProduct
import com.example.bff.restclient.client.DownstreamUser
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutorService

@Service
class DashboardService(
    private val downstreamClient: DownstreamClient,
    private val downstreamExecutor: ExecutorService,
) {
    /**
     * Aggregates user, orders and products. The user future is awaited first so that
     * a user 404 takes precedence over failures of the orders endpoint for the same id.
     */
    fun buildDashboard(userId: String): DashboardResponse {
        val userFuture = supplyAsync { downstreamClient.fetchUser(userId) }
        val ordersFuture = supplyAsync { downstreamClient.fetchOrders(userId) }

        val user = await(userFuture)
        val orders = await(ordersFuture)

        val productFutures = orders.map { order ->
            order to supplyAsync { downstreamClient.fetchProduct(order.productId) }
        }
        val orderSummaries = productFutures.map { (order, productFuture) ->
            toOrderSummary(order, await(productFuture))
        }

        return DashboardResponse(
            user = toUserSummary(user),
            orders = orderSummaries,
            totalAmount = orderSummaries.sumOf { it.product.price.toLong() * it.quantity },
        )
    }

    private fun <T> supplyAsync(call: () -> T): CompletableFuture<T> =
        CompletableFuture.supplyAsync(call, downstreamExecutor)

    private fun <T> await(future: CompletableFuture<T>): T =
        try {
            future.join()
        } catch (e: CompletionException) {
            throw e.cause ?: e
        }

    private fun toUserSummary(user: DownstreamUser): UserSummary =
        UserSummary(id = user.id, name = user.name, tier = user.tier)

    private fun toOrderSummary(order: DownstreamOrder, product: DownstreamProduct): OrderSummary =
        OrderSummary(
            orderId = order.orderId,
            quantity = order.quantity,
            product = ProductSummary(id = product.id, name = product.name, price = product.price),
        )
}
