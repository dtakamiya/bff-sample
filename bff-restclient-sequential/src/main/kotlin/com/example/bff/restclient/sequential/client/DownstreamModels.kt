package com.example.bff.restclient.sequential.client

/**
 * 下流 user の応答モデル。
 *
 * API 側の [com.example.bff.restclient.sequential.api.UserSummary] とは意図的に別の型にしている。
 * 下流の JSON 仕様が変わっても、画面向けの形（API 側）へ波及しないようにするため。
 */
data class DownstreamUser(
    val id: String,
    val name: String,
    val tier: String,
)

/**
 * 下流 orders の 1 件。
 *
 * [productId] だけを持ち、商品の名前・価格は含まない。
 * 商品情報は別途 [DownstreamClient.fetchProduct] で取る必要があり、
 * これが逐次版で注文数ぶんの追加呼び出しが発生する原因になっている。
 */
data class DownstreamOrder(
    val orderId: String,
    val productId: String,
    val quantity: Int,
)

/** 下流 product の応答モデル。 */
data class DownstreamProduct(
    val id: String,
    val name: String,
    val price: Int,
)
