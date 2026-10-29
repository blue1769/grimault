# 단일 소분류 조회 (Category Item Retrieval)

## 개요

단일 소분류 조회 시 요청한 ID에 해당하는 소분류의 존재 여부를 검증한다.

존재하지 않는 소분류 ID가 전달되면 비즈니스 예외를 발생시켜야 한다.

## 테스트 대상

- `CategoryItemService.findById()`

## 테스트 시나리오

### 1. 존재하지 않는 소분류는 조회할 수 없다

- **대상:** `CategoryItemService.findById()`
- **시나리오:** 존재하지 않는 소분류 ID로 조회를 요청하면 예외를 발생시킨다.
- **사전 조건:**
    - `id = 999`
    - `CategoryItemRepository`는 해당 ID 조회 시 결과가 없음을 반환한다.
- **실행:** `findById(id = 999)` 호출
- **기대 결과:**
    - `GrimaultException` 발생
    - 예외의 `errorCode`가 `ErrorCode.CATEGORY_ITEM_NOT_FOUND`와 일치
- **테스트 형태:** `@Test`