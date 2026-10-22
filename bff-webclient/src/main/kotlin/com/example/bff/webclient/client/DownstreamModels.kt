package com.example.bff.webclient.client

/**
 * 下流 API の応答をそのまま受けるための DTO 群。
 *
 * 画面へ返す形（model パッケージ）とは意図的に分けている。下流の仕様変更が画面の契約へ直接漏れないようにするため。
 * 項目はすべて non-null。下流が必須項目を欠いて返すと JSON 変換が失敗し、呼び出し側では下流失敗（502）として扱われる。
 */

/** 下流のユーザー情報。tier は会員ランクの文字列。 */
data class UserResponse(
    val id: String,
    val name: String,
    val tier: String,
)

/** 下流の注文 1 件。商品の詳細は含まず、productId で商品を別途引き当てる。 */
data class OrderResponse(
    val orderId: String,
    val productId: String,
    val quantity: Int,
)

/** 下流の商品詳細。price の単位は下流の仕様に従う（この BFF では変換しない）。 */
data class ProductResponse(
    val id: String,
    val name: String,
    val price: Int,
)
