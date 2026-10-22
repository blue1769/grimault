# 계정 목록 조회 (Account List)

## 개요

계정 목록 조회 시 전달된 계정 종류 필터의 유효성을 검증한다.

유효하지 않은 계정 종류가 포함된 경우 조회를 진행하지 않고 비즈니스 예외를 발생시켜야 한다.

## 테스트 대상

- `AccountService.findAll()`

## 테스트 시나리오

### 1. 유효하지 않은 계정 종류는 조회할 수 없다

- **대상:** `AccountService.findAll()`
- **시나리오:** 존재하지 않는 계정 종류가 조회 필터에 전달되면 요청을 거부한다.
- **사전 조건:** `kinds = listOf("INVALID_KIND")`
- **실행:** `findAll(kinds = listOf("INVALID_KIND"))` 호출
- **기대 결과:**
    - `GrimaultException` 발생
    - 예외의 `errorCode`가 `ErrorCode.INVALID_ACCOUNT_KIND`와 일치
    - `AccountQueryRepository.findAll()`은 호출되지 않음
- **테스트 형태:** `@Test`