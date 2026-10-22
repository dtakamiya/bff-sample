package com.example.bff.stub.support

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/**
 * クエリ `delayMs` も設定値も無いときに使う遅延（ミリ秒）。
 *
 * 通常は application.yml の `stub.delay-ms.*` が優先される。このコード既定値が効くのは、
 * [com.example.bff.stub.config.StubProperties] の初期値として使われる場合だけ（設定ファイルに値が無いとき）。
 */
const val DEFAULT_DELAY_MS = 200L

/**
 * 「存在しない」を表す特別な ID。どのエンドポイントでもこの ID を指定すると 404 になる。
 *
 * 任意の ID を 404 にする仕組みは持たず、ID を固定文字列の 1 つだけにしている。
 * テストや BFF の 404 経路を確実に再現するための、分かりやすいトリガーとして機能する。
 */
const val UNKNOWN_ID = "unknown"

/**
 * 全エンドポイント共通の遅延・障害注入を適用する。下流の振る舞いはここに集約している。
 *
 * 処理順序は次のとおり（順序は仕様の一部）。
 * 1. 遅延: [resolveDelayMs] で決まった時間だけ待つ（[Thread.sleep]）。
 * 2. 障害注入: `fail=true` なら 500。
 * 3. 存在しない ID: [UNKNOWN_ID] なら 404。
 *
 * 遅延を最初に置いているので、500 や 404 の応答にも遅延が乗る。
 * 失敗応答にも時間がかかる、という下流の挙動を模すための配置と読めるが、コードに明記はない。
 * 障害注入を 404 判定より先に評価するため、`/users/unknown?fail=true` は 404 ではなく 500 を返す。
 *
 * 例外として、負の `delayMs` は遅延の計算（引数評価）の段階で 400 になる。
 * その場合は sleep もせず、障害注入も 404 判定も行われない。
 *
 * @param id パスの ID。[UNKNOWN_ID] のときだけ 404 になる。
 * @param fail true なら遅延後に 500 を投げる。
 * @param delayMsOverride クエリの `delayMs`。null（未指定）なら [configuredDelayMs] を使う。
 * @param configuredDelayMs 設定値（`stub.delay-ms.*`）。クエリ指定が無いときの遅延。
 * @throws ResponseStatusException 負の delayMs は 400、fail は 500、存在しない ID は 404。
 */
fun applyStubBehavior(
    id: String,
    fail: Boolean,
    delayMsOverride: Long?,
    configuredDelayMs: Long,
) {
    // 遅延は同期的に Thread.sleep で待つ。サーブレットのリクエストスレッドを実際に占有する。
    // 注意: スタブの同時処理数は server.tomcat.threads.max で頭打ちになる（application.yml では仮想スレッドを有効にしていない）。
    // 負荷計測で同時実行数がこの上限を超えると、スタブ側で待ち行列が生じて計測結果が歪むおそれがある。
    Thread.sleep(resolveDelayMs(delayMsOverride, configuredDelayMs))
    // 障害注入は ID 判定より前に評価する（unknown かつ fail=true なら 500 になる）。
    if (fail) {
        throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "injected failure")
    }
    // 404 は ID が UNKNOWN_ID のときだけ。BFF では user の 404 はそのまま 404 として返り、
    // product の 404 は 502 に変換される（README の仕様）。
    if (id == UNKNOWN_ID) {
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "resource not found: $id")
    }
}

/**
 * 今回の遅延ミリ秒を決める。優先順位は「クエリ `delayMs` > 設定値」。
 *
 * クエリの値が null（未指定）のときだけ設定値を使う。
 * 0 を明示すると設定値より優先されて遅延なしになる。null と 0 を区別するのは、
 * 「指定しなかった」と「0ms と指定した」を別の意味として扱うため。
 *
 * @throws ResponseStatusException 負の値は 400。
 */
private fun resolveDelayMs(delayMsOverride: Long?, configuredDelayMs: Long): Long {
    if (delayMsOverride == null) {
        return configuredDelayMs
    }
    // 負の値を Thread.sleep に渡すと例外になり、500 になってしまう。
    // そうならないよう、sleep の前に 400 で弾く。
    if (delayMsOverride < 0) {
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "delayMs must be >= 0")
    }
    return delayMsOverride
}
