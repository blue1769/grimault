# 비신용카드 계정의 신용카드 전용 메타정보 검증 (Unexpected Credit Card Metadata)

## 개요

신용카드가 아닌 금융 계정에는 신용카드 전용 메타정보를 지정할 수 없다.

계정 생성 요청에 신용카드 전용 메타정보가 하나라도 포함되어 있다면 검증에 실패해야 한다.

## 테스트 대상

- `CreateAccountRequestValidator.isValid()`

## 테스트 시나리오

### 1. 비신용카드 계정에 신용카드 전용 메타정보가 포함되면 거부한다

- **대상:** `CreateAccountRequestValidator.isValid()`
- **시나리오:** 신용카드가 아닌 계정 생성 요청에 신용카드 전용 메타정보가 포함되어 있으면 검증에 실패한다.
- **사전 조건:**
    - `CreateAccountRequest.name = "테스트 은행 계정"`
    - `type = "ASSET"`
    - `subtype = "BANK"`
    - `issuer = "HYUNDAI"`
    - 그 외 신용카드 전용 메타정보는 모두 `null`이다.
    - `ConstraintValidatorContext`는 Mock으로 구성한다.
- **실행:** `isValid(request, context)` 호출
- **기대 결과:** `isValid()`가 `false`를 반환한다.
- **테스트 형태:** `@Test`