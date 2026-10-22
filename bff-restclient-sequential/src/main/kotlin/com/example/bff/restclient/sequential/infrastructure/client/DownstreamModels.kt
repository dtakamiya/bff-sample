package com.example.bff.restclient.sequential.infrastructure.client

/**
 * 下流 API の応答をそのまま受けるための DTO 群（infrastructure 層）。
 *
 * ドメイン型（domain.model）とも、画面向けの型（presentation.dto）とも意図的に別にしている。
 * 下流の JSON 仕様が変わっても、ドメインや画面の契約へ波及しないようにするため。
 * 変換は DownstreamMapper（ACL）が行う。項目はすべて non-null。下流が必須項目を欠いて返すと JSON 変換が失敗し、
 * 下流失敗（502）として扱われる。
 */

/** 下流 user の応答モデル。 */
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

/** 下流 product の応答モデル。price の単位は下流の仕様に従う（この BFF では変換しない）。 */
data class DownstreamProduct(
    val id: String,
    val name: String,
    val price: Int,
)
