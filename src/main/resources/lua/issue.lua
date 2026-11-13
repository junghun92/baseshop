-- 재고 차감 + 사용자 추가 를 한 덩어리에 묶는다.
--
-- KEYS[1] = coupon:{id}:stock   (재고 카운터, 쿠폰 생성,수정 시 totalQuantity 로 초기화)
-- KEYS[2] = coupon:{id}:members   (발급된 사용자 set)
-- ARGV[1] = member_id
-- 반환:
--   1   발급 성공
--   0   매진
--  -1   이미 발급된 사용자

-- 발급된 사용자 체크
if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
  return -1
end

-- 재고 확인
local remaining = tonumber(redis.call('GET', KEYS[1]) or '0')
if remaining <= 0 then
  return 0
end

-- 재고 차감 + 사용자 추가
redis.call('DECR', KEYS[1])
redis.call('SADD', KEYS[2], ARGV[1])
return 1
