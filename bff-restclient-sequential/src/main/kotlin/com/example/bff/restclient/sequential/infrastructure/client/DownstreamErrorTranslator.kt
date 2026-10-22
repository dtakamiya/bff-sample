package com.example.bff.restclient.sequential.infrastructure.client

import com.example.bff.restclient.sequential.domain.exception.DomainException
import com.example.bff.restclient.sequential.domain.exception.DownstreamException
import com.example.bff.restclient.sequential.domain.exception.UserNotFoundException
import org.springframework.web.client.HttpClientErrorException

/*
 * ACL のうち、下流の例外をドメイン例外へ変換する部分（infrastructure 層）。
 *
 * 規則はポートごとに異なる。
 *  - ユーザー取得: 下流 404 → UserNotFoundException。それ以外の失敗 → DownstreamException。
 *  - 注文・商品取得: 404 を含む全エラー → DownstreamException（404 を 404 として返さない）。
 */

/**
 * ユーザー取得の失敗をドメイン例外に変換する。下流の 404 だけは「ユーザーが存在しない」として扱う。
 * それ以外は [toDomainFailure]（既にドメイン例外なら二重に包まない）。
 */
internal fun Throwable.toUserFailure(userId: String, operation: String): DomainException =
    if (this is HttpClientErrorException.NotFound) {
        UserNotFoundException(userId, this)
    } else {
        toDomainFailure(operation)
    }

/**
 * 任意の例外を DomainException に揃える。既にドメイン例外ならそのまま返し、二重に包まない。
 * それ以外は DownstreamException に包む（下流の生の例外をドメインの外へ漏らさないため）。
 * [operation] は「どの呼び出しが失敗したか」をログに残すための説明文。
 */
internal fun Throwable.toDomainFailure(operation: String): DomainException =
    this as? DomainException ?: DownstreamException("downstream call failed: $operation", this)

/**
 * 下流呼び出しを行い、例外をユーザー取得用の規則でドメイン例外に変換する。
 * 呼び出し結果は [call] の戻り値のまま返す（ここでは値を変えない）。
 */
internal inline fun <T> translatingUserErrors(userId: String, operation: String, call: () -> T): T =
    try {
        call()
    } catch (e: Exception) {
        throw e.toUserFailure(userId, operation)
    }

/** 下流呼び出しを行い、例外を共通の規則（[toDomainFailure]）でドメイン例外に変換する。 */
internal inline fun <T> translatingErrors(operation: String, call: () -> T): T =
    try {
        call()
    } catch (e: Exception) {
        throw e.toDomainFailure(operation)
    }

/**
 * 下流の本文が null（空レスポンス）なら DownstreamException にする。
 * 空レスポンスは下流の不具合として扱い、502 になる。
 */
internal fun <T : Any> T?.requireBody(operation: String): T =
    this ?: throw DownstreamException("empty response body from $operation")
