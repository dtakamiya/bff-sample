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

/**
 * ユーザー情報と注文一覧を返す下流 API。
 *
 * - `GET /users/{id}`: 名前は ID から組み立て、tier は固定値（[USER_TIER]）を返す。
 * - `GET /users/{id}/orders`: ID の内容は見ず、固定の注文一覧（[STUB_ORDERS]）を返す。
 *
 * どちらも ID が `unknown` のときだけ 404 になる。BFF の user の 404 経路を再現するため。
 *
 * BFF は user と orders に同じ ID を渡す。user と orders が同時に 404 になる場合は user の 404 が
 * 優先されるので（README の仕様）、orders の 404 が BFF の応答として外に出ることはない。
 * 逐次版は user が先に失敗すると orders を呼ばない。
 */
@RestController
@RequestMapping("/users")
class UserController(
    private val stubProperties: StubProperties,
) {
    /**
     * 遅延・障害注入を適用してからユーザーを返す。
     *
     * 処理順序と優先順位は [applyStubBehavior] の KDoc を参照。
     * 遅延の既定値はユーザー用の `stub.delay-ms.users`。
     */
    @GetMapping("/{id}")
    fun getUser(
        @PathVariable("id") id: String,
        // nullable にしているのは「未指定」と「0 ms 指定」を区別するため（ProductController と同じ理由）。
        @RequestParam("delayMs", required = false) delayMs: Long?,
        @RequestParam("fail", defaultValue = "false") fail: Boolean,
    ): User {
        applyStubBehavior(id, fail, delayMs, stubProperties.delayMs.users)
        return User(id = id, name = "User $id", tier = USER_TIER)
    }

    /**
     * 遅延・障害注入を適用してから注文一覧を返す。
     *
     * 遅延の既定値は注文用の `stub.delay-ms.orders`。ユーザーの取得とは別に設定できるので、
     * 2 つの呼び出しの遅延を個別に変えられる。
     * ID は 404 判定にだけ使い、返す内容は変えない。
     */
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
