package com.example.bff.webflux.infrastructure.client

import com.example.bff.webflux.domain.model.Order
import com.example.bff.webflux.domain.model.OrderId
import com.example.bff.webflux.domain.model.Product
import com.example.bff.webflux.domain.model.ProductId
import com.example.bff.webflux.domain.model.User
import com.example.bff.webflux.domain.model.UserId

/*
 * ACL（腐敗防止層）のうち、下流 DTO からドメイン型への変換（infrastructure 層）。
 * 下流の形（ID が String、項目名など）はここで止め、domain には持ち込まない。
 * 値オブジェクトの検証（空白の ID など）に失敗すると例外になる。その例外はアダプタが DownstreamFailureException へ変換する。
 */

internal fun UserResponse.toDomain(): User =
    User(id = UserId(id), name = name, tier = tier)

internal fun OrderResponse.toDomain(): Order =
    Order(id = OrderId(orderId), productId = ProductId(productId), quantity = quantity)

internal fun ProductResponse.toDomain(): Product =
    Product(id = ProductId(id), name = name, price = price)
