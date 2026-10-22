package com.example.bff.webflux.infrastructure.client

import com.example.bff.webflux.domain.exception.DomainException
import com.example.bff.webflux.domain.exception.UserNotFoundException
import com.example.bff.webflux.domain.exception.toDomainFailure
import com.example.bff.webflux.domain.model.UserId
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClientResponseException

/*
 * ACL のうち、下流の例外をドメイン例外へ変換する部分（infrastructure 層）。
 *
 * 変換の規則はポートごとに異なるため、共通化せず関数を分けている。
 *  - ユーザー取得（toUserFailure）: 下流 404 → UserNotFoundException。それ以外（5xx・タイムアウト・変換失敗）→ DownstreamFailureException。
 *  - 注文・商品取得（toDomainFailure）: 404 を含む全エラー → DownstreamFailureException（404 を 404 として返さない）。
 */

/**
 * ユーザー取得の失敗をドメイン例外に変換する。下流の 404 だけは「ユーザーが存在しない」として扱う。
 * それ以外は toDomainFailure（DownstreamFailureException。既にドメイン例外なら二重に包まない）。
 */
internal fun Throwable.toUserFailure(userId: UserId): DomainException =
    if (isDownstreamNotFound()) UserNotFoundException(userId.value) else toDomainFailure()

/** 下流の HTTP 404 かどうか。タイムアウトや接続失敗など、HTTP 応答が無いエラーは false。 */
private fun Throwable.isDownstreamNotFound(): Boolean =
    this is WebClientResponseException && statusCode.value() == HttpStatus.NOT_FOUND.value()
