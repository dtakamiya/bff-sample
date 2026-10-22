package com.example.bff.webflux.service

/**
 * 利用者が存在しない（下流の user が 404）ことを表す。BffExceptionHandler で 404 に変換される。
 * メッセージにはクライアントに返す文言を入れる。
 */
class UserNotFoundException(userId: String) : RuntimeException("user not found: $userId")

/**
 * 下流呼び出しの失敗（5xx・タイムアウト・接続失敗など）をまとめて表す。
 * 原因は cause に残し、ログで使う。クライアントには汎用メッセージの 502 だけを返す。
 */
class DownstreamFailureException(cause: Throwable) : RuntimeException("downstream call failed", cause)
