package com.example.bff.restclient.sequential

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.Executors

const val FAKE_UNKNOWN_USER_ID = "unknown"
const val FAKE_SLOW_USER_ID = "slow"
const val FAKE_ORDERS_FAILURE_USER_ID = "orders-500"
const val FAKE_SLOW_RESPONSE_MS = 1_000L
const val FAKE_PRODUCT_PRICE = 1200

private const val HTTP_OK = 200
private const val HTTP_NOT_FOUND = 404
private const val HTTP_SERVER_ERROR = 500
private const val USER_PATH_SEGMENTS = 2
private const val USER_ORDERS_PATH_SEGMENTS = 3
private const val ANY_PORT = 0
private const val NO_BACKLOG = 0

/**
 * Minimal stand-in for the backend-stub, served by the JDK HTTP server.
 * Behavior is decided purely by the request path so each test case is independent.
 */
class FakeDownstreamServer {
    private val server: HttpServer = HttpServer.create(
        InetSocketAddress(InetAddress.getLoopbackAddress(), ANY_PORT),
        NO_BACKLOG,
    ).apply {
        createContext("/") { exchange -> route(exchange) }
        executor = Executors.newCachedThreadPool()
        start()
    }

    val baseUrl: String = "http://localhost:${server.address.port}"

    fun stop() {
        server.stop(NO_BACKLOG)
    }

    private fun route(exchange: HttpExchange) {
        val segments = exchange.requestURI.path.trim('/').split('/')
        val (status, body) = when {
            segments.size == USER_PATH_SEGMENTS && segments[0] == "users" -> userResponse(segments[1])
            segments.size == USER_ORDERS_PATH_SEGMENTS && segments[0] == "users" && segments[2] == "orders" ->
                ordersResponse(segments[1])
            segments.size == USER_PATH_SEGMENTS && segments[0] == "products" -> productResponse(segments[1])
            else -> HTTP_NOT_FOUND to """{"error":"no route"}"""
        }
        write(exchange, status, body)
    }

    private fun userResponse(userId: String): Pair<Int, String> = when (userId) {
        FAKE_UNKNOWN_USER_ID -> HTTP_NOT_FOUND to """{"error":"not found"}"""
        FAKE_SLOW_USER_ID -> {
            Thread.sleep(FAKE_SLOW_RESPONSE_MS)
            HTTP_OK to userJson(userId)
        }
        else -> HTTP_OK to userJson(userId)
    }

    private fun ordersResponse(userId: String): Pair<Int, String> =
        if (userId == FAKE_ORDERS_FAILURE_USER_ID) {
            HTTP_SERVER_ERROR to """{"error":"injected failure"}"""
        } else {
            HTTP_OK to """[{"orderId":"o1","productId":"p1","quantity":2},""" +
                """{"orderId":"o2","productId":"p2","quantity":1}]"""
        }

    private fun productResponse(productId: String): Pair<Int, String> =
        HTTP_OK to """{"id":"$productId","name":"Product $productId","price":$FAKE_PRODUCT_PRICE}"""

    private fun userJson(userId: String): String =
        """{"id":"$userId","name":"User $userId","tier":"gold"}"""

    private fun write(exchange: HttpExchange, status: Int, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
        exchange.close()
    }
}
