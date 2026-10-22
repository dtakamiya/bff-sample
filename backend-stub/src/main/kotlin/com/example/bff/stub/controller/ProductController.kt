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

@RestController
@RequestMapping("/products")
class ProductController(
    private val stubProperties: StubProperties,
) {
    @GetMapping("/{id}")
    fun getProduct(
        @PathVariable("id") id: String,
        @RequestParam("delayMs", required = false) delayMs: Long?,
        @RequestParam("fail", defaultValue = "false") fail: Boolean,
    ): Product {
        applyStubBehavior(id, fail, delayMs, stubProperties.delayMs.products)
        return Product(id = id, name = "Product $id", price = PRODUCT_PRICE)
    }
}
