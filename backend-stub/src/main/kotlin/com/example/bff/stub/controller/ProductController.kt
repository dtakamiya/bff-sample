package com.example.bff.stub.controller

import com.example.bff.stub.config.StubProperties
import com.example.bff.stub.model.Product
import com.example.bff.stub.support.PRODUCT_PRICE
import com.example.bff.stub.support.applyStubBehavior
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 商品情報を返す下流 API（`GET /products/{id}`）。
 *
 * BFF は注文に含まれる productId でこの API を呼んで商品情報を取得する。
 * 商品は実在チェックをせず、`unknown` 以外の ID はすべて 200 を返す。
 *
 * 商品の 404 は BFF では 502（下流障害）に変換される。ただし、ダッシュボード経由では
 * 注文の productId が p1〜p3 に限られるため、商品の 404 には到達しない。
 * 見るには `GET /products/unknown` を直接叩く。
 */
@RestController
@RequestMapping("/products")
class ProductController(
    private val stubProperties: StubProperties,
) {
    /**
     * 遅延・障害注入を適用してから商品を返す。
     *
     * 処理順序と優先順位は [applyStubBehavior] の KDoc を参照。
     * 商品名は `Product $id`、価格は [PRODUCT_PRICE] 固定。
     */
    @GetMapping("/{id}")
    fun getProduct(
        @PathVariable("id") id: String,
        // nullable にしているのは「未指定」と「0 ms 指定」を区別するため。
        // 未指定なら設定値（stub.delay-ms.products）が使われる。
        @RequestParam("delayMs", required = false) delayMs: Long?,
        // Boolean に変換できない値（例: fail=abc）は Spring が 400 を返し、この処理には入らない。
        @RequestParam("fail", defaultValue = "false") fail: Boolean,
    ): Product {
        // 遅延・障害・404 判定をここで一括して適用する。例外が出なければ正常応答を組み立てる。
        applyStubBehavior(id, fail, delayMs, stubProperties.delayMs.products)
        return Product(id = id, name = "Product $id", price = PRODUCT_PRICE)
    }
}
