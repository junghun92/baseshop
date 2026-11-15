-- =====================================================================
-- 쿠폰 발급 결과 판정 (over_issuance.js / issue_burst.js 실행 후)
-- =====================================================================
-- 사용
--   @coupon_id 에 create_coupon.mjs 가 출력한 쿠폰 ID 를 넣고 실행한다.
--
-- 정상 결과
--   1번 : over_issuance = OK, count_match = OK, issued_rows = 총 수량(5000)
--   2번 : 결과 없음 (같은 회원 2장 이상이 없음)
--
-- Redis 방식일 때는 Redis 값도 같이 확인 (아래 3번)
-- =====================================================================

SET @coupon_id = 0;   -- ← 쿠폰 ID 로 변경

-- 1. 초과 발급 / 카운터 불일치
--    total_quantity  : 총 수량 (쿠폰 정책)
--    issued_quantity : coupon 테이블의 발급 수량 카운터 (UPDATE ... + 1 로 증가)
--    issued_rows     : issued_coupon 에 실제로 저장된 발급 행 수
--    over_issuance   : issued_rows > total_quantity        → FAIL (초과 발급)
--    count_match     : issued_quantity ≠ issued_rows        → FAIL (카운터 갱신 손실)
SELECT v.coupon_id,
       v.total_quantity,
       v.issued_quantity,
       v.issued_rows,
       IF(v.issued_rows > v.total_quantity, 'FAIL', 'OK') AS over_issuance,
       IF(v.issued_quantity = v.issued_rows, 'OK', 'FAIL') AS count_match
FROM (
         SELECT c.coupon_id,
                c.total_quantity,
                c.issued_quantity,
                (SELECT COUNT(*) FROM issued_coupon ic WHERE ic.coupon_id = c.coupon_id) AS issued_rows
         FROM coupon c
         WHERE c.coupon_id = @coupon_id
     ) v;

-- 2. 같은 회원이 2장 이상 받았는지 (1인 1매 위반)
--    UNIQUE(member_id, coupon_id) 가 있어서 정상이라면 결과가 없어야 한다.
SELECT member_id, COUNT(*) AS cnt
FROM issued_coupon
WHERE coupon_id = @coupon_id
GROUP BY member_id
HAVING COUNT(*) > 1;

-- 3. Redis 방식일 때 (터미널에서 실행, 쿠폰 ID 로 변경)
--    redis-cli GET   coupon:{쿠폰ID}:stock     → 남은 재고
--    redis-cli SCARD coupon:{쿠폰ID}:members   → 받은 회원 수
--
--    정상 :  총 수량        = issued_rows + 남은 재고
--            받은 회원 수   = issued_rows
--    다르면 : Redis 는 통과했는데 DB 저장이 실패했고, 되돌리기(cancelIssue)도 실패한 경우