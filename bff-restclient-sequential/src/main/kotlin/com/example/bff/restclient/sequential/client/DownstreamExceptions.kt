package com.example.bff.restclient.sequential.client

/**
 * 下流が指定ユーザーを 404 で返したことを表す業務例外。
 *
 * ダッシュボードで 404 にマップされる唯一の例外。
 * [cause] には元の `HttpClientErrorException.NotFound` を残し、調査時に下流の応答を追えるようにする。
 */
class UserNotFoundException(
    val userId: String,
    cause: Throwable? = null,
) : RuntimeException("user not found: $userId", cause)

/**
 * 下流呼び出しの失敗全般を表す例外。接続失敗・タイムアウト・5xx・想定外の 4xx・空ボディが対象。
 *
 * 呼び出し元（集約・ハンドラ）はこの型の違いを見ず、[com.example.bff.restclient.sequential.api.GlobalExceptionHandler] で 502 にする。
 * 失敗の種類は [message] と cause で区別する。
 */
class DownstreamException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
