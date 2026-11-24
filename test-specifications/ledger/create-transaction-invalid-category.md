# 신규 거래 등록 시 손익 분류 검증 (Invalid Transaction Category)

## 개요

손익 분류가 필요한 신규 거래는 존재하는 선택 가능 소분류를
참조해야 하며, 손익 분류 유형은 거래 유형과 일치해야 한다.

## 테스트 대상

- `TransactionService.create()`

## 테스트 시나리오

### 1. 존재하지 않거나 선택 불가능한 손익 소분류의 참조를 거부한다

- **대상:** `TransactionService.create()`
- **시나리오:** 존재하지 않는 소분류 또는 소분류와 상위 대분류 중
  하나라도 비활성 상태인 손익 분류를 참조하면 거래 등록에 실패한다.
- **사전 조건:**
    - SPENDING 요청을 구성한다.
    - 참조 금융 계정은 유효한 활성 BANK 계정이다.
    - 거래일은 금융 계정의 원장 시작일 이상이다.
    - 소분류가 존재하는 경우 상위 대분류의 유형은 EXPENSE다.
    - Repository는 Mock으로 구성한다.
- **실행:** `create(request)` 호출
- **기대 결과:**
    - `INVALID_TRANSACTION_CATEGORY`에 해당하는
      `GrimaultException`이 발생한다.
    - `TransactionRepository.save()`는 호출되지 않는다.
- **테스트 형태:** `@ParameterizedTest`

#### 입력 조건

| 소분류 존재 여부 | 대분류 활성 여부 | 소분류 활성 여부 | 기대 결과 |
|---|---|---|---|
| false | - | - | 거래 등록 거부 |
| true | true | false | 거래 등록 거부 |
| true | false | true | 거래 등록 거부 |

### 2. 거래 유형과 일치하지 않는 손익 분류의 사용을 거부한다

- **대상:** `TransactionService.create()`
- **시나리오:** 거래 유형에서 허용하지 않는 손익 분류 유형을
  사용하면 거래 등록에 실패한다.
- **사전 조건:**
    - 참조 금융 계정은 해당 거래 유형에서 사용할 수 있다.
    - 참조 금융 계정은 존재하며 활성 상태다.
    - 거래일은 금융 계정의 원장 시작일 이상이다.
    - 참조 소분류는 존재하며 상위 대분류와 자신의 활성 상태가 모두 참이다.
    - Repository는 Mock으로 구성한다.
- **실행:** `create(request)` 호출
- **기대 결과:**
    - `INVALID_TRANSACTION_CATEGORY`에 해당하는
      `GrimaultException`이 발생한다.
    - `TransactionRepository.save()`는 호출되지 않는다.
- **테스트 형태:** `@ParameterizedTest`

#### 입력 조건

| 거래 유형 | 손익 분류 유형 | 기대 결과 |
|---|---|---|
| SPENDING | INCOME | 거래 등록 거부 |
| INCOME_RECEIPT | EXPENSE | 거래 등록 거부 |
| REFUND | INCOME | 거래 등록 거부 |