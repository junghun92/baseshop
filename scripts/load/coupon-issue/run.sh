#!/usr/bin/env bash
# =====================================================================
# 쿠폰 발급 부하 테스트 한 번에 실행
#   토큰 생성 → (워밍업) → 테스트 쿠폰 생성 → k6 실행 → 결과 판정
# =====================================================================
# 사용 (Git Bash, 아무 폴더에서나)
#   bash scripts/load/coupon-issue/run.sh                 # 정확성 (over_issuance)
#   bash scripts/load/coupon-issue/run.sh issue_burst     # 성능 (issue_burst)
#
# 첫 번째 인자 뒤에 붙인 값은 k6 에 그대로 넘어간다
#   bash scripts/load/coupon-issue/run.sh over_issuance -e RATE=1000 -e DURATION=10s
#
# 환경 변수는 scripts/load/.env 에 적어 두면 자동으로 읽는다 (.env.example 참고)
#
# 필수 환경 변수
#   MYSQL_CONTAINER  MySQL 컨테이너 이름 (테스트 회원 준비, verify.sql 실행)
#   MYSQL_PWD        MySQL 비밀번호
#
# 선택 환경 변수
#   K6_MODE          docker | local  (기본 docker : grafana/k6 이미지로 실행)
#   WARMUP           1 | 0           (기본 1 : 본 측정 전에 한 번 돌리고 결과는 버림)
#   SKIP_TOKENS      1 이면 토큰 생성 생략 (30분 안에 다시 돌릴 때)
#   MYSQL_USER       MySQL 사용자 (기본 root)
#   REDIS_CONTAINER  Redis 컨테이너 이름 (있으면 Redis 재고 / 받은 회원 수 출력)
#
# 미리 필요한 것
#   baseshop 서버, MySQL, Redis 실행 중 / demo 데이터와 상관없이 동작
#   (테스트 회원 2만 명, 관리자 loadadmin 은 setup_members.sql 로 자동 준비)
#   측정할 발급 방식을 서비스의 @Qualifier 로 지정하고 서버 재시작
# =====================================================================

set -euo pipefail

SCRIPT="${1:-over_issuance}"          # 실행할 k6 스크립트 이름
shift || true                         # 나머지 인자는 k6 옵션으로 넘김
K6_ARGS=("$@")

K6_MODE="${K6_MODE:-docker}"
WARMUP="${WARMUP:-1}"

# 프로젝트 루트로 이동 (이 파일 위치 : scripts/load/coupon-issue/)
cd "$(dirname "$0")/../../.."

# ---------------------------------------------------------------------
# scripts/load/.env 가 있으면 읽어서 환경 변수로 등록
#   set -a : 읽은 변수를 자동으로 export (node, docker 에도 전달되도록)
#   sed    : Windows 에서 저장한 파일의 줄 끝 \r 제거 (값 뒤에 \r 이 붙는 문제 방지)
#   이미 export 해 둔 값이 있어도 .env 값으로 덮어쓴다
# ---------------------------------------------------------------------
ENV_FILE="scripts/load/.env"
if [[ -f "$ENV_FILE" ]]; then
  set -a
  source <(sed 's/\r$//' "$ENV_FILE")
  set +a
fi

# ---------------------------------------------------------------------
# 실행할 스크립트 확인
# ---------------------------------------------------------------------
if [[ ! -f "scripts/load/coupon-issue/${SCRIPT}.js" ]]; then
  echo "스크립트가 없습니다 : scripts/load/coupon-issue/${SCRIPT}.js" >&2
  exit 1
fi

# ---------------------------------------------------------------------
# k6 실행 (docker / local)
#   threshold 미달이면 k6 가 종료 코드 99 를 반환 → 스크립트를 멈추지 않고 계속 진행
# ---------------------------------------------------------------------
run_k6() {
  local coupon_id="$1"
  if [[ "$K6_MODE" == "docker" ]]; then
    # 컨테이너 안의 localhost 는 컨테이너 자신 → PC 서버는 host.docker.internal
    # Git Bash 경로 변환 방지(MSYS_NO_PATHCONV), Windows 경로 전달(pwd -W)
    MSYS_NO_PATHCONV=1 docker run --rm \
      -v "$(pwd -W)/scripts/load:/load" \
      grafana/k6 run \
      -e BASE_URL=http://host.docker.internal:8080 \
      -e COUPON_ID="$coupon_id" \
      "${K6_ARGS[@]}" \
      "/load/coupon-issue/${SCRIPT}.js" || echo "(k6 종료 코드 $? : 99 는 threshold 미달)"
  else
    k6 run \
      -e COUPON_ID="$coupon_id" \
      "${K6_ARGS[@]}" \
      "scripts/load/coupon-issue/${SCRIPT}.js" || echo "(k6 종료 코드 $? : 99 는 threshold 미달)"
  fi
}

# ---------------------------------------------------------------------
# 0. 테스트 회원 준비 (setup_members.sql)
#    issued_coupon.member_id 가 회원 FK 라서 요청하는 회원이 DB 에 있어야 한다.
#    없는 회원만 만들고, 비밀번호를 loadtest1234 로 맞춘다 (매번 실행해도 안전)
# ---------------------------------------------------------------------
: "${MYSQL_CONTAINER:?MYSQL_CONTAINER 환경 변수가 필요합니다 (테스트 회원 준비용)}"
: "${MYSQL_PWD:?MYSQL_PWD 환경 변수가 필요합니다 (테스트 회원 준비용)}"

mysql_exec() {
  docker exec -i -e MYSQL_PWD="$MYSQL_PWD" "$MYSQL_CONTAINER" \
    mysql -u "${MYSQL_USER:-root}" baseshop "$@"
}

echo "== 0. 테스트 회원 준비"
mysql_exec --table < scripts/load/setup_members.sql

# ---------------------------------------------------------------------
# 1. 토큰 생성
# ---------------------------------------------------------------------
if [[ "${SKIP_TOKENS:-0}" != "1" ]]; then
  echo "== 1. 테스트 회원 토큰 생성"
  node scripts/load/create_tokens.mjs
fi

# ---------------------------------------------------------------------
# 2. 워밍업 (결과 버림)
#    서버를 띄운 직후 첫 측정은 JIT 컴파일, 커넥션 풀 생성 때문에 느리게 나온다.
#    같은 부하를 한 번 먼저 주고, 본 측정은 새 쿠폰으로 한다.
# ---------------------------------------------------------------------
if [[ "$WARMUP" == "1" ]]; then
  echo "== 2. 워밍업 (결과 버림)"
  WARMUP_ID=$(node scripts/load/create_coupon.mjs)
  run_k6 "$WARMUP_ID" > /dev/null
fi

# ---------------------------------------------------------------------
# 3. 본 측정
# ---------------------------------------------------------------------
echo "== 3. 본 측정 쿠폰 생성"
COUPON_ID=$(node scripts/load/create_coupon.mjs)
echo "쿠폰 ID = $COUPON_ID"

echo "== 4. k6 실행 : ${SCRIPT} (${K6_MODE})"
run_k6 "$COUPON_ID"

# ---------------------------------------------------------------------
# 4. 판정
#    MySQL 이 Docker 컨테이너라서 docker exec 로 컨테이너 안의 mysql 을 실행한다.
#    verify.sql 의 "SET @coupon_id = 0;" 을 이번 쿠폰 ID 로 바꿔서 넘긴다.
#    비밀번호는 -e MYSQL_PWD 로 컨테이너에 전달 (명령줄에 비밀번호가 보이지 않게)
# ---------------------------------------------------------------------
echo "== 5. 결과 판정 (쿠폰 ID = $COUPON_ID)"
if [[ -n "${MYSQL_CONTAINER:-}" && -n "${MYSQL_PWD:-}" ]]; then
  sed "s/SET @coupon_id = 0;/SET @coupon_id = ${COUPON_ID};/" scripts/load/coupon-issue/verify.sql \
    | docker exec -i -e MYSQL_PWD="$MYSQL_PWD" "$MYSQL_CONTAINER" \
        mysql -u "${MYSQL_USER:-root}" baseshop --table
else
  echo "MYSQL_CONTAINER, MYSQL_PWD 가 없어서 자동 판정을 건너뜁니다."
  echo "verify.sql 의 @coupon_id 를 ${COUPON_ID} 로 바꿔서 직접 실행하세요."
fi

# Redis 값 (Redis 방식일 때 확인용)
#   정상 : 총 수량 = DB 발급 행 수 + 남은 재고, 받은 회원 수 = DB 발급 행 수
if [[ -n "${REDIS_CONTAINER:-}" ]]; then
  echo "남은 재고    : $(docker exec "$REDIS_CONTAINER" redis-cli GET "coupon:${COUPON_ID}:stock")"
  echo "받은 회원 수 : $(docker exec "$REDIS_CONTAINER" redis-cli SCARD "coupon:${COUPON_ID}:members")"
fi

echo "== 완료 (쿠폰 ID = $COUPON_ID)"