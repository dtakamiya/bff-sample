package com.example.bff.stub.controller

import com.example.bff.stub.config.StubProperties
import com.example.bff.stub.model.Order
import com.example.bff.stub.model.User
import com.example.bff.stub.support.STUB_ORDERS
import com.example.bff.stub.support.USER_TIER
import com.example.bff.stub.support.applyStubBehavior
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class UserController(
    private val stubProperties: StubProperties,
) {
    @GetMapping("/{id}")
    fun getUser(
        @PathVariable("id") id: String,
        @RequestParam("delayMs", required = false) delayMs: Long?,
        @RequestParam("fail", defaultValue = "false") fail: Boolean,
    ): User {
        applyStubBehavior(id, fail, delayMs, stubProperties.delayMs.users)
        return User(id = id, name = "User $id", tier = USER_TIER)
    }

    @GetMapping("/{id}/orders")
    fun getOrders(
        @PathVariable("id") id: String,
        @RequestParam("delayMs", required = false) delayMs: Long?,
        @RequestParam("fail", defaultValue = "false") fail: Boolean,
    ): List<Order> {
        applyStubBehavior(id, fail, delayMs, stubProperties.delayMs.orders)
        return STUB_ORDERS
    }
}
