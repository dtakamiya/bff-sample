package com.example.bff.webclient.domain.exception

import com.example.bff.webclient.domain.model.UserId

/**
 * ドメインが表明する失敗の基底。HTTP ステータスや下流のライブラリの型は知らない（変換は presentation が行う）。
 * sealed なので、ドメイン例外の集合はこのファイルで閉じている。Spring・Jackson・Reactor には依存しない。
 */
sealed class DomainException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * ユーザーが存在しないことを表す。presentation で HTTP 404 に変換される。
 * 下流の 404 をこの例外に変換するのは infrastructure の ACL の役目。
 * 引数は生の ID 文字列。UserId を作れない入力（空白のみ）もこの例外で 404 にできるようにするため。
 */
class UserNotFoundException(rawUserId: String) : DomainException("user not found: $rawUserId")

/**
 * 下流呼び出しの失敗（タイムアウト・5xx・接続失敗・変換失敗など）を 1 種類にまとめた例外。
 * 元の例外は cause に残し、presentation がログに出す。HTTP 502 に変換される。
 * 商品の 404 もここに集約される（商品の 404 を 404 として返さない）。
 */
class DownstreamFailureException(cause: Throwable) : DomainException("downstream call failed", cause)

/**
 * 任意の例外を DomainException に揃える。既にドメイン例外ならそのまま返し、二重に包まない。
 * それ以外は DownstreamFailureException に包む（ドメインの外へ生の下流例外を漏らさないため）。
 */
fun Throwable.toDomainFailure(): DomainException =
    this as? DomainException ?: DownstreamFailureException(this)
