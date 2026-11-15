# 쿠폰 발급 부하 테스트

선착순 쿠폰 오픈 순간처럼 서로 다른 회원이 동시에 몰릴 때
**총 수량만큼만 발급되는지**(정확성)와 **응답 시간**(성능)을 발급 방식별로 비교한다.

| 항목 | 값 |
|---|---|
| 쿠폰 수량 | 5,000장 |
| 부하 | 초당 5,000건 × 30초 (총 15만 건) |
| 회원 | 2만 명 (재고의 4배) |

## 파일

| 파일 | 하는 일 |
|---|---|
| `setup_members.sql` | 테스트 회원 2만 명 + 관리자 생성 (`run.sh` 가 자동 실행) |
| `create_tokens.mjs` | 테스트 회원 로그인 → `tokens.json` |
| `create_coupon.mjs` | 테스트 쿠폰 생성 → 쿠폰 ID 출력 |
| `coupon-issue/run.sh` | 토큰 → 워밍업 → 쿠폰 생성 → k6 → 판정 **한 번에 실행** |
| `coupon-issue/over_issuance.js` | [정확성] 재고만큼만 발급되는가 |
| `coupon-issue/issue_burst.js` | [성능] 응답 시간 p99 |
| `coupon-issue/verify.sql` | 초과 발급 / 카운터 불일치 / 중복 발급 판정 |
| `cleanup.sql` | 테스트 쿠폰·발급 데이터 정리 |

## 테스트 회원이 필요한 이유

`issued_coupon.member_id` 가 회원 FK 라서, 발급에 성공한 회원은 실제로 DB 에 있어야 한다.
그래서 회원을 미리 만들고, 로그인 토큰도 미리 받아 둔다 (부하 중에 로그인 비용이 섞이지 않도록).

테스트 회원(`loadtest00001~20000`)과 테스트 관리자(`loadadmin`)는 `setup_members.sql` 이 만든다.
비밀번호는 모두 `loadtest1234` 로 고정 (로컬 테스트 전용).

## 실행 환경

| 구성 | 실행 위치 |
|---|---|
| baseshop 서버 | PC (localhost:8080) |
| MySQL, Redis | Docker 컨테이너 |
| k6 | Docker (`grafana/k6`) 또는 PC 설치 |
| 토큰·쿠폰 생성 | PC (Node 18 이상) |

컨테이너 이름 확인 : `docker ps --format "{{.Names}}  {{.Image}}"`

## 준비 (한 번만)

```bash
# 1. k6 이미지 (PC 에 설치할 때는 winget install k6 --source winget)
docker pull grafana/k6
```

`tokens.json` 은 `.gitignore` 에 추가한다 (`scripts/load/tokens.json`).

## 발급 방식 선택

쿠폰 발급 서비스에서 `CouponIssuedProcessor` 를 주입받는 `@Qualifier` 값을
측정할 방식의 빈 이름으로 바꾸고 서버를 다시 띄운다.

| 방식 | 클래스 | `@Qualifier` 값 |
|---|---|---|
| 비관적 락 | `PessimisticLockCouponIssueProcessor` | `"pessimisticLockCouponIssueProcessor"` |
| Redis Lua | `RedisLuaCouponIssueProcessor` | `"redisLuaCouponIssueProcessor"` |

빈 이름은 클래스 이름의 첫 글자를 소문자로 바꾼 것 (`@Component` 기본 규칙).

## 실행 (Git Bash)

처음 한 번 : `.env` 작성 (커밋되지 않음)

```bash
cp scripts/load/.env.example scripts/load/.env
# scripts/load/.env 를 열어 값 채우기
```

이후에는 바로 실행

```bash
bash scripts/load/coupon-issue/run.sh                 # 정확성
bash scripts/load/coupon-issue/run.sh issue_burst     # 성능
```

`run.sh` 가 하는 일

0. 테스트 회원·관리자 준비 (`setup_members.sql`, 이미 있으면 건너뜀)
1. 테스트 회원 토큰 생성 (Access Token 30분 만료 → 매번 새로)
2. 워밍업 : 쿠폰 하나로 같은 부하를 한 번 주고 결과는 버림 (JIT, 커넥션 풀 준비)
3. 본 측정용 새 쿠폰 생성
4. k6 실행
5. `verify.sql` 실행 + Redis 재고 / 받은 회원 수 출력

| 옵션 | 예 | 설명 |
|---|---|---|
| `WARMUP=0` | `WARMUP=0 bash .../run.sh` | 워밍업 생략 |
| `SKIP_TOKENS=1` | `SKIP_TOKENS=1 bash .../run.sh` | 토큰 생성 생략 (30분 안에 재실행) |
| `K6_MODE=local` | `K6_MODE=local bash .../run.sh` | PC 에 설치한 k6 사용 |
| k6 옵션 | `bash .../run.sh over_issuance -e RATE=1000 -e DURATION=10s` | 부하 조절 |

### Docker k6 실행 시 옵션 설명

| 옵션 | 이유 |
|---|---|
| `BASE_URL=http://host.docker.internal:8080` | 컨테이너 안의 `localhost` 는 컨테이너 자신이라 PC 의 서버에 닿지 않음 |
| `-v "$(pwd -W)/scripts/load:/load"` | `tokens.json` 과 스크립트를 컨테이너에 연결 |
| `MSYS_NO_PATHCONV=1` | Git Bash 가 `/load/...` 를 Windows 경로로 바꾸지 않도록 |

## 판정 기준

| 항목 | 정상 |
|---|---|
| `over_issuance` | OK (발급 행 수 ≤ 총 수량) |
| `count_match` | OK (`issued_quantity` = 발급 행 수) |
| 같은 회원 2장 이상 | 결과 없음 |
| Redis (Redis 방식) | 총 수량 = 발급 행 수 + 남은 재고, 받은 회원 수 = 발급 행 수 |
| k6 `result_other` / `status_other` | 0 |

## 기록할 값

| 값 | 어디서 |
|---|---|
| 발급 성공 / 매진 / 이미 받음 / 그 외 | k6 결과 (`result_*`, `status_*`) |
| 응답 시간 p95, p99 | k6 결과 (`issue_latency`) |
| 실제 보낸 요청 / 못 보낸 요청 | k6 결과 (`http_reqs`, `dropped_iterations`) |
| 초과 발급, 카운터 일치, 중복 발급 | `verify.sql` |

서버, DB, k6 를 같은 PC 에서 돌리면 초당 5,000건이 다 나가지 않을 수 있다.
Docker k6 는 WSL2 위에서 동작해서 로컬 설치보다 `dropped_iterations` 가 더 많을 수 있다.
**같은 실행 환경에서 방식끼리 비교**하고, 결과에 실행 환경도 같이 적는다.

## 정리

```bash
docker exec -i -e MYSQL_PWD='<MySQL 비밀번호>' <MySQL 컨테이너> \
  mysql -u root baseshop < scripts/load/cleanup.sql

docker exec <Redis 컨테이너> redis-cli DEL coupon:<쿠폰ID>:stock coupon:<쿠폰ID>:members
```