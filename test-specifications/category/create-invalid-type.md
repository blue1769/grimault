# 대분류 생성 시 유효하지 않은 유형 검증 (Category Creation Invalid Type)

## 개요

대분류 생성 시 `type`은 `INCOME` 또는 `EXPENSE` 중 하나여야 한다.

정의되지 않은 분류 유형이 전달되면 생성 요청 검증에 실패해야 한다.

## 테스트 대상

- `CreateCategoryRequestValidator.isValid()`

## 테스트 시나리오

### 1. 유효하지 않은 분류 유형으로 대분류를 생성할 수 없다

- **대상:** `CreateCategoryRequestValidator.isValid()`
- **시나리오:** 정의되지 않은 분류 유형이 전달되면 요청 검증에 실패한다.
- **사전 조건:**
    - `CreateCategoryRequest.name = "테스트 대분류"`
    - `CreateCategoryRequest.type = "INVALID"`
    - `ConstraintValidatorContext`는 Mock으로 구성한다.
- **실행:** `isValid(request, context)` 호출
- **기대 결과:**
    - `isValid()`가 `false`를 반환한다.
    - 실제 대분류 생성 및 Repository 호출은 발생하지 않는다.
- **테스트 형태:** `@Test`