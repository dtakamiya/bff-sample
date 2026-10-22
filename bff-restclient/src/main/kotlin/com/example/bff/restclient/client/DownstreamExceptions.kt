package com.example.bff.restclient.client

/**
 * 下流でユーザーが存在しない（user API が 404 を返した）ことを表す例外。
 *
 * 他の下流障害と区別するための専用型で、[com.example.bff.restclient.api.GlobalExceptionHandler] が 404 に変換する。
 * [cause] には元の 404 例外を残し、ログで原因をたどれるようにしている。
 */
class UserNotFoundException(
    val userId: String,
    cause: Throwable? = null,
) : RuntimeException("user not found: $userId", cause)

/**
 * 下流呼び出しの失敗を表す汎用例外。
 *
 * RestClient の例外（接続失敗・タイムアウト・4xx/5xx・変換失敗）と空ボディを、すべてこの型に寄せる。
 * 上位では 502（Bad Gateway）として扱われる。
 */
class DownstreamException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
