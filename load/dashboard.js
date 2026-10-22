// BFF の GET /api/dashboard/{userId} を負荷計測する k6 スクリプト。
// 必須: BASE_URL (例: http://localhost:8081)
// 任意: VUS (既定 50), DURATION (既定 30s)
// 結果の要約は標準出力に出る。JSON で保存するには --summary-export を付ける。
import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL;
if (!BASE_URL) {
  throw new Error('BASE_URL is required, e.g. BASE_URL=http://localhost:8081');
}

const VUS = parseInt(__ENV.VUS || '50', 10);
if (!Number.isInteger(VUS) || VUS <= 0) {
  throw new Error(`VUS must be a positive integer, got: ${__ENV.VUS}`);
}
const DURATION = __ENV.DURATION || '30s';

const DASHBOARD_PATH = '/api/dashboard/u1';

export const options = {
  vus: VUS,
  duration: DURATION,
};

function hasTotalAmount(res) {
  try {
    const body = res.json();
    return body !== null && typeof body === 'object' && 'totalAmount' in body;
  } catch (e) {
    return false;
  }
}

export default function () {
  const res = http.get(`${BASE_URL}${DASHBOARD_PATH}`);
  check(res, {
    'status is 200': (r) => r.status === 200,
    'totalAmount exists': (r) => r.status === 200 && hasTotalAmount(r),
  });
}
