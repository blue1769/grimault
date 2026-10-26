# 신용카드 상세정보 부분 수정 검증 (Partial Credit Card Details Update)

## 개요

신용카드 계정의 결제일과 청구 기간은 독립적으로 수정할 수 있다.

한쪽 정보만 수정할 때 요청에서 생략된 다른 정보는 기존 값을 유지해야 한다.

## 테스트 대상

- `Account.updateCreditCardDetails()`

## 테스트 시나리오

### 1. 결제일만 수정하면 기존 청구 기간을 유지한다

- **대상:** `Account.updateCreditCardDetails()`
- **시나리오:** 결제일만 변경하고 청구 기간을 생략하면 기존 청구 기간이 유지된다.
- **사전 조건:**
    - 신용카드 계정이 존재한다.
    - `type = LIABILITY`, `subtype = CREDIT_CARD`
    - 초기 신용카드 메타정보:
        - `issuer = SHINHAN`
        - `settlementDay = 15`
        - `billingPeriod = (-1, 1) ~ (0, 15)`
- **실행:** `updateCreditCardDetails(settlementDay = 20, billingPeriod = null)` 호출
- **기대 결과:**
    - `settlementDay = 20`으로 변경된다.
    - 기존 `billingPeriod`는 유지된다.
    - `issuer = SHINHAN`이 유지된다.
- **테스트 형태:** `@Test`

### 2. 청구 기간만 수정하면 기존 결제일을 유지한다

- **대상:** `Account.updateCreditCardDetails()`
- **시나리오:** 청구 기간만 변경하고 결제일을 생략하면 기존 결제일이 유지된다.
- **사전 조건:**
    - 신용카드 계정이 존재한다.
    - `type = LIABILITY`, `subtype = CREDIT_CARD`
    - 초기 신용카드 메타정보:
        - `issuer = SHINHAN`
        - `settlementDay = 15`
        - `billingPeriod = (-1, 1) ~ (0, 15)`
- **실행:** `updateCreditCardDetails(settlementDay = null, billingPeriod = (-1, 5) ~ (0, 20))` 호출
- **기대 결과:**
    - `billingPeriod = (-1, 5) ~ (0, 20)`으로 변경된다.
    - 기존 `settlementDay = 15`가 유지된다.
    - `issuer = SHINHAN`이 유지된다.
- **테스트 형태:** `@Test`