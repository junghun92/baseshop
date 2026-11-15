-- =====================================================================
-- 부하 테스트 데이터 정리
-- =====================================================================
-- 지우는 것
--   이름이 'load test' 로 시작하는 쿠폰(create_coupon.mjs 가 만든 쿠폰)과 그 발급 데이터만.
--   실제 작업 데이터는 건드리지 않는다.
--
-- 순서
--   issued_coupon 이 coupon 을 FK 로 참조하므로 발급 데이터 → 쿠폰 순서로 지운다.
--
-- Redis 키는 따로 지운다 (터미널, 쿠폰 ID 로 변경)
--   redis-cli DEL coupon:{쿠폰ID}:stock coupon:{쿠폰ID}:members
-- =====================================================================

-- 1. 테스트 쿠폰의 발급 데이터
DELETE ic FROM issued_coupon ic
JOIN coupon c ON c.coupon_id = ic.coupon_id
WHERE c.name LIKE 'load test%';

-- 2. 테스트 쿠폰
DELETE FROM coupon WHERE name LIKE 'load test%';

-- 3. 테스트 회원까지 지우려면 (주석 해제)
--    주문, 장바구니 등 테스트 회원이 만든 다른 데이터가 없을 때만 가능 (FK)
DELETE FROM members WHERE username LIKE 'loadtest%';