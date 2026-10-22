package com.example.bff.webclient.infrastructure.client

import com.example.bff.webclient.domain.exception.DomainException
import com.example.bff.webclient.domain.exception.DownstreamFailureException
import com.example.bff.webclient.domain.exception.UserNotFoundException
import com.example.bff.webclient.domain.exception.toDomainFailure
import com.example.bff.webclient.domain.model.UserId
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

/*
 * ACL のうち、下流の例外をドメイン例外へ変換する部分（infrastructure 層）。
 *
 * 変換の規則はポートごとに異なるため、共通化せず関数を分けている。
 *  - ユーザー取得（toUserFailure）: 下流 404 → UserNotFoundException。それ以外（5xx・タイムアウト・変換失敗）→ DownstreamFailureException。
 *  - 注文・商品取得（toDomainFailure）: 404 を含む全エラー → DownstreamFailureException（404 を 404 として返さない）。
 *  - 本文なしの応答（failIfEmptyBody）: 全ポート共通で DownstreamFailureException（502）。
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

/**
 * 下流が本文なしで応答し、Mono が値なしで完了した場合を DownstreamFailureException（502）にする。
 * 空の Mono は onErrorMap を通らないため、アダプタの変換チェーンの末尾で呼ぶ。
 * 本文なしの 2xx は下流の契約違反なので、利用者からは他の下流失敗と同じ 502 として扱う。
 */
internal fun <T : Any> Mono<T>.failIfEmptyBody(): Mono<T> =
    switchIfEmpty(Mono.defer { Mono.error(DownstreamFailureException(IllegalStateException("empty body"))) })
