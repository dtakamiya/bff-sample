package com.example.bff.webclient.service

/**
 * ユーザーが下流で 404（存在しない）と判定されたことを表す例外。
 * BffExceptionHandler が HTTP 404 に変換する。
 */
class UserNotFoundException(userId: String) : RuntimeException("user not found: $userId")

/**
 * 下流呼び出しの失敗（タイムアウト・5xx・接続失敗・変換失敗など）を 1 種類にまとめた例外。
 * 元の例外は cause に残し、BffExceptionHandler がログに出す。HTTP 502 に変換される。
 * 商品の 404 もここに集約される（商品 404 を 404 として返さない）。
 */
class DownstreamFailureException(cause: Throwable) : RuntimeException("downstream call failed", cause)
