package com.example.bff.restclient.sequential.infrastructure.client

import com.example.bff.restclient.sequential.domain.model.Order
import com.example.bff.restclient.sequential.domain.model.OrderId
import com.example.bff.restclient.sequential.domain.model.Product
import com.example.bff.restclient.sequential.domain.model.ProductId
import com.example.bff.restclient.sequential.domain.model.User
import com.example.bff.restclient.sequential.domain.model.UserId

/*
 * ACL（腐敗防止層）のうち、下流 DTO からドメイン型への変換（infrastructure 層）。
 * 下流の形（ID が String、項目名など）はここで止め、domain には持ち込まない。
 * 値オブジェクトの検証（空白の ID など）に失敗すると例外になる。その例外はアダプタが DownstreamException へ変換する。
 */

internal fun DownstreamUser.toDomain(): User =
    User(id = UserId(id), name = name, tier = tier)

internal fun DownstreamOrder.toDomain(): Order =
    Order(id = OrderId(orderId), productId = ProductId(productId), quantity = quantity)

internal fun DownstreamProduct.toDomain(): Product =
    Product(id = ProductId(id), name = name, price = price)
