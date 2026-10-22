package com.example.bff.stub.support

import com.example.bff.stub.model.Order

/**
 * 全ユーザー共通の tier。`GET /users/{id}` の応答にだけ使う。
 *
 * ユーザーごとの差は持たせていない。ID に関係なく同じ値を返す。
 */
const val USER_TIER = "gold"

/**
 * 全商品共通の価格（整数、単位は定義していない）。
 *
 * 価格は商品 ID に関係なく同じ値を返す。README の `totalAmount` の例（9600）は、
 * この値と [STUB_ORDERS] の数量から計算した結果になっている。
 */
const val PRODUCT_PRICE = 1200

/**
 * 全ユーザー共通の注文一覧（固定データ）。`GET /users/{id}/orders` の応答に使う。
 *
 * 注文の内容はユーザー ID を見ずに返すので、どのユーザーも同じ 3 件を受け取る。
 * 各注文の `productId`（p1〜p3）は `GET /products/{id}` でそのまま取得できる。
 * 3 件とも商品 ID が異なるため、同じ商品を複数回取得するケースは含まれない。
 */
val STUB_ORDERS: List<Order> = listOf(
    // 数量は 2 / 1 / 5 と 3 件で異なる。合計金額の結果から、数量が正しく掛けられたかを判別できる。
    Order(orderId = "o1", productId = "p1", quantity = 2),
    Order(orderId = "o2", productId = "p2", quantity = 1),
    Order(orderId = "o3", productId = "p3", quantity = 5),
)
