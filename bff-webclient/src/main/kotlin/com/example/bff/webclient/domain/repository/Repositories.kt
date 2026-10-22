package com.example.bff.webclient.domain.repository

import com.example.bff.webclient.domain.model.Order
import com.example.bff.webclient.domain.model.Product
import com.example.bff.webclient.domain.model.ProductId
import com.example.bff.webclient.domain.model.User
import com.example.bff.webclient.domain.model.UserId
import reactor.core.publisher.Mono

/*
 * ドメインが必要とするデータ取得の能力（ポート）。実装（下流 API を呼ぶアダプタ）は infrastructure が担う。
 * ドメイン型のみを参照し、下流の DTO や WebClient には依存しない。
 *
 * 共通の契約:
 *  - 戻り値は Mono（リアクティブ）。購読されるまで取得は始まらない。
 *  - 失敗はエラーシグナルとして流す。型は DomainException のサブクラスに限る（HTTP の概念は持ち込まない）。
 */

/** ユーザーを取得するポート。存在しなければ UserNotFoundException、取得に失敗すれば DownstreamFailureException。 */
interface UserRepository {
    fun findById(id: UserId): Mono<User>
}

/** ユーザーの注文を取得するポート。注文が 0 件のときは空リストを返す。失敗は DownstreamFailureException。 */
interface OrderRepository {
    fun findByUserId(userId: UserId): Mono<List<Order>>
}

/** 商品を 1 件取得するポート。失敗は（404 を含め）DownstreamFailureException に集約する。 */
interface ProductRepository {
    fun findById(id: ProductId): Mono<Product>
}
