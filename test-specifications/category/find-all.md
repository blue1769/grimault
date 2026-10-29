# 전체 분류 계층 조회 (Category Tree Retrieval)

## 개요

전체 분류 계층 조회 시 전달된 분류 유형 필터의 유효성을 검증한다.

유효하지 않은 분류 유형이 전달되면 조회를 진행하지 않고 비즈니스 예외를 발생시켜야 한다.

## 테스트 대상

- `CategoryService.findAll()`

## 테스트 시나리오

### 1. 유효하지 않은 분류 유형은 조회할 수 없다

- **대상:** `CategoryService.findAll()`
- **시나리오:** 존재하지 않는 분류 유형이 조회 필터에 전달되면 요청을 거부한다.
- **사전 조건:** `typeName = "INVALID_TYPE"`
- **실행:** `findAll(typeName = "INVALID_TYPE")` 호출
- **기대 결과:**
    - `GrimaultException` 발생
    - 예외의 `errorCode`가 `ErrorCode.INVALID_CATEGORY_TYPE`과 일치
    - `CategoryTreeQueryRepository.findAll()`은 호출되지 않음
- **테스트 형태:** `@Test`