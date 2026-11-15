// =====================================================================
// [정확성] 동시 요청에도 재고만큼만 발급되는가
// =====================================================================
// 시나리오
//   서로 다른 회원이 30초 동안 초당 5,000번 쿠폰 발급을 요청한다 (총 15만 건).
//   5,000장 재고를 두고 다 같이 경쟁한다 (선착순 쿠폰 오픈 순간).
//
// 판정 (k6 가 아니라 verify.sql 로 한다)
//   - 실제 발급 행 수(issued_coupon) > 총 수량       → 초과 발급 (FAIL)
//   - coupon.issued_quantity ≠ 실제 발급 행 수         → 카운터 갱신 손실 (FAIL)
//   - 같은 회원이 2장 이상                           → 중복 발급 (FAIL)
//
// 회원 사용 방식
//   테스트 회원 2만 명(재고의 4배)을 요청 순서대로 하나씩 사용한다.
//   2만 번째 이후 요청은 같은 회원이 다시 요청 → 409 이미 받음
//   → 같은 회원의 중복 요청 방어도 같이 확인된다.
//
// 사용 (Git Bash)
//   k6 run -e COUPON_ID=<쿠폰 ID> scripts/load/coupon-issue/over_issuance.js
//
// 옵션 (-e 로 전달)
//   BASE_URL  서버 주소            (기본 http://localhost:8080)
//   RATE      초당 요청 수          (기본 5000)
//   DURATION  지속 시간            (기본 30s)
//   VUS       미리 만들어 둘 VU 수  (기본 2000)
//   MAX_VUS   최대 VU 수           (기본 5000)
//
// 결과에서 볼 것
//   result_issued         발급 성공 수 (5,000 이어야 정상)
//   result_sold_out       매진 응답 수
//   result_already_issued 이미 받음 응답 수
//   result_other          그 외 (401 토큰 만료, 5xx, 타임아웃 등 → 0 이어야 정상)
//   http_reqs             실제로 보낸 요청 수
//   dropped_iterations    PC 가 못 따라가서 보내지 못한 요청 수 (부하 생성 한계 확인용)
// =====================================================================

import http from 'k6/http';
import exec from 'k6/execution';                 // 현재 요청이 전체 중 몇 번째인지 알기 위해
import { SharedArray } from 'k6/data';           // 큰 데이터를 모든 VU 가 같이 쓰도록 (메모리 절약)
import { Counter, Trend } from 'k6/metrics';     // Trend: 분포(평균/p95/p99), Counter: 누적 개수

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const COUPON_ID = __ENV.COUPON_ID;               // create_coupon.mjs 가 출력한 쿠폰 ID

// tokens.json 은 create_tokens.mjs 가 만든 파일 (scripts/load/tokens.json)
// open() 경로는 이 스크립트 파일 위치 기준이라 '../tokens.json'
// SharedArray 로 감싸면 VU 수천 개가 파일을 각자 복사하지 않고 하나를 같이 읽는다.
const tokens = new SharedArray('tokens', () => JSON.parse(open('../tokens.json')));

// 결과 지표 (k6 요약에 이 이름으로 출력됨)
const issueLatency = new Trend('issue_latency', true);        // 응답 시간 (true = 시간 단위로 표시)
const issued = new Counter('result_issued');                   // 200 발급 성공
const soldOut = new Counter('result_sold_out');                // 409 COUPON_SOLD_OUT
const alreadyIssued = new Counter('result_already_issued');    // 409 COUPON_ALREADY_ISSUED
const other = new Counter('result_other');                     // 그 외 (401, 5xx, 타임아웃 등)

export const options = {
    scenarios: {
        issue: {
            // constant-arrival-rate : VU 수와 상관없이 "초당 RATE 건"을 계속 보낸다.
            // 서버가 느려져도 요청 속도가 줄지 않아서, 오픈 순간 몰리는 상황을 그대로 만든다.
            executor: 'constant-arrival-rate',
            rate: Number(__ENV.RATE || 5000),
            timeUnit: '1s',
            duration: __ENV.DURATION || '30s',
            // VU = 동시에 요청을 처리하는 가상 사용자.
            // 응답이 느려지면 동시에 기다리는 요청이 늘어서 VU 가 더 필요하다 → 최대 MAX_VUS 까지 늘어남
            preAllocatedVUs: Number(__ENV.VUS || 2000),
            maxVUs: Number(__ENV.MAX_VUS || 5000),
        },
    },
};

// 테스트 시작 전에 한 번 실행 : 필수 값 확인
export function setup() {
    if (!COUPON_ID) throw new Error('COUPON_ID 가 필요합니다. (-e COUPON_ID=...)');
    if (tokens.length === 0) throw new Error('tokens.json 이 비어 있습니다. create_tokens.mjs 를 먼저 실행하세요.');
}

// 요청 1건 (constant-arrival-rate 가 초당 RATE 번 호출)
export default function () {
    // iterationInTest : 전체 테스트에서 이 요청이 몇 번째인지 (0, 1, 2, ...)
    // → 회원을 순서대로 하나씩 사용, 2만 명을 다 쓰면 처음부터 다시
    const token = tokens[exec.scenario.iterationInTest % tokens.length];

    const res = http.post(`${BASE}/api/v1/coupon/${COUPON_ID}/issue`, null, {
        headers: { Authorization: `Bearer ${token}` },
        tags: { name: 'coupon_issue' },    // k6 요약에서 URL 별로 흩어지지 않게 이름 고정
    });

    issueLatency.add(res.timings.duration);   // 응답 시간(ms) 기록

    // 응답별 개수 (409 는 바디의 statusCode 로 매진 / 이미 받음 구분)
    if (res.status === 200) {
        issued.add(1);
    } else if (res.status === 409) {
        const code = res.json('statusCode');
        if (code === 'COUPON_SOLD_OUT') soldOut.add(1);
        else if (code === 'COUPON_ALREADY_ISSUED') alreadyIssued.add(1);
        else other.add(1);
    } else {
        other.add(1);
    }
}