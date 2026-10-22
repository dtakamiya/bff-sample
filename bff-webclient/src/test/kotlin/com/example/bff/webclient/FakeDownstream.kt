package com.example.bff.webclient

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

const val UNKNOWN_ID = "unknown"
const val BROKEN_ID = "broken"
const val SLOW_ID = "slow"
const val SLOW_DELAY_MS = 3000L
const val FAKE_PRODUCT_PRICE = 1200

private const val HTTP_OK = 200
private const val HTTP_NOT_FOUND = 404
private const val HTTP_INTERNAL_ERROR = 500
private const val LOOPBACK_PORT = 0

private val USER_PATH = Regex("/users/([^/]+)")
private val USER_ORDERS_PATH = Regex("/users/([^/]+)/orders")
private val PRODUCT_PATH = Regex("/products/([^/]+)")

private const val USER_JSON = """{"id":"%s","name":"User %s","tier":"gold"}"""
private const val PRODUCT_JSON = """{"id":"%s","name":"Product %s","price":$FAKE_PRODUCT_PRICE}"""
private const val ORDERS_JSON = """[
    {"orderId":"o1","productId":"p1","quantity":2},
    {"orderId":"o2","productId":"p2","quantity":1},
    {"orderId":"o3","productId":"p3","quantity":5}
]"""
private const val ERROR_JSON = """{"error":"%s"}"""

/**
 * Minimal in-process stand-in for the downstream API, built on the JDK HttpServer so no extra test dependency is needed.
 * Ids: UNKNOWN_ID -> 404, BROKEN_ID -> 500 (user only), SLOW_ID -> sleeps SLOW_DELAY_MS before answering.
 */
class FakeDownstream private constructor(
    private val server: HttpServer,
    private val executor: ExecutorService,
) : AutoCloseable {

    val baseUrl: String = "http://localhost:${server.address.port}"

    override fun close() {
        server.stop(0)
        executor.shutdownNow()
    }

    companion object {
        fun start(): FakeDownstream {
            val executor = Executors.newCachedThreadPool()
            val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), LOOPBACK_PORT), 0)
            server.executor = executor
            server.createContext("/") { exchange -> handle(exchange) }
            server.start()
            return FakeDownstream(server, executor)
        }

        private fun handle(exchange: HttpExchange) {
            val path = exchange.requestURI.path
            val reply = route(path)
            val bytes = reply.body.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(reply.status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }

        private fun route(path: String): Reply {
            USER_ORDERS_PATH.matchEntire(path)?.let { return ordersReply(it.groupValues[1]) }
            USER_PATH.matchEntire(path)?.let { return userReply(it.groupValues[1]) }
            PRODUCT_PATH.matchEntire(path)?.let { return productReply(it.groupValues[1]) }
            return Reply(HTTP_NOT_FOUND, ERROR_JSON.format("no route"))
        }

        private fun userReply(id: String): Reply {
            sleepIfSlow(id)
            return when (id) {
                UNKNOWN_ID -> Reply(HTTP_NOT_FOUND, ERROR_JSON.format("user not found"))
                BROKEN_ID -> Reply(HTTP_INTERNAL_ERROR, ERROR_JSON.format("injected failure"))
                else -> Reply(HTTP_OK, USER_JSON.format(id, id))
            }
        }

        private fun ordersReply(id: String): Reply {
            sleepIfSlow(id)
            return when (id) {
                UNKNOWN_ID -> Reply(HTTP_NOT_FOUND, ERROR_JSON.format("user not found"))
                else -> Reply(HTTP_OK, ORDERS_JSON)
            }
        }

        private fun productReply(id: String): Reply = Reply(HTTP_OK, PRODUCT_JSON.format(id, id))

        private fun sleepIfSlow(id: String) {
            if (id == SLOW_ID) {
                Thread.sleep(SLOW_DELAY_MS)
            }
        }

        private data class Reply(val status: Int, val body: String)
    }
}
