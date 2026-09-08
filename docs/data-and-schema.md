# 데이터 추출 및 스키마 설계

## DBMS 및 IDE 선정

> [!IMPORTANT]
> **“PostgreSQL”** & **“DataGrip”**

### PostgreSQL의 강점
1. 원장(Ledger) 모델링 및 계산 쿼리의 성숙도
    - 누적 잔액(Running Balance) 연산 최적화 및 메모리 처리 방식에서의 성숙도와 유연성
    - 계층형 카테고리 트리 구조 탐색 시 재귀 쿼리나 ltree 확장 모듈 직접적 활용성

2. 강력한 준정형 데이터 타입 `JSONB` 의 존재
    - RAW 데이터 적재 후 재가공 시점에 GIN Index를 이용
    - JSON 속성에 대한 쿼리 및 데이터 가공에서의 자유도 확보

3. 데이터 정합성 보장에서의 엄격성
    - CHECK 제약 조건, Partial Index, 조건부 고유 제약 등 설계에서 MySQL 대비 강력

### 프로젝트 부수 목적에 부합
- 기존에 다뤄온 RDBMS의 핵심 원리를 크게 벗어나지 않는 러닝 커브
- 추가적인 RDBMS의 실질적인 경험치 누적의 계기
- NAS 컨테이너를 통한 손쉬운 운영 환경 가용성 확보

### DataGrip

> [!NOTE]
> [DataGrip, 이제 비상업적 용도로 무료 사용 가능](https://blog.jetbrains.com/ko/datagrip/2025/10/04/datagrip-is-now-free-for-non-commercial-use/) (2025년 10월 4일)

## 릴리즈 시점 데이터 컷오프 플로우 (Delta Migration)
1. 개발 및 검증 기간
    - 네이버 가계부에서 원본 데이터 다운로드
    - 원본 데이터를 이용한 스키마 검증 및 마이그레이션 스크립트 작성 완료
    - 기간 내 9월 가계부 작성은 네이버 가계부에서 수행
2. 최종 릴리즈 직전 (Cut-off 시점)
    - 네이버 가계부에서 원본 데이터 재다운로드
    - 개발 기간에 이미 내려받은 데이터를 필터링(신규 자료에서 9월 이후만 유효)
    - 필터링이 완료된 신규 작성 데이터만 이미 검증한 스크립트로 마이그레이션 수행
    - 이 후 가계부는 신규 서비스에서 작성 시작

## 태스크 목록
- PostgreSQL 컨테이너 구동 (via Synology NAS)
- 네이버 가계부 데이터 다운로드 (전체 약 2만 건, 2012. 12 - 2026. 8)
- 원본 데이터 가져오기 (DataGrip > Import)

    > [!WARNING]
    > Import 전 단계에서 XLS → TSV 후 헤더(제목, 조회기간 및 빈 줄) 전처리 필요 
    > ```bash
    > sed -i '' '1,3d' moneybook_outgo_*.tsv
    > sed -i '' '1,3d' moneybook_income_*.tsv
    > ```

- [RAW 데이터 기반 가계부 사용성 분석](raw-data-reverse-engineering.md)
- [네이버 가계부 주요 기능 분석과 선별 및 명세](requirements-specification.md)
- 신규 가계부 대상 스키마 정규화 및 설계
- RAW 데이터 클렌징 및 마이그레이션 스크립트 작성

## 스키마 명세 ([DDL](../sql/public-schema.sql))
### Account: 금융 계정
| **Column Name** | **Data Type** | **Nullable** | **Description** |
| --- | --- | --- | --- |
| id | integer | NOT NULL | 계정 고유 식별자 (PK) |
| name | character varying | NOT NULL | 계정 고유 명칭 (예: 신한은행, 현대카드 제로, 네이버머니 등) |
| type | character varying | NOT NULL | 회계 대분류 (ASSET: 자산, LIABILITY: 부채) |
| sub_type | character varying | NOT NULL | 실사용 세부 성격 (BANK: 은행통장, CASH: 현금, PREPAID: 선불충전금, CREDIT_CARD: 신용카드) |
| settlement_day | smallint | NULL | 신용카드 결제일 (1~31, 신용카드 전용 메타데이터) |
| is_active | boolean | NOT NULL | 신규 거래 작성 시 계정 활성화 여부 (true: 현재 유효 계정, false: 과거 해지 계정) |
| created_at | timestamp with time zone | NOT NULL | 계정 등록 시스템 일시 |

### Category: 분류 카테고리
| **Column Name** | **Data Type** | **Nullable** | **Description** |
| --- | --- | --- | --- |
| id | integer | NOT NULL | 카테고리 고유 식별자 (PK) |
| type | character varying | NOT NULL | 손익 분류 구분 (EXPENSE: 지출, INCOME: 수입) |
| parent_name | character varying | NOT NULL | 1단계 대분류 명칭 (예: 식비, 주거/통신, 생활용품 등) |
| sub_name | character varying | NOT NULL | 2단계 소분류 명칭 (예: 외식, 통신비, 기타 등) |
| is_active | boolean | NOT NULL | 신규 거래 작성 UI 노출 여부 (true: 기본 노출, false: 기타 등 선택 지양 항목) |

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
| category_id | integer | NULL | 손익 인식 대상 카테고리 식별자 (수입/지출 손익 인식 시 매핑, FK) |
| amount | numeric | NOT NULL | 거래 금액 (부호형: 증가는 양수(+), 감소는 음수(-)) |
| entry_type | character varying | NOT NULL | 분개 속성 구분 (ASSET: 자산, LIABILITY: 부채, EXPENSE: 비용, REVENUE: 수익) |
