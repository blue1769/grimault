# 신용카드 청구 기간 순서 검증 (Credit Card Billing Period Order)

## 개요

신용카드 계정 생성 시 청구 기간의 시작일은 종료일보다 앞서야 한다.

시작일이 종료일과 같거나 더 늦으면 계정 생성 요청을 거부한다.

## 테스트 대상

- `CreateAccountRequestValidator.isValid()`

## 테스트 시나리오

### 1. 신용카드 청구 기간의 시작일이 종료일보다 빠르지 않으면 거부한다

- **대상:** `CreateAccountRequestValidator.isValid()`
- **시나리오:** 신용카드 청구 기간의 시작일이 종료일과 같거나 더 늦으면 검증에 실패한다.
- **사전 조건:**
    - `CreateAccountRequest.name = "테스트 신용카드"`
    - `type = "LIABILITY"`
    - `subtype = "CREDIT_CARD"`
    - `issuer = "HYUNDAI"`
    - `settlementDay = 15`
    - `ConstraintValidatorContext`는 Mock으로 구성한다.
- **입력 케이스:**
    1. 시작일과 종료일이 동일함
        - `startMonthOffset = 0`, `startDay = 15`
        - `endMonthOffset = 0`, `endDay = 15`
    2. 같은 월에서 시작일이 종료일보다 늦음
        - `startMonthOffset = 0`, `startDay = 20`
        - `endMonthOffset = 0`, `endDay = 15`
    3. 시작 월이 종료 월보다 늦음
        - `startMonthOffset = 0`, `startDay = 1`
        - `endMonthOffset = -1`, `endDay = 28`
- **실행:** 각 입력 케이스에 대해 `isValid(request, context)` 호출
- **기대 결과:**
    - 모든 입력 케이스에서 `isValid()`가 `false`를 반환한다.
    - 세 입력 케이스를 독립적으로 실행 및 검증한다.
- **테스트 형태:** `@ParameterizedTest`