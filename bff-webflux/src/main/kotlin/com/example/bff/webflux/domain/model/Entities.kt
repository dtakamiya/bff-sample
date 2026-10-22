package com.example.bff.webflux.domain.model

/**
 * ドメインのエンティティ群（ユーザー・注文・商品）。下流の DTO とは別の型で、infrastructure の ACL 変換を経て作られる。
 * 各エンティティは識別子（値オブジェクト）を持つ。値の意味づけはこの BFF では行わず、下流の値をそのまま採用する。
 */

/** ユーザー。tier は会員ランクの文字列のまま扱う。 */
data class User(
    val id: UserId,
    val name: String,
    val tier: String,
)

/**
 * 注文。商品の詳細は持たず、productId で商品を別途引き当てる。
 * quantity の正負や上限は検査しない。検査を足すと、現在受理している下流の応答が 502 に変わるため。
 */
data class Order(
    val id: OrderId,
    val productId: ProductId,
    val quantity: Int,
)

/** 商品。price の単位は下流の仕様に従う（この BFF では変換しない）。 */
data class Product(
    val id: ProductId,
    val name: String,
    val price: Int,
)
