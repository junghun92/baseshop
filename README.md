# baseshop

Spring Boot + JPA 기반 쇼핑몰 백엔드입니다.
**홈서버에 Docker 환경을 직접 구축해 HTTPS · CI/CD · 모니터링까지 운영**하고 있고, 포트원(PortOne) V2 결제의 **동시성 · 정합성**과 **대용량 데이터 조회 성능**에 집중했습니다.

- 🚀 서비스 : https://baseshop.co.kr
- 📄 API 문서 : https://baseshop.co.kr/swagger-ui/index.html

<br>

## 📈 핵심 성과

| 주제 | 내용 | 결과 |
| --- | --- | --- |
| 대용량 페이징 | 게시글 100만 건, 뒤 페이지 조회를 커버링 인덱스(지연 조인)로 개선 | **9,427ms → 102ms** |
| 대용량 엑셀 | 상품 10만 건 다운로드를 No Offset 청크 + SXSSF + 스트리밍으로 개선 | **힙 1.5GiB → 142MiB** |
| 결제 정합성 | 결제완료 · 웹훅 · 만료 배치가 같은 동기화 규칙, 비관적 락 + 상태 전이 규칙 | 중복 처리 · 재고 이중 복구 차단 |
| 선착순 쿠폰 | 비관적 락 → Redis Lua (재고 · 중복 판단을 Redis에서 원자적으로) | 동일 과부하에서 **처리량 약 1.9배**, 초과 발급 0건 |
| 인프라 · 운영 | 홈서버 Ubuntu + Docker Compose, GitHub Actions 자동 배포, Prometheus · Grafana · Slack 알림 | 외부 공개 포트 80/443만 |

<br>

## 🛠 기술 스택

| 구분 | 기술 |
| --- | --- |
| Backend | Java 17, Spring Boot 4.1, Spring Data JPA, QueryDSL 5, Spring Security, JWT |
| Database | MySQL 8.4, Redis 7 |
| Infra | Ubuntu Server 24.04 (홈서버), Docker Compose, Nginx, GitHub Actions, GHCR |
| Monitoring | Actuator, Micrometer, Prometheus, Grafana, Slack 알림, Cloudflare Tunnel + Access |
| 외부 연동 | PortOne V2 (결제 · 환불 · 웹훅), Cloudflare R2 (상품 이미지) |
| Frontend | Vue 3 + Vite (연동 확인용) |

<br>

## 🏗 아키텍처

```mermaid
flowchart LR
    U["사용자"] -->|"HTTPS"| CF["Cloudflare DNS"]
    PO["포트원 웹훅"] -->|"HTTPS"| CF
    CF --> N
    ADM["관리자"] -->|"HTTPS"| ACC["Cloudflare Access<br>허용 계정만"]
    ACC --> TUN["Cloudflare Tunnel"]
    subgraph S["홈서버 (Ubuntu 24.04, UFW: 80/443/SSH)"]
        subgraph D["Docker Compose 내부 네트워크"]
            N["Nginx<br>HTTPS · 정적 파일 · /api 프록시"]
            SP["Spring Boot"]
            DB[("MySQL")]
            RD[("Redis<br>Refresh Token · 쿠폰 재고 · 쿠폰 캐시 · 조회수")]
            PR["Prometheus"]
            GF["Grafana"]
            CFD["cloudflared"]
        end
    end
    N -->|"/api/**"| SP
    SP --> DB
    SP --> RD
    PR -->|"15초마다 수집"| SP
    GF --> PR
    TUN --> CFD --> GF
    GF -->|"서버 다운 알림"| SL["Slack"]
    SP -->|"결제 조회 · 취소"| PG["PortOne"]
    SP -->|"이미지"| R2["Cloudflare R2"]
```

- 외부에 열린 포트는 **Nginx 80/443뿐**이고, Spring · MySQL · Redis · 모니터링 도구는 내부 네트워크에서만 통신합니다.
- Grafana는 포트를 열지 않고 **Cloudflare Tunnel + Access**로 허용된 계정만 접근합니다.
- **CI/CD** : `git push` → GitHub Actions에서 이미지 빌드 → GHCR → SSH로 서버에서 `compose pull → up -d`. 비밀 설정은 GitHub Secret으로 배포 때 주입합니다.
- 인증서(Let's Encrypt)와 유동 IP(DDNS)도 컨테이너로 자동 갱신합니다.

<details>
<summary><b>ERD</b></summary>

<img width="1250" height="851" alt="캡처" src="https://github.com/user-attachments/assets/54ee836e-9ea3-4be2-aa3b-53946d4379b6" />


</details>

<br>

## ✨ 주요 기능

| 도메인 | 기능 | 포인트 |
| --- | --- | --- |
| 회원 · 인증 | 가입, 로그인, 로그아웃, 재발급 | Access / Refresh 분리, Refresh는 httpOnly 쿠키 + Redis(TTL) |
| 게시판 | CRUD, 페이징 · 검색, 조회수, 주간 인기글 | 작성자 본인만 수정 · 삭제, 100만 건 페이징 최적화, 조회수 Redis + 비동기 DB 백업 |
| 상품 | 관리(관리자), 조회, 이미지, 엑셀 다운로드 | 소프트 삭제, 이미지는 R2, 엑셀은 메모리 일정 |
| 장바구니 · 주문 | 담기, 주문 생성 · 취소, 쿠폰 적용 | 재고 선차감(비관적 락), 취소 · 실패 시 환불 + 재고 · 쿠폰 복구 |
| 결제 | 결제완료, 웹훅, 만료 배치 | 포트원 재조회로 검증, 금액 불일치 자동 환불 |
| 쿠폰 | 관리(관리자), 쿠폰 목록, 선착순 발급, 내 쿠폰함 | 1인 1매, Redis Lua 발급, 쿠폰 정보 캐시, 발급 방식 교체 가능(전략 패턴) |

<details>
<summary><b>API 목록</b></summary>

| Method | URI | 설명 | 권한 |
| --- | --- | --- | --- |
| POST | `/api/v1/member` | 회원가입 | 누구나 |
| POST | `/api/v1/login` · `/logout` · `/reissue` | 로그인 · 로그아웃 · 재발급 | 누구나 |
| GET | `/api/v1/board`, `/api/v1/board/{id}` | 게시글 목록 · 상세 | 누구나 |
| POST · PUT · DELETE | `/api/v1/board`, `/api/v1/board/{id}` | 게시글 작성 · 수정 · 삭제 | USER, ADMIN |
| GET | `/api/v1/board/rank` | 인기 게시글 (최근 7일 조회수 상위 5개) | 누구나 |
| GET | `/api/v1/product`, `/api/v1/product/{id}` | 상품 목록 · 상세 | 누구나 |
| POST · PUT · DELETE | `/api/v1/product`, `/api/v1/product/{id}` | 상품 등록 · 수정 · 삭제 | ADMIN |
| PUT | `/api/v1/product/{id}/quantity` | 재고 증감 | ADMIN |
| POST | `/api/v1/product/{id}/file` | 상품 이미지 업로드 | ADMIN |
| GET | `/api/v1/product/excel` | 상품 전체 엑셀 다운로드 | ADMIN |
| GET · POST · PUT · DELETE | `/api/v1/cart` | 장바구니 | USER, ADMIN |
| POST | `/api/v1/order/sheet`, `/api/v1/order` | 주문서, 주문 생성 | USER, ADMIN |
| GET | `/api/v1/order` | 주문 목록 | USER, ADMIN |
| PUT | `/api/v1/order/{id}/cancel` | 주문 취소 | USER, ADMIN |
| POST | `/api/v1/payment/complete` | 결제완료 검증 | USER, ADMIN |
| POST | `/api/v1/payment/webhook` | 포트원 웹훅 | 서명 검증 |
| GET · POST · PUT | `/api/v1/admin/coupon`, `/api/v1/admin/coupon/{id}` | 쿠폰 목록 · 등록 · 수정 · 중지 | ADMIN |
| GET | `/api/v1/coupon` | 쿠폰 목록 (발급 가능 여부 포함) | USER, ADMIN |
| POST | `/api/v1/coupon/{id}/issue` | 쿠폰 발급 | USER, ADMIN |
| GET | `/api/v1/issued-coupon` | 내 쿠폰함 | USER, ADMIN |

</details>

<br>

## ⚡ 성능 개선

### 1. 게시판 페이징 : 커버링 인덱스 (9,427ms → 102ms)

**문제** : 게시글 100만 건에서 뒤 페이지(`LIMIT 300000, 10`) 조회가 9초 이상
- `board_type` 인덱스가 없어 테이블을 90만 행 읽으며 필터링
- offset만큼 건너뛸 행까지 전부 테이블에서 읽고 버림

**해결**
1. `board_type` 인덱스 추가 (보조 인덱스에 PK가 포함돼 정렬까지 인덱스로 처리)
2. 인덱스만으로 id 10개 조회 → 그 id로 본 데이터 조회 (지연 조인)
   - QueryDSL은 FROM 서브쿼리를 지원하지 않아 쿼리 2번으로 구현, 반환 타입은 유지해서 서비스 · 프론트 변경 없음

| | 테이블 접근 | 실행 시간 |
| --- | --- | --- |
| 개선 전 | 90만 회 | 9,427ms |
| 개선 후 | **10회** | **102ms (약 92배)** |

<details>
<summary>측정 환경 · EXPLAIN ANALYZE</summary>

- 게시글 100만 건 (QNA 약 33만 건), 테이블 884MB / 인덱스 23MB, InnoDB 버퍼 풀 128MB
- `EXPLAIN ANALYZE`, 3회 실행 후 첫 회 제외

**개선 전** — 9,427ms
```
-> Limit/Offset: 10/300000 row(s)  (actual time=9427..9427 rows=10 loops=1)
    -> Filter: (b1_0.board_type = 'QNA')  (actual time=0.0573..9405 rows=300010 loops=1)
        -> Index scan on b1_0 using PRIMARY (reverse)  (actual time=0.052..9308 rows=900030 loops=1)
```

**개선 후** — 102ms
```
-> Nested loop inner join  (actual time=102..102 rows=10 loops=1)
    -> Table scan on t  (actual time=98.5..98.5 rows=10 loops=1)
        -> Materialize  (actual time=98.4..98.4 rows=10 loops=1)
            -> Limit/Offset: 10/300000 row(s)  (actual time=98.1..98.1 rows=10 loops=1)
                -> Covering index lookup on b1_0 using idx_board_board_type (board_type = 'QNA') (reverse)  (actual time=0.147..87.5 rows=300010 loops=1)
    -> Single-row index lookup on b using PRIMARY (board_id = t.board_id)  (actual time=0.00429..0.00432 rows=1 loops=10)
```
</details>

### 2. 상품 엑셀 다운로드 : 메모리 개선 (힙 1.5GiB → 142MiB)

**문제** : `findAll()` + `XSSFWorkbook`으로 상품 10만 건과 엑셀 셀 70만 개를 전부 힙에 올림 → 동시 요청 시 OOM 위험

**해결**
1. **No Offset 청크 조회** : `where id > 마지막id limit 1000`으로 1,000건씩, 필요한 컬럼만 DTO로
2. **SXSSF** : 메모리에는 최근 100행만, 나머지는 임시 파일
3. **StreamingResponseBody** : 출력 스트림에 바로 전송 (`byte[]`로 힙에 올리지 않음). 생성은 응답 전에 끝내서 오류는 정상 JSON 에러로 응답
4. **공통화** : `@ExcelColumn` 어노테이션 + 리플렉션 → 다른 도메인도 DTO만 만들면 재사용

| | 응답 시간 | Heap used 최대 |
| --- | --- | --- |
| 개선 전 | 23.85초 | 약 1.5GiB |
| 개선 후 | **5.50초 (77%↓)** | **142MiB (91%↓)** |

<table>
  <tr>
    <td align="center"><img width="366" height="247" alt="엑셀 다운로드(개선전) heap 메모리 사용량" src="https://github.com/user-attachments/assets/d175183a-d490-46c4-8b95-8614be9bb34a" /><br><b>개선 전</b></td>
    <td align="center"><img width="361" height="255" alt="엑셀 다운로드(개선후) heap 메모리 사용량" src="https://github.com/user-attachments/assets/4a71e761-3de4-4b76-8396-8fdd73e53b36" /><br><b>개선 후</b></td>
  </tr>
</table>

<details>
<summary>측정 환경</summary>

- 상품 10만 건 (설명 약 500자, 컬럼 7개), 로컬, JVM 힙 기본값, SQL 로그 OFF
- 시간 : 브라우저 Network → Waiting for server response / 메모리 : Grafana JVM Heap `used` 최대값 (측정마다 서버 재시작)
</details>

<br>

## 🎟 선착순 쿠폰 발급

**문제** : 오픈 순간 수천 명이 동시에 요청 → 재고보다 많이 발급되거나, 한 사람이 여러 장 받을 수 있음

```mermaid
sequenceDiagram
    participant U as 사용자
    participant S as 서버
    participant R as Redis
    participant D as MySQL
    U->>S: 발급 요청
    S->>R:
