# 신규 거래 등록 시 금융 계정 검증 (Invalid Transaction Account)

## 개요

신규 거래는 존재하는 활성 금융 계정만 참조할 수 있으며,
거래 유형별 금융 계정 사용 조건과 이체 계정 조합을 충족해야 한다.

## 테스트 대상

- `TransactionService.create()`

## 테스트 시나리오

### 1. 존재하지 않거나 비활성 상태인 금융 계정의 참조를 거부한다

- **대상:** `TransactionService.create()`
- **시나리오:** 신규 거래에서 존재하지 않거나 비활성 상태인
  금융 계정을 참조하면 거래 등록에 실패한다.
- **사전 조건:**
    - SPENDING 요청을 구성한다.
    - 금융 계정 참조 조건을 제외한 요청값은 유효하다.
    - Repository는 Mock으로 구성한다.
- **실행:** `create(request)` 호출
- **기대 결과:**
    - `INVALID_TRANSACTION_ACCOUNT`에 해당하는
      `GrimaultException`이 발생한다.
    - `TransactionRepository.save()`는 호출되지 않는다.
- **테스트 형태:** `@ParameterizedTest`

#### 입력 조건

| 금융 계정 상태 | 기대 결과 |
|---|---|
| 존재하지 않는 금융 계정 | 거래 등록 거부 |
| 비활성 BANK 계정 | 거래 등록 거부 |

### 2. 거래 유형에서 허용하지 않는 금융 계정 subtype의 사용을 거부한다

- **대상:** `TransactionService.create()`
- **시나리오:** 거래 유형에서 허용하지 않는 subtype의 금융 계정을
  사용하면 거래 등록에 실패한다.
- **사전 조건:**
    - 참조 금융 계정은 존재하며 활성 상태다.
    - 거래일은 금융 계정의 원장 시작일 이상이다.
    - 참조 손익 분류는 해당 거래 유형에서 사용할 수 있다.
    - Repository는 Mock으로 구성한다.
- **실행:** `create(request)` 호출
- **기대 결과:**
    - `INVALID_TRANSACTION_ACCOUNT`에 해당하는
      `GrimaultException`이 발생한다.
    - `TransactionRepository.save()`는 호출되지 않는다.
- **테스트 형태:** `@ParameterizedTest`

#### 입력 조건

| 거래 유형 | 금융 계정 subtype | 기대 결과 |
|---|---|---|
| SPENDING | LOAN | 거래 등록 거부 |
| INCOME_RECEIPT | CREDIT_CARD | 거래 등록 거부 |
| INCOME_RECEIPT | LOAN | 거래 등록 거부 |
| REFUND | LOAN | 거래 등록 거부 |

### 3. 동일한 금융 계정 간 이체를 거부한다

- **대상:** `TransactionService.create()`
- **시나리오:** 이체의 원계정과 목적계정이 동일하면 등록에 실패한다.
- **사전 조건:**
    - TRANSFER 요청의 sourceAccountId와 targetAccountId가 동일하다.
    - Repository는 Mock으로 구성한다.
- **실행:** `create(request)` 호출
- **기대 결과:**
    - `INVALID_TRANSACTION_ACCOUNT`에 해당하는
      `GrimaultException`이 발생한다.
    - `TransactionRepository.save()`는 호출되지 않는다.
- **테스트 형태:** `@Test`

### 4. 허용하지 않는 금융 계정 조합의 이체를 거부한다

- **대상:** `TransactionService.create()`
- **시나리오:** 이체에 EQUITY 계정이 참여하거나 두 부채 계정 간
  이체가 요청되면 거래 등록에 실패한다.
- **사전 조건:**
    - 원계정과 목적계정은 서로 다른 계정이다.
    - 두 계정은 존재하며 활성 상태다.
    - 거래일은 두 계정의 원장 시작일 이상이다.
    - Repository는 Mock으로 구성한다.
- **실행:** `create(request)` 호출
- **기대 결과:**
    - `INVALID_TRANSACTION_ACCOUNT`에 해당하는
      `GrimaultException`이 발생한다.
    - `TransactionRepository.save()`는 호출되지 않는다.
- **테스트 형태:** `@ParameterizedTest`

#### 입력 조건

| 원계정 | 목적계정 | 기대 결과 |
|---|---|---|
| EQUITY (CAPITAL) | ASSET (BANK) | 거래 등록 거부 |
| ASSET (BANK) | EQUITY (CAPITAL) | 거래 등록 거부 |
| LIABILITY (CREDIT_CARD) | LIABILITY (LOAN) | 거래 등록 거부 |