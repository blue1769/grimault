# 소분류 생성 시 소속 대분류 존재 여부 검증 (Category Item Creation Parent Not Found)

## 개요

신규 소분류는 반드시 존재하는 대분류에 소속되어야 한다.

소분류 생성 요청에서 지정한 대분류가 존재하지 않으면 생성 작업을 중단하고 `CATEGORY_NOT_FOUND` 예외를 발생시킨다.

## 테스트 대상

- `CategoryItemService.create()`

## 테스트 시나리오

### 1. 존재하지 않는 대분류에는 소분류를 생성할 수 없다

- **대상:** `CategoryItemService.create()`
- **시나리오:** 소분류 생성 시 지정한 대분류가 존재하지 않으면 생성 요청을 거부한다.
- **사전 조건:**
    - `categoryId = 999`
    - `CreateCategoryItemRequest.name = "테스트 소분류"`
    - `CategoryRepository.findByIdForUpdate(999)`는 `null`을 반환한다.
    - `CategoryRepository`, `CategoryItemRepository`는 Mock으로 구성한다.
- **실행:** `CategoryItemService.create(categoryId, body)` 호출
- **기대 결과:**
    - `GrimaultException`이 발생한다.
    - 예외의 `errorCode`는 `ErrorCode.CATEGORY_NOT_FOUND`와 일치한다.
    - `CategoryItemRepository.findMaxSortOrder()` 및 `save()`는 호출되지 않는다.
- **테스트 형태:** `@Test`