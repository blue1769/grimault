# 신용카드 필수 메타정보 검증 (Required Credit Card Metadata)

## 개요

신용카드 계정을 생성할 때는 카드 발급사, 결제일, 청구 기간 정보가 모두 제공되어야 한다.

필수 메타정보 중 하나라도 누락되면 해당 계정 생성 요청을 거부한다.

## 테스트 대상

- `CreateAccountRequestValidator.isValid()`

## 테스트 시나리오

### 1. 신용카드 필수 메타정보가 하나라도 누락되면 거부한다

- **대상:** `CreateAccountRequestValidator.isValid()`
- **시나리오:** 신용카드 생성 요청에서 필수 메타정보 6개 중 하나라도 누락되면 검증에 실패한다.
- **사전 조건:**
    - `CreateAccountRequest.name = "테스트 신용카드"`
    - `type = "LIABILITY"`
    - `subtype = "CREDIT_CARD"`
    - 기본 메타정보:
        - `issuer = "HYUNDAI"`
        - `settlementDay = 15`
        - `startMonthOffset = -1`
        - `startDay = 1`
        - `endMonthOffset = 0`
        - `endDay = 15`
    - 각 테스트 케이스에서는 필수 필드 중 지정한 하나만 `null`로 변경한다.
    - 나머지 필드는 기본값을 유지한다.
    - `ConstraintValidatorContext`는 Mock으로 구성한다.
- **입력 케이스:**
    1. `issuer = null`
    2. `settlementDay = null`
    3. `startMonthOffset = null`
    4. `startDay = null`
    5. `endMonthOffset = null`
    6. `endDay = null`
- **실행:** 각 입력 케이스에 대해 `isValid(request, context)` 호출
- **기대 결과:**
    - 모든 입력 케이스에서 `isValid()`가 `false`를 반환한다.
    - 여섯 케이스를 각각 독립적으로 실행 및 검증한다.
- **테스트 형태:** `@ParameterizedTest`