// =====================================================================
// 테스트 회원 로그인 → Access Token 을 tokens.json 에 저장
// =====================================================================
// 왜 필요한가
//   쿠폰 발급 API 는 로그인한 회원만 호출할 수 있다 (JWT).
//   k6 가 부하를 주는 동안 로그인까지 하면 로그인 비용이 측정에 섞이므로,
//   테스트 전에 토큰을 미리 받아 파일에 저장해 두고 k6 는 읽기만 한다.
//
// 사용 (Git Bash)
//   node scripts/load/create_tokens.mjs
//
// 옵션 (환경 변수)
//   BASE_URL     서버 주소            (기본 http://localhost:8080)
//   USERS        토큰을 받을 회원 수   (기본 20000, setup_members.sql 과 맞춤)
//   CONCURRENCY  동시에 보낼 로그인 수 (기본 50)
//
// 주의
//   - Access Token 만료가 30분이라 테스트 직전에 실행한다.
//   - 로그인은 BCrypt 비교 때문에 느리다 (2만 명 기준 1~2분).
//   - tokens.json 은 .gitignore 에 넣어서 커밋하지 않는다.
// =====================================================================

import { writeFile } from 'node:fs/promises';

const BASE = process.env.BASE_URL ?? 'http://localhost:8080';
const USERS = Number(process.env.USERS ?? 20000);
const CONCURRENCY = Number(process.env.CONCURRENCY ?? 50);

// 테스트 회원 비밀번호 (로컬 테스트 전용)
// setup_members.sql 의 해시(@pw)와 같은 비밀번호여야 한다.
const PASSWORD = 'loadtest1234';

// loadtest00001 ~ loadtest20000 (setup_members.sql 에서 만든 이름과 같아야 함)
const usernames = Array.from({ length: USERS }, (_, i) => `loadtest${String(i + 1).padStart(5, '0')}`);

const tokens = new Array(USERS);   // 회원 순서대로 토큰 저장 (tokens[0] = loadtest00001)
let next = 0;                      // 다음에 로그인할 회원 순번 (worker 들이 같이 사용)

/**
 * 로그인 1건
 * - POST /api/v1/login, 바디 {username, password}
 * - 성공하면 응답 헤더 Authorization: "Bearer <토큰>" 으로 Access Token 이 옴 (LoginSuccessHandler)
 * - "Bearer " 를 떼고 토큰 문자열만 반환
 */
async function login(username) {
    const res = await fetch(`${BASE}/api/v1/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password: PASSWORD }),
    });
    const auth = res.headers.get('authorization');
    if (!res.ok || !auth) {
        throw new Error(`${username} 로그인 실패 (HTTP ${res.status})`);
    }
    return auth.replace('Bearer ', '');
}

/**
 * 로그인을 하나씩 계속 처리하는 작업자
 * - 남은 회원이 없을 때까지 next 를 하나씩 가져가서 로그인
 * - 이런 작업자를 CONCURRENCY 개 동시에 돌려서, 한 번에 50건씩 로그인이 진행됨
 *   (2만 건을 순서대로 하면 BCrypt 때문에 너무 오래 걸림)
 */
async function worker() {
    while (next < USERS) {
        const i = next++;
        tokens[i] = await login(usernames[i]);
    }
}

const started = Date.now();

// 작업자 CONCURRENCY 개를 동시에 실행하고, 모두 끝날 때까지 기다림
await Promise.all(Array.from({ length: CONCURRENCY }, worker));

// 이 스크립트와 같은 폴더(scripts/load)에 tokens.json 저장
await writeFile(new URL('./tokens.json', import.meta.url), JSON.stringify(tokens));

console.log(`토큰 ${tokens.length}개 저장 (${((Date.now() - started) / 1000).toFixed(1)}초)`);