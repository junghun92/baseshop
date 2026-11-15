// =====================================================================
// [성능] 발급 요청이 폭증할 때 응답 시간
// =====================================================================
// 시나리오
//   회원 2만 명 중 랜덤으로 30초 동안 초당 5,000번 발급 요청 (총 15만 건).
//   재고 5,000 << 회원 2만 → 대부분 409(매진 또는 이미 받음) 응답을 받는다.
//   보고 싶은 것은 발급 성공 수가 아니라 "응답 시간 분포"(p95, p99) 이다.
//
// 목표 (thresholds)
//   issue_latency p(99) < 500ms
//   - DB 저장까지 요청 안에서 끝내는 방식(비관적 락, Redis Lua + DB 동기 저장)은 못 넘을 수 있다.
//   - 큐로 DB 저장을 분리한 뒤 통과하는 것이 목표.
//   k6 는 threshold 를 못 넘으면 종료 코드 99 로 끝난다 (실패가 아니라 "목표 미달" 표시)
//
// 사용 (Git Bash)
//   k6 run -e COUPON_ID=<쿠폰 ID> scripts/load/coupon-issue/issue_burst.js
//
// 옵션 (-e 로 전달) : over_issuance.js 와 같음 (BASE_URL, RATE, DURATION, VUS, MAX_VUS)
//
// 결과에서 볼 것
//   issue_latency (avg, p95, p99)
//   status_2xx                   발급 성공 수
//   status_409_sold_out_or_dup   매진 또는 이미 받음 (예상된 거절)
//   status_other                 그 외 (0 이어야 정상)
//   http_reqs, dropped_iterations
// =====================================================================

import http from 'k6/http';
import { SharedArray } from 'k6/data';
import { Counter, Trend } from 'k6/metrics';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const COUPON_ID = __ENV.COUPON_ID;

// create_tokens.mjs 가 만든 토큰 2만 개 (모든 VU 가 같이 읽음)
const tokens = new SharedArray('tokens', () => JSON.parse(open('../tokens.json')));

const issueLatency = new Trend('issue_latency', true);           // 응답 시간 분포
const status2xx = new Counter('status_2xx');                      // 발급 성공
const status409 = new Counter('status_409_sold_out_or_dup');      // 매진 or 이미 받음 (예상된 거절)
const statusOther = new Counter('status_other');                  // 401, 5xx, 타임아웃 등 비정상

export const options = {
    scenarios: {
        burst: {
            // VU 수와 상관없이 초당 RATE 건을 강제로 보냄 (서버가 느려도 속도 유지)
            executor: 'constant-arrival-rate',
            rate: Number(__ENV.RATE || 5000),
            timeUnit: '1s',
            duration: __ENV.DURATION || '30s',
            preAllocatedVUs: Number(__ENV.VUS || 2000),
            maxVUs: Number(__ENV.MAX_VUS || 5000),
        },
    },
    thresholds: {
        // 응답 시간 목표 : 99% 의 요청이 500ms 안에 응답
        issue_latency: ['p(99)<500'],
    },
};

export function setup() {
    if (!COUPON_ID) throw new Error('COUPON_ID 가 필요합니다. (-e COUPON_ID=...)');
    if (tokens.length === 0) throw new Error('tokens.json 이 비어 있습니다. create_tokens.mjs 를 먼저 실행하세요.');
}

export default function () {
    // 2만 명 중 랜덤 (같은 회원이 여러 번 걸릴 수 있음 → 이미 받음 409)
    const token = tokens[Math.floor(Math.random() * tokens.length)];

    const res = http.post(`${BASE}/api/v1/coupon/${COUPON_ID}/issue`, null, {
        headers: { Authorization: `Bearer ${token}` },
        tags: { name: 'coupon_issue' },
    });

    issueLatency.add(res.timings.duration);

    if (res.status >= 200 && res.status < 300) status2xx.add(1);
    else if (res.status === 409) status409.add(1);
    else statusOther.add(1);
}