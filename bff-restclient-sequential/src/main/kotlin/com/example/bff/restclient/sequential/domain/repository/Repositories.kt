package com.example.bff.restclient.sequential.domain.repository

import com.example.bff.restclient.sequential.domain.model.Order
import com.example.bff.restclient.sequential.domain.model.Product
import com.example.bff.restclient.sequential.domain.model.ProductId
import com.example.bff.restclient.sequential.domain.model.User
import com.example.bff.restclient.sequential.domain.model.UserId

/*
 * ドメインが必要とするデータ取得の能力（ポート）。実装（下流 API を呼ぶアダプタ）は infrastructure が担う。
 * ドメイン型のみを参照し、下流の DTO や RestClient には依存しない。
 *
 * 共通の契約:
 *  - 戻り値は同期的に返す（ブロッキング）。呼び出した時点で下流の応答を待つ。
 *  - 失敗は例外として投げる。型は DomainException のサブクラスに限る（HTTP の概念は持ち込まない）。
 */

/** ユーザーを取得するポート。存在しなければ UserNotFoundException、取得に失敗すれば DownstreamException。 */
interface UserRepository {
    fun findById(id: UserId): User
}

/** ユーザーの注文を取得するポート。注文が 0 件のときは空リストを返す。失敗は DownstreamException。 */
interface OrderRepository {
    fun findByUserId(userId: UserId): List<Order>
}

/** 商品を 1 件取得するポート。失敗は（404 を含め）DownstreamException に集約する。 */
interface ProductRepository {
    fun findById(id: ProductId): Product
}
