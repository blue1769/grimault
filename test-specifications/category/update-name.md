# 대분류 이름 수정 (Category Name Update)

## 개요

대분류의 이름은 변경할 수 있으며, 이름 수정 과정에서 대분류의 식별자, 유형, 활성 상태 및 순번은 유지한다.

비활성 대분류도 이름 수정이 가능하다.

## 테스트 대상

- `CategoryService.update()`

## 테스트 시나리오

### 1. 대분류 이름을 변경해도 다른 속성은 유지된다

- **대상:** `CategoryService.update()`
- **시나리오:** 기존 대분류의 이름을 변경하고, 변경 대상이 아닌 속성이 유지되는지 검증한다.
- **사전 조건:**
  - `id = 10`
  - `CategoryRepository`는 해당 ID 조회 시 다음 Category를 반환한다.
    - `id = 10`
    - `type = EXPENSE`
    - `name = "식비"`
    - `isActive = false`
    - `sortOrder = 3`
  - `UpdateCategoryRequest.name = "외식비"`
- **실행:** `CategoryService.update(id = 10, body)` 호출
- **기대 결과:**
  - 조회된 Category의 `name`이 `"외식비"`로 변경된다.
  - Category의 `id`, `type`, `isActive`, `sortOrder`는 기존 값으로 유지된다.
  - 반환된 `CategoryResponse`의 `id`, `type`, `name`, `isActive`, `sortOrder`는 변경 후 Category의 상태와 일치한다.
- **테스트 형태:** `@Test`

## 검증 범위

- 비활성 대분류에 대해서도 이름 변경이 허용되는지 확인한다.
- JPA Dirty Checking 및 실제 DB UPDATE 실행은 검증하지 않는다.
