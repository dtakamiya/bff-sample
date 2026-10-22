package com.example.bff.restclient.domain.exception

/**
 * ドメインが表明する失敗の基底（domain 層）。HTTP ステータスや下流のライブラリの型は知らない（変換は presentation が行う）。
 * sealed なので、ドメイン例外の集合はこのファイルで閉じている。Spring・Jackson には依存しない。
 */
sealed class DomainException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * ユーザーが存在しないことを表す。presentation で HTTP 404 に変換される。
 *
 * [userId] は生の文字列で持つ。空白のみの ID は値オブジェクト（UserId）を作れないため、
 * 「値オブジェクトにできなかった ID」もこの例外で表せるようにしている。
 * 下流の 404 をこの例外に変換するのは infrastructure の ACL（DownstreamErrorTranslator）の役目。
 */
class UserNotFoundException(
    val userId: String,
    cause: Throwable? = null,
) : DomainException("user not found: $userId", cause)

/**
 * 下流呼び出しの失敗（接続失敗・タイムアウト・4xx/5xx・変換失敗・空ボディ）を 1 種類にまとめた例外。
 * 上位（presentation）では 502（Bad Gateway）になる。message には呼び出し内容を入れ、ログで追えるようにする。
 *
 * 商品の 404 もここに集約される（商品の不在を 404 として返さない）。
 */
class DownstreamException(
    message: String,
    cause: Throwable? = null,
) : DomainException(message, cause)
