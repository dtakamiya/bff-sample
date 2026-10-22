package com.example.bff.restclient.sequential.service

import com.example.bff.restclient.sequential.api.DashboardResponse
import com.example.bff.restclient.sequential.api.OrderSummary
import com.example.bff.restclient.sequential.api.ProductSummary
import com.example.bff.restclient.sequential.api.UserSummary
import com.example.bff.restclient.sequential.client.DownstreamClient
import com.example.bff.restclient.sequential.client.DownstreamOrder
import com.example.bff.restclient.sequential.client.DownstreamProduct
import com.example.bff.restclient.sequential.client.DownstreamUser
import org.springframework.stereotype.Service

@Service
class DashboardService(
    private val downstreamClient: DownstreamClient,
) {
    /**
     * Aggregates user, orders and products by calling the downstream one after another.
     * The user is fetched first, so a user 404 returns before the orders endpoint is called.
     */
    fun buildDashboard(userId: String): DashboardResponse {
        val user = downstreamClient.fetchUser(userId)
        val orders = downstreamClient.fetchOrders(userId)

        val orderSummaries = orders.map { order ->
            toOrderSummary(order, downstreamClient.fetchProduct(order.productId))
        }

        return DashboardResponse(
            user = toUserSummary(user),
            orders = orderSummaries,
            totalAmount = orderSummaries.sumOf { it.product.price.toLong() * it.quantity },
        )
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
