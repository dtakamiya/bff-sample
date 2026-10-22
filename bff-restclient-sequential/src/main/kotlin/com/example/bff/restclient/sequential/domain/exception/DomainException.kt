package com.example.bff.restclient.sequential.domain.exception

/**
 * ドメインが表明する失敗の基底。HTTP ステータスや下流のライブラリの型は知らない（変換は presentation が行う）。
 * sealed なので、ドメイン例外の集合はこのファイルで閉じている。Spring・Jackson には依存しない。
 */
sealed class DomainException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * ユーザーが存在しないことを表す。presentation で HTTP 404 に変換される。
 *
 * [userId] は生の文字列で持つ。空白のみの ID は値オブジェクトを作れないため、生の値のまま 404 の文言に使う。
 * [cause] には元の下流 404 を残す（調査用）。
 */
class UserNotFoundException(
    val userId: String,
    cause: Throwable? = null,
) : DomainException("user not found: $userId", cause)

/**
 * 下流呼び出しの失敗全般を表す例外。接続失敗・タイムアウト・5xx・想定外の 4xx・空ボディ・変換失敗が対象。
 * 元の例外は cause に残し、presentation がログに出す。HTTP 502 に変換される。
 * 商品の 404 もここに集約される（商品の 404 を 404 として返さない）。
 */
class DownstreamException(
    message: String,
    cause: Throwable? = null,
) : DomainException(message, cause)
