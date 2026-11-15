// =====================================================================
// 관리자 로그인 → 지금 바로 발급 가능한 테스트 쿠폰 생성 → 쿠폰 ID 출력
// =====================================================================
// 왜 필요한가
//   회차마다 새 쿠폰을 만들어서, 그 쿠폰 ID 로만 발급 결과를 검증한다.
//   로컬 DB 에 실제 작업 데이터가 있어서 테이블을 비우지(TRUNCATE) 않기 위해서다.
//
// 사용 (Git Bash)
//   COUPON_ID=$(node scripts/load/create_coupon.mjs)
//   관리자 계정(loadadmin)은 setup_members.sql 이 만든다.
//
// 옵션 (환경 변수)
//   BASE_URL  서버 주소     (기본 http://localhost:8080)
//   TOTAL     쿠폰 총 수량   (기본 5000)
//
// 만들어지는 쿠폰
//   이름        "load test <생성 시각>"  ← cleanup.sql 이 이 이름으로 테스트 쿠폰을 찾아 지움
//   할인        1,000원, 최소 주문 금액 0원
//   발급 기간   1분 전 ~ 하루 뒤 (만들자마자 발급 가능)
//
// 참고
//   쿠폰을 만들면 AdminCouponService 가 Redis 재고(coupon:{id}:stock)도 총 수량으로 넣는다.
//   그래서 Redis 방식 테스트도 따로 준비할 것이 없다.
//
// 출력
//   표준 출력에는 쿠폰 ID 숫자만 나온다 (COUPON_ID=$(...) 로 받기 위해).
//   에러는 표준 에러로 출력.
// =====================================================================

const BASE = process.env.BASE_URL ?? 'http://localhost:8080';
const TOTAL = Number(process.env.TOTAL ?? 5000);

// 부하 테스트 관리자 (로컬 테스트 전용, setup_members.sql 에서 생성)
const ADMIN_USERNAME = 'loadadmin';
const ADMIN_PASSWORD = 'loadtest1234';

/**
 * Date → "2026-11-15T15:10:00"
 * - 서버는 LocalDateTime(시간대 없음, 한국 시각)으로 받는다.
 * - toISOString() 은 UTC 로 바뀌어서 9시간이 밀리므로 쓰지 않고, 로컬 시각 그대로 만든다.
 */
function toLocalDateTime(date) {
    const p = (n) => String(n).padStart(2, '0');
    return `${date.getFullYear()}-${p(date.getMonth() + 1)}-${p(date.getDate())}`
        + `T${p(date.getHours())}:${p(date.getMinutes())}:${p(date.getSeconds())}`;
}

// 1. 관리자 로그인 → 응답 헤더 Authorization 에서 "Bearer <토큰>" 을 그대로 받음
const loginRes = await fetch(`${BASE}/api/v1/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: ADMIN_USERNAME, password: ADMIN_PASSWORD }),
});
const auth = loginRes.headers.get('authorization');
if (!loginRes.ok || !auth) {
    console.error(`관리자 로그인 실패 (HTTP ${loginRes.status})`);
    process.exit(1);
}

// 2. 쿠폰 생성 (POST /api/v1/admin/coupon, ADMIN 권한 필요)
const now = new Date();
const res = await fetch(`${BASE}/api/v1/admin/coupon`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: auth },
    body: JSON.stringify({
        name: `load test ${toLocalDateTime(now)}`,
        discountAmount: 1000,
        minOrderAmount: 0,
        totalQuantity: TOTAL,
        issueStartDate: toLocalDateTime(new Date(now.getTime() - 60 * 1000)),         // 1분 전 → 바로 발급 가능
        issueEndDate: toLocalDateTime(new Date(now.getTime() + 24 * 60 * 60 * 1000)), // 하루 뒤
        validDays: 7,
    }),
});

// 3. 응답은 ApiResponse 형식 { status, statusCode, message, data } → data 가 쿠폰 ID
const body = await res.json();
if (!res.ok) {
    console.error(`쿠폰 생성 실패 (HTTP ${res.status})`, body);
    process.exit(1);
}
console.log(body.data);