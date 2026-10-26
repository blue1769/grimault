# 계정 수정 시 불완전한 청구 기간 검증 (Incomplete Billing Period Update)

## 개요

신용카드 청구 기간은 시작 월·일과 종료 월·일을 하나의 완전한 정보로 취급한다.

계정 수정 요청에서 청구 기간을 변경하려면 관련 필드 4개를 모두 전달해야 한다. 일부만 전달한 수정 요청은 거부한다.

청구 기간을 전혀 변경하지 않는 요청은 이 규칙의 검증 대상에서 제외한다.

## 테스트 대상

- `UpdateAccountRequestValidator.isValid()`

## 테스트 시나리오

### 1. 청구 기간 일부 필드만 전달하면 거부한다

- **대상:** `UpdateAccountRequestValidator.isValid()`
- **시나리오:** 계정 수정 요청에서 청구 기간의 일부 필드만 지정하면 검증에 실패한다.
- **사전 조건:**
    - `UpdateAccountRequest`의 청구 기간 관련 필드는 기본적으로 모두 `null`이다.
    - 각 입력 케이스에서 지정한 필드만 값을 설정한다.
    - 나머지 수정 필드는 `null`을 유지한다.
    - `ConstraintValidatorContext`는 Mock으로 구성한다.
- **입력 케이스:**
    1. 시작 월만 전달
        - `startMonthOffset = -1`
    2. 시작 월·일만 전달
        - `startMonthOffset = -1`
        - `startDay = 1`
    3. 종료일만 누락
        - `startMonthOffset = -1`
        - `startDay = 1`
        - `endMonthOffset = 0`
- **실행:** 각 입력 케이스에 대해 `isValid(request, context)` 호출
- **기대 결과:**
    - 모든 입력 케이스에서 `isValid()`가 `false`를 반환한다.
    - 세 입력 케이스를 각각 독립적으로 실행 및 검증한다.
- **테스트 형태:** `@ParameterizedTest`