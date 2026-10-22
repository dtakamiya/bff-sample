package com.example.bff.restclient.infrastructure.client

import com.example.bff.restclient.domain.exception.DownstreamException
import com.example.bff.restclient.domain.exception.UserNotFoundException
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClientException

/*
 * ACL のうち、下流の例外をドメイン例外へ変換する部分（infrastructure 層）。
 *
 * 変換の規則はポートごとに異なるため、関数を分けている。
 *  - ユーザー取得（userCall）: 下流 404 → UserNotFoundException。それ以外の失敗は downstreamCall の規則に従う。
 *  - 注文・商品取得（downstreamCall）: 404 を含む全エラー → DownstreamException（404 を 404 として返さない）。
 *
 * 順序に注意: HttpClientErrorException.NotFound は RestClientException でもあるため、
 * 404 の判定は必ず downstreamCall の内側（呼び出しの近く）で行う。外側で先に捕まえると、ユーザー不在が 502 に潰れる。
 */

/**
 * 下流呼び出しを実行し、失敗を [DownstreamException] に正規化する。
 *
 * - 戻り値が null（空ボディ）は異常とみなす。
 * - [RestClientException]（4xx/5xx・接続失敗・タイムアウト・JSON 変換失敗）を捕まえる。
 *   それ以外（既定の RuntimeException など）は捕まえない。広く捕まえると、意図した例外まで 502 に潰れてしまう。
 * - [call] の中で値オブジェクトを作る ACL 変換を行う場合は、その検証失敗（IllegalArgumentException）も
 *   下流の不正な応答とみなし、DownstreamException（502）にする。
 */
internal inline fun <T : Any> downstreamCall(description: String, call: () -> T?): T =
    try {
        call() ?: throw DownstreamException("empty response body from $description")
    } catch (e: RestClientException) {
        throw DownstreamException("downstream call failed: $description", e)
    } catch (e: IllegalArgumentException) {
        throw DownstreamException("invalid downstream response from $description", e)
    }

/**
 * ユーザー取得の下流呼び出し。下流の 404 だけは「ユーザーが存在しない」として [UserNotFoundException] にする。
 * 404 以外の失敗は [downstreamCall] の規則（DownstreamException）に従う。
 */
internal inline fun <T : Any> userCall(userId: String, description: String, call: () -> T?): T =
    downstreamCall(description) {
        try {
            call()
        } catch (e: HttpClientErrorException.NotFound) {
            throw UserNotFoundException(userId, e)
        }
    }
