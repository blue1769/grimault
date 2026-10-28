# 데이터베이스 스키마 설계

## PostgreSQL 선정 이유

### 금융 원장 데이터 관리 및 분석 확장성
- 윈도 함수를 활용한 누적 잔액(Running Balance) 계산 및 기간별 금융 데이터 집계
- 향후 고급 통계와 데이터 분석을 고려한 인덱싱 및 쿼리 최적화 기능
- 계층형 카테고리 분석을 위한 재귀 쿼리 및 `ltree` 확장 활용 가능성

### 관계형 데이터 정합성
- 트랜잭션의 원자성과 관계형 제약조건을 통한 금융 원장 데이터의 일관성 보장
- `CHECK`, Partial Index, 조건부 고유 제약 등을 활용한 세밀한 데이터 정합성 관리

### JSONB 활용
- 신용카드 전용 메타데이터 등 계정별 부가 속성을 유연하게 관리
- 향후 준정형 데이터 검색 및 분석 시 `GIN Index`를 활용한 쿼리 최적화 가능성

## 스키마 명세 ([DDL](../sql/public-schema.sql))
### Account: 금융 계정
| **Column Name** | **Data Type** | **Nullable** | **Description** |
| --- | --- | --- | --- |
| id | integer | NOT NULL | 계정 고유 식별자 (PK) |
| name | character varying | NOT NULL | 계정 고유 명칭 (예: 신한은행, 현대카드 제로, 네이버머니 등) |
| type | character varying | NOT NULL | 회계 대분류 (ASSET: 자산, LIABILITY: 부채, EQUITY: 기초자본) |
| subtype | character varying | NOT NULL | 실사용 세부 성격 (BANK: 은행통장, CASH: 현금, PREPAID: 선불충전금, CREDIT_CARD: 신용카드, LOAN: 대출/차입금, CAPITAL: 기초자본) |
| credit_card_metadata | jsonb | NULL | 신용카드 전용 메타데이터 (카드사, 결제일, 이용기간 등; CREDIT_CARD 외 계정은 NULL) |
| is_active | boolean | NOT NULL | 신규 거래 작성 시 계정 활성화 여부 (true: 현재 유효 계정, false: 과거 해지 계정) |
| created_at | timestamp with time zone | NOT NULL | 계정 등록 시스템 일시 |

### Category: 분류 대분류
| **Column Name** | **Data Type** | **Nullable** | **Description** |
| --- | --- | --- | --- |
| id | integer | NOT NULL | 대분류 고유 식별자 (PK) |
| type | character varying | NOT NULL | 손익 분류 구분 (EXPENSE: 지출, INCOME: 수입; 생성 후 변경 불가) |
| name | character varying | NOT NULL | 대분류 명칭 (전체 대분류에서 중복 불가) |
| is_active | boolean | NOT NULL | 대분류 활성 상태 (소분류 활성 상태와 독립적으로 관리) |
| sort_order | integer | NOT NULL | 전체 대분류에서 1부터 연속되는 사용자 지정 순번 (비활성 포함) |

### Category Item: 분류 소분류
| **Column Name** | **Data Type** | **Nullable** | **Description** |
| --- | --- | --- | --- |
| id | integer | NOT NULL | 소분류 고유 식별자 (PK; 원장 참조 대상) |
| category_id | integer | NOT NULL | 소속 대분류 식별자 (FK) |
| name | character varying | NOT NULL | 소분류 명칭 (동일 대분류 안에서 중복 불가) |
| is_active | boolean | NOT NULL | 소분류 활성 상태 (실제 선택 가능 여부는 대분류 활성 상태와 함께 판단) |
| sort_order | integer | NOT NULL | 소속 대분류 내에서 1부터 연속되는 사용자 지정 순번 (비활성 포함) |
| is_favorite | boolean | NOT NULL | 자주 쓰는 분류 지정 여부 (활성 상태와 독립적으로 관리) |

### Transaction: 거래 원천 헤더
| **Column Name** | **Data Type** | **Nullable** | **Description** |
| --- | --- | --- | --- |
| id | bigint | NOT NULL | 거래 고유 식별자 (PK) |
| transaction_date | date | NOT NULL | 실제 소비/수입이 발생한 비즈니스 일자 (YYYY-MM-DD) |
| merchant | character varying | NOT NULL | 결제 가맹점 및 거래처 상호명 (예: 스타벅스, SKT, 쿠팡 등) |
| description | character varying | NOT NULL | 상세 구매 품목 및 사용 내역 적요 |
| payment_method | character varying | NULL | 결제 수단 식별 메타데이터 (체크카드명, 현금 등 세부 수단) |
| tags | text | NULL | 모임/인명/맥락 기록용 검색 태그 (쉼표 구분 자유 텍스트) |
| is_waste | boolean | NOT NULL | 낭비성 지출 여부 플래그 (true: 낭비, false: 일반) |
| created_at | timestamp with time zone | NOT NULL | 데이터베이스 최초 등록 시스템 감사 일시 |

### Ledger Entry: 분개 원장 라인
| **Column Name** | **Data Type** | **Nullable** | **Description** |
| --- | --- | --- | --- |
| id | bigint | NOT NULL | 분개 라인 고유 식별자 (PK) |
| transaction_id | bigint | NOT NULL | 연계 거래 헤더 식별자 (FK, ON DELETE CASCADE) |
| account_id | integer | NULL | 변동 대상 금융 계정 식별자 (자산/부채 변동 시 필수, FK) |
| category_item_id | integer | NULL | 손익 인식 대상 소분류 식별자 (수입/지출 손익 인식 시 매핑, FK) |
| amount | numeric | NOT NULL | 거래 금액 (부호형: 증가는 양수(+), 감소는 음수(-)) |
| entry_type | character varying | NOT NULL | 분개 속성 구분 (ASSET: 자산, LIABILITY: 부채, EXPENSE: 비용, REVENUE: 수익, EQUITY: 기초자본) |
