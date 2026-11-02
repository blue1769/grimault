# 존재하지 않는 대분류 수정 (Category Update Not Found)

## 개요

이름 수정 대상 대분류가 존재하지 않으면 수정 작업을 수행하지 않고 `CATEGORY_NOT_FOUND` 예외를 발생시킨다.

## 테스트 대상

- `CategoryService.update()`

## 테스트 시나리오

### 1. 존재하지 않는 대분류의 이름은 수정할 수 없다

- **대상:** `CategoryService.update()`
- **시나리오:** 존재하지 않는 대분류 ID로 이름 수정을 요청하면 예외를 발생시킨다.
- **사전 조건:**
  - `id = 999`
  - `UpdateCategoryRequest.name = "외식비"`
  - `CategoryRepository`는 해당 ID 조회 시 결과가 없음을 반환한다.
- **실행:** `CategoryService.update(id = 999, body)` 호출
- **기대 결과:**
  - `GrimaultException`이 발생한다.
  - 예외의 `errorCode`가 `ErrorCode.CATEGORY_NOT_FOUND`와 일치한다.
- **테스트 형태:** `@Test`
