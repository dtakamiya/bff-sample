# bff-sample

同じ API を異なる呼び出し方式で実装した 4 つの BFF（Backend For Frontend）を比較するためのサンプルです。

- `bff-webclient`: MVC + WebClient + Mono で並列化（コントローラで `block()` して同期的に返す）
- `bff-webflux`: WebFlux + WebClient + Mono で並列化（`block()` を使わず、Mono のまま返す）
- `bff-restclient`: RestClient + 固定スレッドプール + CompletableFuture で並列化
- `bff-restclient-sequential`: RestClient で逐次呼び出し（スレッドを使わないケース）

4 つとも同じ下流 API（backend-stub）を呼び、同じレスポンスとエラー仕様を返します。4 モジュールとも DDD + オニオンアーキテクチャで、`presentation` / `application` / `domain` / `infrastructure` の層に分けています。負荷計測用の k6 スクリプトと実行スクリプトも含みます。

## 構成

```
                       +----------------------------------+
  client (curl / k6) ─▶| bff-webclient  :8081             |──┐  GET /users/{id}
                       | MVC + WebClient (Mono.zip)       |  │  GET /users/{id}/orders
                       +----------------------------------+  │  GET /products/{id}
                       +----------------------------------+  │
  client (curl / k6) ─▶| bff-webflux    :8084             |──┤
                       | WebFlux + WebClient (Mono.zip)   |  │
                       +----------------------------------+  │
                       +----------------------------------+  │
  client (curl / k6) ─▶| bff-restclient :8082             |──┤
                       | MVC + RestClient (CF + pool)     |  │
                       +----------------------------------+  │
                       +----------------------------------+  │
  client (curl / k6) ─▶| bff-restclient-sequential :8083  |──┤
                       | MVC + RestClient (serial)        |  │
                       +----------------------------------+  │
                                                             ▼
                                                  +--------------------+
                                                  | backend-stub :9090 |
                                                  +--------------------+
```

| モジュール | ポート | 役割 |
|---|---|---|
| `backend-stub` | 9090 | 下流 API のスタブ。`?delayMs=` で遅延、`?fail=true` で 500 を注入可能 |
| `bff-webclient` | 8081 | BFF（WebClient 版、並列） |
| `bff-restclient` | 8082 | BFF（RestClient 版、固定スレッドプールで並列） |
| `bff-restclient-sequential` | 8083 | BFF（RestClient 版、逐次。追加スレッドを使わない） |
| `bff-webflux` | 8084 | BFF（WebFlux 版、フルリアクティブ。`block()` を使わない） |

共通の API: `GET /api/dashboard/{userId}`

## 前提

- Java 17（ルートの `build.gradle.kts` で toolchain 17 を指定）
- Gradle wrapper（同梱。`gradle-9.6.1`）
- curl
- k6（負荷計測のみ必要）: `brew install k6`

## 起動方法

ビルド（5 モジュールの bootJar: backend-stub と 4 つの BFF）:

```sh
./gradlew :backend-stub:bootJar :bff-webclient:bootJar :bff-restclient:bootJar :bff-restclient-sequential:bootJar :bff-webflux:bootJar
```

起動（別々のターミナルで実行）:

```sh
java -jar backend-stub/build/libs/backend-stub.jar
java -jar bff-webclient/build/libs/bff-webclient.jar
java -jar bff-restclient/build/libs/bff-restclient.jar
java -jar bff-restclient-sequential/build/libs/bff-restclient-sequential.jar
java -jar bff-webflux/build/libs/bff-webflux.jar
```

### curl での確認

下流 API を直接叩く:

```sh
curl -s localhost:9090/users/u1
# {"id":"u1","name":"User u1","tier":"gold"}

curl -s localhost:9090/users/u1/orders
# [{"orderId":"o1","productId":"p1","quantity":2},{"orderId":"o2","productId":"p2","quantity":1},{"orderId":"o3","productId":"p3","quantity":5}]

# 遅延を指定（ミリ秒）
curl -s -o /dev/null -w '%{time_total}s\n' 'localhost:9090/users/u1?delayMs=500'
```

BFF を叩く（4 つの BFF で同一の応答）:

```sh
curl -s localhost:8081/api/dashboard/u1
# {"user":{"id":"u1","name":"User u1","tier":"gold"},"orders":[{"orderId":"o1","quantity":2,"product":{"id":"p1","name":"Product p1","price":1200}},{"orderId":"o2","quantity":1,"product":{"id":"p2","name":"Product p2","price":1200}},{"orderId":"o3","quantity":5,"product":{"id":"p3","name":"Product p3","price":1200}}],"totalAmount":9600}

# 8082（restclient）・8083（sequential）・8084（webflux）も同じ応答
curl -s localhost:8083/api/dashboard/u1

curl -i localhost:8082/api/dashboard/unknown
# HTTP/1.1 404
# {"error":"user not found: unknown"}
```

`totalAmount` は `quantity × price` の合計（この例は 2×1200 + 1×1200 + 5×1200 = 9600）。

## 負荷計測

```sh
scripts/run-compare.sh
```

実行内容:

1. k6 の存在確認（無ければ `brew install k6` を促して終了）
2. ポート 9090 / 8081 / 8082 / 8083 / 8084 が空いているか確認（使用中なら終了）
3. `:backend-stub:bootJar` と 4 つの BFF の bootJar をビルド
4. backend-stub を起動し、`/users/u1` への curl リトライで起動完了を待つ
5. BFF を 1 つずつ起動（webclient → restclient → restclient-sequential → webflux の順）し、`/api/dashboard/u1` の応答を待ってから k6 で計測。計測後に停止する
6. 終了時（正常終了・TERM など）に起動したプロセスを全て停止する

環境変数:

| 変数 | 既定値 | 内容 |
|---|---|---|
| `VUS` | `50` | 同時仮想ユーザー数 |
| `DURATION` | `30s` | 計測時間 |
| `SKIP_K6` | 未設定 | `1` にすると k6 を実行せず、ビルド・起動・疎通確認・後始末だけを行う |
| `K6_SUMMARY_TREND_STATS` | `avg,min,med,max,p(90),p(95),p(99)` | k6 のサマリに出す統計値（k6 が直接読む環境変数） |

結果: `build/load-results/bff-webclient.json`、`build/load-results/bff-restclient.json`、`build/load-results/bff-restclient-sequential.json`、`build/load-results/bff-webflux.json`（k6 の `--summary-export`）。起動ログは `build/load-logs/` に出力されます。

k6 スクリプトだけを単独で実行することもできます（4 つのうち任意の BFF を指定）:

```sh
BASE_URL=http://localhost:8083 VUS=50 DURATION=30s k6 run --summary-export out.json load/dashboard.js
```

`load/dashboard.js` は `GET /api/dashboard/u1` に対して status 200 と `totalAmount` の存在を check します。thresholds は設定していないため、p95 などはサマリで確認します。

## 実装比較

コードはすべて `src/main` 配下を読んで記載しています。表中のファイルパスは、`src/main/kotlin/` 配下の次のディレクトリからの相対パスです: `bff-webclient` は `com/example/bff/webclient/`、`bff-webflux` は `com/example/bff/webflux/`、`bff-restclient` は `com/example/bff/restclient/`、`bff-restclient-sequential` は `com/example/bff/restclient/sequential/`。

| 項目 | bff-webclient | bff-webflux | bff-restclient | bff-restclient-sequential |
|---|---|---|---|---|
| 集約ロジック | `application/usecase/GetDashboardUseCase.kt` | 同左 | 同左 | 同左 |
| 並列化の方式 | `Mono.zip` で user と orders を並列取得。その後 `Flux.fromIterable(...).flatMap` で商品を並列取得し `collectMap` | 同左 | `CompletableFuture.supplyAsync` で user と orders を並列取得。その後、注文ごとに `supplyAsync` で商品を並列取得 | なし。user → orders → 注文ごとの商品を順に呼ぶ |
| 並列実行先 | Reactor のイベントループ（WebClient の非同期 I/O）。`flatMap` の並列度はコードで指定していない | 同左 | `infrastructure/config/DownstreamConfig.kt` の `downstreamExecutor`（`Executors.newFixedThreadPool(downstream.parallelism)`、既定 32）。全リクエストで共有 | 使わない。追加のスレッドプールはない |
| リクエストスレッド | MVC のリクエストスレッドが `block()` で完了まで待機（`presentation/DashboardController.kt`） | 待たない。コントローラは `Mono<DashboardResponse>` を返し、`block()` を使わない（`presentation/DashboardController.kt`） | MVC のリクエストスレッドが `join()` で完了まで待機（`application/usecase/GetDashboardUseCase.kt` の `await`） | MVC のリクエストスレッドが各下流呼び出しを同期的に実行する。待機は呼び出しごとに発生する |
| 下流呼び出しのスレッド | Reactor Netty のスレッド（非ブロッキング） | 同左 | 固定プールのスレッドが同期的な `RestClient` 呼び出しでブロック | リクエストスレッド（同期的な `RestClient` 呼び出し） |
| 下流呼び出しの回数 | 2 + 商品の種類数（distinct） | 同左 | 2 + 注文数（商品は重複して呼ぶ） | 2 + 注文数（商品は重複して呼ぶ） |
| タイムアウトの設定箇所 | `infrastructure/client/DownstreamClient.kt` の各 `Mono` に `.timeout(timeout)`。値は `infrastructure/config/DownstreamProperties.kt`（`downstream.timeout-ms`） | 同左 | `infrastructure/config/DownstreamConfig.kt` の `JdkClientHttpRequestFactory` に connectTimeout と readTimeout を同じ値で設定（`downstream.timeout-ms`） | 同左 |
| user 失敗と orders 失敗が同時に起きた場合 | 両方の完了を待ってから user の結果を判定（`settle` で成否を値に包んでから `Mono.zip`）。user の 404 が優先 | 同左 | user を先に `await` する。user が失敗した時点で例外を投げ、orders の完了は待たない。user の 404 が優先 | user を先に呼び、失敗したら orders は呼ばない。user の 404 が優先 |
| user の 404 | `UserNotFoundException` → 404 | 同左 | `HttpClientErrorException.NotFound` を `UserNotFoundException` に変換 → 404 | 同左 → 404 |
| その他の下流失敗（5xx・タイムアウト・商品の 404 を含む） | `DownstreamFailureException` → 502 | 同左 | `RestClientException` を `DownstreamException` に変換 → 502 | `DomainException` 以外の `Exception` を `DownstreamException` に変換 → 502 |
| 下流が本文なしで応答（空ボディ） | `failIfEmptyBody()` で `DownstreamFailureException` → 502 | 同左 | 戻り値が null → `DownstreamException` → 502 | 同左（null → `DownstreamException` → 502） |
| エラーの変換箇所 | 例外の生成: `infrastructure/client/DownstreamErrorTranslator.kt`（`toUserFailure`）、`domain/exception/DomainException.kt`（`toDomainFailure`）。HTTP 変換: `presentation/BffExceptionHandler.kt` | 同左 | 例外変換: `infrastructure/client/DownstreamErrorTranslator.kt`。HTTP 変換: `presentation/GlobalExceptionHandler.kt` | 同左 |
| 404 / 502 のボディ | `{"error":"user not found: {id}"}` / `{"error":"downstream service unavailable"}` | 同じ | 同じ | 同じ |
| 依存スターター | `spring-boot-starter-webmvc`、`spring-boot-starter-webclient` | `spring-boot-starter-webflux`、`spring-boot-starter-webclient` | `spring-boot-starter-webmvc`、`spring-boot-starter-restclient` | 同左 |
| 実行時の HTTP クライアント | Reactor Netty（`reactor-netty-http` 1.3.7 が解決される） | Reactor Netty（webclient と同じ。バージョンは未確認） | JDK `HttpClient`（`JdkClientHttpRequestFactory` で設定） | 同左 |

### スレッドを使わないケース（逐次版）

`bff-restclient-sequential` は、`bff-restclient` と API・設定・エラー仕様が同一で、並列化だけを外したものです。

- 追加のスレッドを一切使わず、リクエストスレッド 1 本で user → orders → 注文ごとの商品を順に呼びます。
- 下流呼び出しは直列なので、BFF の応答時間は個々の呼び出しの時間の合計になります。並列版との差はこの部分に現れます。
- 並列版（webclient / webflux / restclient）との比較では、「並列化の効果」を測る対照（baseline）として扱います。

### コード量（`wc -l` の実測）

数え方: `find <module>/src/main -name '*.kt'` で `.kt` ファイルを集め、`wc -l` で数えた（`application.yml` は含めない）。表の値は「コメント込み / コメント・空行を除く」の順。後者は空行と、`//`・`/*`・`/**`・`*` で始まる行を除いた行数。

| 対象 | bff-webclient | bff-webflux | bff-restclient | bff-restclient-sequential |
|---|---|---|---|---|
| `.kt` ファイル数 | 19 | 19 | 19 | 19 |
| `.kt` 全体 | 779 / 424 | 781 / 423 | 783 / 413 | 746 / 409 |
| `application/usecase/GetDashboardUseCase.kt` | 123 / 68 | 125 / 68 | 127 / 68 | 84 / 53 |
| `infrastructure/client/DownstreamClient.kt` | 65 / 37 | 64 / 37 | 43 / 23 | 41 / 21 |
| エラーハンドラ（`presentation/BffExceptionHandler.kt` / `presentation/GlobalExceptionHandler.kt`） | 44 / 24 | 45 / 24 | 46 / 27 | 48 / 25 |
| 設定（`infrastructure/config/WebClientConfig.kt` / `infrastructure/config/DownstreamConfig.kt`） | 26 / 12 | 31 / 12 | 65 / 29 | 43 / 24 |

参考: `backend-stub` の `.kt` は 325 行（コメント・空行を除くと 138 行）。

## 負荷比較の結果

条件: VUS=50、DURATION=30s、下流は backend-stub、k6 v2.3.0、計測日 2026-10-22（`scripts/run-compare.sh` で測定）。

| 指標 | bff-webclient(8081) | bff-webflux(8084) | bff-restclient 並列(8082) | bff-restclient-sequential(8083) |
|---|---|---|---|---|
| スループット（req/s） | 119.9 | 119.4 | 30.8 | 48.0 |
| p50 レイテンシ | 413ms | 413ms | 1635ms | 1029ms |
| p95 レイテンシ | 432ms | 435ms | 1683ms | 1093ms |
| p99 レイテンシ | 492ms | 467ms | 1823ms | 1212ms |
| エラー率 | 0% | 0% | 0% | 0% |

注記:

- `bff-restclient` 並列が逐次版より遅い（30.8 vs 48.0 req/s）のは、固定スレッドプール（32 スレッド、全リクエスト共有）の枯渇が原因という仮説で、追加検証により支持された（各構成 2 回、キュー長は未観測）。
  - プールサイズ N を変えると RPS は N ÷（5 呼び出し × 0.2 秒）にほぼ一致（N=8 / 32 / 64 で 7.7 / 31.0 / 62.1 req/s）し、N=64 で逐次版を上回った。
  - 詳細は [docs/bff-selection-guide.md](docs/bff-selection-guide.md) の第 3 章「追加検証: 並列度の影響」を参照。
- オニオン化（DDD + onion architecture への再構成）の前後の比較はしていない。ここに載せた数値はオニオン化後の各モジュールのもの。

## Spring Boot 4.1.1 の注意点

- 本リポジトリの MVC スターターは `spring-boot-starter-webmvc`（`build.gradle.kts` で指定）。WebClient / RestClient はそれぞれ `-webclient` / `-restclient` を使う。
- JSON は Jackson 3（パッケージ `tools.jackson`）。Kotlin 対応は `tools.jackson.module:jackson-module-kotlin`（解決結果は 3.1.5）。
- Kotlin は `build.gradle.kts` のプラグインで 2.2.21 に固定している。解決された `kotlin-stdlib` も 2.2.21（`dependencyInsight` で確認）。Spring Boot BOM が管理する Kotlin 版との関係は本リポジトリでは確認していない。

## Java 21 での発展

本プロジェクトは Java 17 で動かしており、以下は未検証の設計上の見通しです。

- 仮想スレッド（Java 21）を使えば、`RestClient` のようなブロッキング呼び出しをそのまま書きつつ、プラットフォームスレッドの固定プールを減らせる。
- WebFlux（リアクティブ）は少数のイベントループで I/O を待たずに処理し、型としてリアクティブストリームを通す。`block()` のような呼び出し元のブロックは発生させない設計になる。
- 仮想スレッドでは「ブロッキングのままスケールさせる」、WebFlux では「非同期の型で最初から書く」という違いがある。

詳細な選び方は [docs/bff-selection-guide.md](docs/bff-selection-guide.md) を参照。
