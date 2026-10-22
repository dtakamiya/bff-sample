#!/usr/bin/env bash
# 下流スタブ(backend-stub)と4つの BFF を同条件で負荷計測する。
# BFF は同時に起動せず 1 つずつ直列に測る（公平性のため）。
#   bff-webclient:8081 / bff-restclient:8082 / bff-restclient-sequential:8083 / bff-webflux:8084
#
# 使い方: scripts/run-compare.sh
# 環境変数:
#   VUS=50         同時仮想ユーザー数（k6 の vus）
#   DURATION=30s   計測時間（k6 の duration）
#   SKIP_K6=1      k6 を実行せず、ビルド・起動・疎通確認・後始末だけを行う
#   K6_SUMMARY_TREND_STATS  サマリの統計値（既定 avg,min,med,max,p(90),p(95),p(99)。k6 が直接読む）
# 結果: build/load-results/<module>.json（k6 --summary-export）
set -euo pipefail

readonly PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
readonly RESULTS_DIR="$PROJECT_DIR/build/load-results"
readonly LOG_DIR="$PROJECT_DIR/build/load-logs"
readonly K6_SCRIPT="$PROJECT_DIR/load/dashboard.js"

readonly STUB_MODULE="backend-stub"
readonly STUB_PORT=9090
readonly BFF_TARGETS=("bff-webclient:8081" "bff-restclient:8082" "bff-restclient-sequential:8083" "bff-webflux:8084")

readonly READY_RETRIES=60
readonly READY_INTERVAL_SEC=1
readonly DASHBOARD_USER_ID="u1"

readonly VUS="${VUS:-50}"
readonly DURATION="${DURATION:-30s}"
readonly SKIP_K6="${SKIP_K6:-0}"
# k6 は環境変数 K6_SUMMARY_TREND_STATS を直接読むため、子プロセスに渡せるよう export する。
export K6_SUMMARY_TREND_STATS="${K6_SUMMARY_TREND_STATS:-avg,min,med,max,p(90),p(95),p(99)}"

STUB_PID=""
BFF_PID=""

log() {
  printf '[run-compare] %s\n' "$*"
}

die() {
  printf '[run-compare] ERROR: %s\n' "$*" >&2
  exit 1
}

cleanup() {
  stop_pid "$BFF_PID"
  stop_pid "$STUB_PID"
}

stop_pid() {
  local pid="$1"
  if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
    kill "$pid" 2>/dev/null || true
    wait "$pid" 2>/dev/null || true
  fi
}

check_prerequisites() {
  if ! [[ "$VUS" =~ ^[1-9][0-9]*$ ]]; then
    die "VUS must be a positive integer, got: $VUS"
  fi
  if [ "$SKIP_K6" = "1" ]; then
    log "SKIP_K6=1: k6 の計測はスキップします"
    return 0
  fi
  if ! command -v k6 >/dev/null 2>&1; then
    die "k6 が見つかりません。brew install k6 が必要です"
  fi
}

require_free_port() {
  local port="$1"
  if lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
    die "port $port is already in use. Stop the process holding it and retry."
  fi
}

build_jars() {
  log "bootJar をビルドします"
  (cd "$PROJECT_DIR" && ./gradlew --console=plain \
    ":$STUB_MODULE:bootJar" \
    ":bff-webclient:bootJar" \
    ":bff-restclient:bootJar" \
    ":bff-restclient-sequential:bootJar" \
    ":bff-webflux:bootJar")
}

jar_path() {
  printf '%s/%s/build/libs/%s.jar' "$PROJECT_DIR" "$1" "$1"
}

# wait_until_ready <name> <url> <pid> <log_file>
# 起動待ちは actuator ではなく、実際の API への curl リトライで行う。
wait_until_ready() {
  local name="$1" url="$2" pid="$3" log_file="$4"
  local attempt
  for attempt in $(seq 1 "$READY_RETRIES"); do
    if ! kill -0 "$pid" 2>/dev/null; then
      tail -n 30 "$log_file" >&2
      die "$name exited before becoming ready (log: $log_file)"
    fi
    if curl -sf -o /dev/null --max-time 2 "$url"; then
      log "$name is ready: $url"
      return 0
    fi
    sleep "$READY_INTERVAL_SEC"
  done
  tail -n 30 "$log_file" >&2
  die "$name did not become ready within ${READY_RETRIES}s: $url"
}

run_k6() {
  local module="$1" base_url="$2"
  local result_file="$RESULTS_DIR/$module.json"
  log "k6 計測: $module ($base_url) VUS=$VUS DURATION=$DURATION"
  k6 run \
    -e "BASE_URL=$base_url" \
    -e "VUS=$VUS" \
    -e "DURATION=$DURATION" \
    --summary-export "$result_file" \
    "$K6_SCRIPT"
  log "結果を保存しました: $result_file"
}

start_stub() {
  java -jar "$(jar_path "$STUB_MODULE")" >"$LOG_DIR/$STUB_MODULE.log" 2>&1 &
  STUB_PID=$!
  wait_until_ready "$STUB_MODULE" "http://localhost:$STUB_PORT/users/$DASHBOARD_USER_ID" \
    "$STUB_PID" "$LOG_DIR/$STUB_MODULE.log"
}

# measure_bff <module> <port>: BFF を1つだけ起動して計測し、停止して終了を待つ
measure_bff() {
  local module="$1" port="$2"
  require_free_port "$port"
  java -jar "$(jar_path "$module")" >"$LOG_DIR/$module.log" 2>&1 &
  BFF_PID=$!
  wait_until_ready "$module" "http://localhost:$port/api/dashboard/$DASHBOARD_USER_ID" \
    "$BFF_PID" "$LOG_DIR/$module.log"
  if [ "$SKIP_K6" != "1" ]; then
    run_k6 "$module" "http://localhost:$port"
  fi
  stop_pid "$BFF_PID"
  BFF_PID=""
  log "$module を停止しました"
}

main() {
  trap cleanup EXIT
  trap 'exit 130' INT
  trap 'exit 143' TERM
  check_prerequisites
  require_free_port "$STUB_PORT"
  for target in "${BFF_TARGETS[@]}"; do
    require_free_port "${target##*:}"
  done

  mkdir -p "$RESULTS_DIR" "$LOG_DIR"
  build_jars
  start_stub

  for target in "${BFF_TARGETS[@]}"; do
    measure_bff "${target%%:*}" "${target##*:}"
  done

  log "完了"
}

main "$@"
