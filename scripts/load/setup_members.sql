-- =====================================================================
-- 부하 테스트용 회원 2만 명 생성
-- =====================================================================
-- 왜 필요한가
--   issued_coupon.member_id 가 members 테이블의 FK 라서,
--   쿠폰 발급에 성공한 회원은 실제로 DB 에 있어야 한다.
--   누가 성공할지 미리 알 수 없으므로 요청하는 회원 모두가 있어야 한다.
--
-- 왜 2만 명인가
--   테스트 쿠폰 수량(5,000장)의 4배.
--   "재고보다 훨씬 많은 서로 다른 회원이 동시에 몰리는 상황"을 만들기에 충분하다.
--
-- 만들어지는 회원
--   username : loadtest00001 ~ loadtest20000
--   email    : loadtest00001@loadtest.local ~
--   role     : USER
--   password : loadtest1234  (로컬 테스트 전용, 아래는 BCrypt 해시)
--              create_tokens.mjs 의 PASSWORD 와 같아야 한다.
--
-- 부하 테스트 관리자
--   username : loadadmin  (테스트 쿠폰 생성용, create_coupon.mjs 가 사용)
--   password : loadtest1234
--   role     : ADMIN
--
-- 실행
--   run.sh 가 매번 실행한다. 여러 번 실행해도 안전하다.
--   - 없는 회원만 INSERT (INSERT IGNORE, username / email UNIQUE)
--   - 비밀번호가 다른 테스트 회원은 loadtest1234 로 맞춤 (UPDATE)
--
-- 주의
--   로컬 DB 에서만 실행. 운영 DB 에서는 실행하지 않는다.
-- =====================================================================

-- 1. 테스트 회원 비밀번호 해시 (loadtest1234, BCrypt 강도 4 : 테스트 회원 로그인 속도용)
SET @pw = '$2a$04$RNA4.13LfgHxaVFXW2QHmeO4ZTuZCh1pYVi5nUXpdhmkwsUqklNH.';

-- 2. 재귀 CTE 로 1 ~ 20000 숫자를 만든다.
--    MySQL 재귀 깊이 기본값이 1000 이라서, 2만까지 만들려면 한도를 올려야 한다.
SET SESSION cte_max_recursion_depth = 25000;

-- 3. 숫자마다 회원 한 명씩 INSERT (이미 있으면 건너뜀)
--    reg_id, reg_date 는 BaseEntity 의 NOT NULL 컬럼이라 직접 넣는다.
--    (SQL 로 넣으면 JPA Auditing 이 동작하지 않음)
INSERT IGNORE INTO members (username, password, email, member_role_type, reg_id, reg_date)
WITH RECURSIVE seq (n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 20000
)
SELECT CONCAT('loadtest', LPAD(n, 5, '0')),                       -- loadtest00001
       @pw,
       CONCAT('loadtest', LPAD(n, 5, '0'), '@loadtest.local'),    -- 이메일도 UNIQUE 라 번호를 붙임
       'USER',
       'loadtest',                                                -- 등록자 : 부하 테스트 데이터 표시
       NOW(6)
FROM seq;

-- 4. 이미 있던 테스트 회원의 비밀번호를 loadtest1234 로 맞춤
--    (예전에 다른 비밀번호로 만들어진 테스트 회원이 있을 때)
UPDATE members
SET password = @pw
WHERE username LIKE 'loadtest%'
  AND password <> @pw;

-- 5. 부하 테스트 관리자 (없으면 생성, 있으면 비밀번호 / 권한 맞춤)
--    테스트 쿠폰 생성(POST /api/v1/admin/coupon)에 ADMIN 권한이 필요하다.
--    이름을 loadtest... 로 하지 않은 이유 : LIKE 'loadtest%' 조건에 같이 걸리지 않도록
INSERT INTO members (username, password, email, member_role_type, reg_id, reg_date)
VALUES ('loadadmin', @pw, 'loadadmin@loadtest.local', 'ADMIN', 'loadtest', NOW(6))
ON DUPLICATE KEY UPDATE password = VALUES(password),
                        member_role_type = VALUES(member_role_type);

-- 6. 결과 확인 (20000 이 나오면 정상)
SELECT COUNT(*) AS loadtest_members FROM members WHERE username LIKE 'loadtest%';