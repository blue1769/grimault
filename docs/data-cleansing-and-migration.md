# 데이터 클렌징 및 마이그레이션 실행 전략

## 마이그레이션 파이프라인 아키텍처 (Medallion Pattern)
### 3단계 파이프라인 구조

> [!IMPORTANT]
> Raw 🥉`Bronze` ➔ Stage 🥈`Silver` ➔ Public 🥇`Gold`

### 계층별 역할 및 책임 (Separation of Concerns)
- `raw` 🥉 (Bronze, 원천 데이터 보존)
  - 네이버 가계부 원본 덤프 보존 (Text-only)
  - 제약조건 없이 원본 형태를 100% 보존하여 데이터 유실 없는 감사 로그(Audit Trail) 역할 수행
- `stage` 🥈 (Silver, 기술적 구문 정제 및 노이즈 원천 차단)
  - 타입 캐스팅: 한글 날짜, 금액 문자열, 빈 문자열 등 기술적 정제
  - 노이즈 필터링 및 원본 데이터의 예외·오기입 보정
  - Curated 데이터를 기반으로 거래 유형(`transaction_type`) 확정
  - Gold 적재 제외 대상 명시 및 사전 정합성 검증
- `public` 🥇 (Gold, 코어 복식부기 원장)
  - Silver에서 확정된 거래 유형을 기준으로 마스터 데이터 및 거래 헤더 생성
  - 계정·카테고리 매핑과 복식부기 분개 생성
  - 완성된 원장을 기준으로 계정별 `opening_date` 확정
  - 거래별 Zero-Sum 불변식 유지

### 아키텍처 도입 이점
- 괴물 쿼리 방지: 기술 정제·거래 유형 확정·원장 적재의 책임을 분리하여 각 단계의 복잡도를 낮추고 가독성 및 유지보수성 확보
- 단계별 검증 가능성과 트랜잭션 기반 원자성 확보: 마이그레이션 검증 실패 시 `public` 원장 오염을 사전에 방어, `stage` 단위 내 안전한 디버깅 환경 확보
- 최종 릴리즈(Cut-off) 파이프라인 표준화: 릴리즈 직전 증분 이관 시 일관된 파이프라인 유지 가용성 확보

## 원본 데이터 특이사항 및 예외 보정 규약 (Policies)
### 0원 거래 제외 및 할부/선결제 이력 수용 정책
1. 대상 데이터
- `raw.outgo` 내 현금/카드 결제액이 모두 0원인 거래 총 35건
- Condition
  ```sql
  where replace(cash_amount, ',', '')::bigint = 0
    and replace(card_amount, ',', '')::bigint = 0
  ```

2. 형태별 데이터 사유: 레거시 단식부기 방어흔
- 교통카드 0원 환승 기록: 금액 변동 없이 이동 동선 기록을 위해 남겨둔 교통비 내역
- 일시불 ➔ 할부 전환 원거래 무력화: 카드사 분할납부 신청 후 원거래 이중 지출 집계 방어를 위한 수기 수정
- 할부 중도 완납(선결제) 잔여 회차 무력화: 할부 이용 중 계좌 선결제로 전액 상환 후 잔여 회차 예정 행을 0원으로 수기 수정
- 결제 취소 및 예약금 환불: 승인 취소 또는 반환된 거래의 감사 목적 메모 보존

3. 마이그레이션 보정 규약
- 회계적 실체(자산/부채 변동)가 없으므로 `ledger_entry` 및 `transaction` 생성 대상에서 전면 배제
- 단, 기존 분할 청구 및 선결제 행은 결산 통계의 연속성을 위해 소급 병합 등을 하지 않고 일체 정상 지출 거래로 수용
- 중도 선결제 출금 건 역시 은행 계정에서 실제 금액 지출로 정합성을 맞추어온 바, 원본 그대로 이관

4. 정합성 검증 결과: 이상 없음
- 0원 더미 행 35건을 제외하더라도 실제 구매 비용 및 통장 출금액, 카드 청구액이 오차 없이 100% 일치

## 단계별 마이그레이션 실행 절차 (Runbook)
### Step 1: 중간 스테이징 데이터 정제 (Bronze ➔ Silver)
1. 스테이징 테이블 생성 및 필드 데이터 타입 고정 (`stage.outgo`, `stage.income`)

> [!IMPORTANT]
> 기술적 정제를 완료한 데이터를 기반으로 Curated 테이블을 구성하고, 원본 데이터의 예외 및 오기입 사항을 보정하여 거래 유형 확정에 필요한 데이터를 준비한다.

- 날짜: 한글 표기를 표준 DATE 타입으로 변환
- 금액: 문자열 콤마 제거 후 정수형 BIGINT 타입으로 변환
- 기타: 빈 문자열을 NULL로 통일

2. 원본 데이터 특이사항 반영
- 회계적 실체가 없는 0원 거래 35건 배제
- 기타 출금 수단 기재 누락 1건 배제

### Step 2: 거래 유형 확정 (Silver Enrichment)

> [!IMPORTANT]
> Silver Enrichment는 원천 거래의 비즈니스 유형을 확정하는 단일 판단 지점(SSOT)으로, Gold 적재 과정에서 거래 유형을 재판별하지 않도록 한다.

| **원천** | **거래 유형** | **판별 기준** |
| --- | --- | --- |
| Outgo | `SPENDING` | 일반 지출 |
| Outgo | `TRANSFER` | 이체/대체, 카드대금 |
| Outgo | `REFUND` | 음수 지출 |
| Income | `INCOME_RECEIPT` | 일반 수입 |
| Income | `REFUND` | 환불 대상 수입 |
| Income | `OPENING_BALANCE` | 기초 잔액 |

- 중복 입력 Outgo 1건과 선불 충전 중복 Income 17건은 `transaction_type`을 `NULL`로 설정하여 Gold 적재에서 제외
- 거래 유형 판별은 [02-enrich-silver.sql](../sql/migration/from-naver/02-enrich-silver.sql)에서 완료
- Gold에서는 원천 카테고리나 예외 ID를 이용한 거래 유형 재판별을 수행하지 않음

### Step 3: 코어 복식부기 원장 마이그레이션 (stage ➔ public)

1. 마스터 시드 생성
- 금융 계정 및 분류 마스터 적재
- Account `opening_date` 초기값 `1970-01-01` 적용
- 기초자본 `EQUITY/CAPITAL` 계정 확보

2. 거래 및 분개 생성
- Silver의 거래 유형에 따라 Transaction 생성
- 유형별 계정·분류 매핑 및 LedgerEntry 생성
- 기존 [분개 전략 문서](journal-entry-rules.md)의 회계 규칙 적용

3. Account Opening Date 확정
- 계정별 최초 분개 거래일 MIN(`transaction_date`) 적용
- 무거래 계정 `CURRENT_DATE` 적용
- `EQUITY/CAPITAL` 계정은 `1970-01-01` 유지

### Step 4: 무결성 검증 및 밸런스 점검
1. Zero-Sum 불변식 검증
- 거래별 분개 합계가 각각 `0.00`인지 검증
  ```sql
  SELECT transaction_id,
         count(*) AS entry_count,
         sum(amount) AS amount_sum
  FROM public.ledger_entry
  GROUP BY transaction_id
  HAVING count(*) <> 2
  OR sum(amount) IS DISTINCT FROM 0;
  ```

2. 종전 Gold 기반 동등성 검증 이력
> [!NOTE]
> Ledger API 설계 도중 확정한 스키마 변경 후 기존 Gold 데이터와 Migration Script 내 계산 로직 비교 검증 내역
- 거래 유형: Silver와 기존 Gold의 `source + raw_id` 기준 전수 일치
- Transaction: 19,931건 대조, 불일치 0건
- LedgerEntry: 39,862건 대조, 불일치 0건
- `opening_date`: 기존 Gold와 계산 결과 불일치 0건
- 거래별 0-Sum 및 분개 수 위반 0건

3. 계정 단위 잔액 대조
- 각 계정별 최종 잔액이 원본 데이터 총 잔액과 일치하는지 대조
  ```sql
  SELECT sum(amount) FROM public.ledger_entry WHERE account_id = {id};
  ```

## 릴리즈 시점 데이터 컷오프 전략 (Delta Migration)

### 개발 및 검증 기간
- 네이버 가계부에서 원본 데이터를 추출하여 스키마 및 마이그레이션 절차 검증
- 개발 기간에는 기존 네이버 가계부를 계속 사용하여 거래 내역 기록

### 최종 릴리즈 및 전환
- 릴리즈 직전 네이버 가계부에서 최신 원본 데이터 재추출
- 기존 추출 데이터와 비교하여 신규 거래 내역 식별
- 신규 거래 내역에 검증된 마이그레이션 절차 적용
- 데이터 정합성 검증 완료 후 Grimault로 기록 환경 전환