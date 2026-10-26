# 계정 대분류·소분류 조합 검증 (Account Type Dependencies)

## 개요

금융 계정 생성 시 계정 대분류(`type`)와 소분류(`subtype`)는 정의된 조합 규칙을 따라야 한다.

유효한 계정 종류가 각각 전달되더라도 대분류와 소분류의 조합이 허용되지 않으면 요청을 거부한다.

## 테스트 대상

- `CreateAccountRequestValidator.isValid()`

## 테스트 시나리오

### 1. 계정 대분류와 소분류의 조합이 유효하지 않으면 거부한다

- **대상:** `CreateAccountRequestValidator.isValid()`
- **시나리오:** 유효한 대분류와 소분류가 서로 허용되지 않는 조합으로 전달되면 검증에 실패한다.
- **사전 조건:**
    - `CreateAccountRequest.name = "테스트 계정"`
    - `type = "ASSET"`
    - `subtype = "LOAN"`
    - 신용카드 전용 메타정보는 모두 `null`이다.
    - `ConstraintValidatorContext`는 Mock으로 구성한다.
- **실행:** `isValid(request, context)` 호출
- **기대 결과:**
    - `isValid()`가 `false`를 반환한다.
    - 실제 계정 생성 및 Repository 호출은 발생하지 않는다.
- **테스트 형태:** `@Test`