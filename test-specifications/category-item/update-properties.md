# 소분류 속성 수정 (Category Item Property Update)

## 개요

소분류의 이름과 즐겨찾기 여부는 `PATCH /category-items/{id}`에서 독립적으로 또는 함께 수정할 수 있다.
수정하지 않은 속성은 유지하며, 활성 상태나 상위 대분류의 활성 상태는 속성 수정 가능 여부에 영향을 주지 않는다.

## 테스트 대상

- `CategoryItemService.update()`

## 테스트 시나리오

### 1. 비활성 소분류의 이름을 수정해도 다른 속성은 유지된다

- **대상:** `CategoryItemService.update()`
- **시나리오:** 비활성 소분류도 이름을 변경할 수 있으며 요청에 없는 속성이 보존되는지 검증한다.
- **사전 조건:**
  - 소분류 ID 10: `category.id=1`, `name="식자재"`, `isActive=false`, `sortOrder=2`, `isFavorite=true`.
  - 소속 대분류는 `EXPENSE` 유형이며 잠금 조회에 성공한다.
  - `UpdateCategoryItemRequest(name="식료품")`을 사용한다. `categoryId`, `isFavorite`는 `null`이다.
  - 최초 조회와 `entityManager.refresh(item)` 이후의 소속은 모두 대분류 1이다.
- **실행:** `CategoryItemService.update(10, body)` 호출.
- **기대 결과:**
  - 대상 소분류의 이름만 `"식료품"`으로 변경된다.
  - ID, 소속, `isActive=false`, `sortOrder=2`, `isFavorite=true`는 그대로 유지된다.
  - 반환된 `CategoryItemResponse`도 수정 후 상태와 일치한다.
  - 소속 이동을 위한 최대 순번 조회 및 순번 재정렬은 수행하지 않는다.
- **테스트 형태:** `@Test`

### 2. 즐겨찾기를 지정하거나 해제할 수 있다

- **대상:** `CategoryItemService.update()`
- **시나리오:** 즐겨찾기 속성은 양방향으로 변경할 수 있고, 다른 속성을 변경하지 않는다.
- **사전 조건:**
  - 소분류 ID 10: `category.id=1`, `name="식자재"`, `sortOrder=2`이며 소속 대분류 잠금 조회에 성공한다.
  - 최초 조회와 `refresh()` 이후의 소속은 모두 대분류 1이다.
  - 요청은 `isFavorite`만 지정한다.
- **실행:** 입력 조건별로 `CategoryItemService.update(10, body)` 호출.
- **기대 결과:**
  - 즐겨찾기가 요청 값으로 변경되고 응답에 반영된다.
  - ID, 이름, 소속, 활성 상태, 순번이 유지된다.
  - 소속 이동 및 순번 재정렬은 수행하지 않는다.
- **테스트 형태:** `@ParameterizedTest`

#### 입력 조건

| 기존 `isFavorite` | 요청 `isFavorite` | 기대 `isFavorite` |
|---|---|---|
| `false` | `true` | `true` |
| `true` | `false` | `false` |

### 3. 이름과 즐겨찾기를 한 요청에서 함께 수정할 수 있다

- **대상:** `CategoryItemService.update()`
- **시나리오:** 두 수정 속성이 모두 지정되면 각각 변경되고 수정 대상이 아닌 속성은 유지된다.
- **사전 조건:**
  - 소분류 ID 10: `category.id=1`, `name="식자재"`, `isFavorite=false`, `sortOrder=2`, `isActive=true`.
  - `UpdateCategoryItemRequest(name="식료품", isFavorite=true)`를 사용한다.
  - 대분류 잠금 조회에 성공하고 `refresh()` 이후에도 소속은 1이다.
- **실행:** `CategoryItemService.update(10, body)` 호출.
- **기대 결과:**
  - 이름은 `"식료품"`, 즐겨찾기는 `true`가 된다.
  - ID, 소속, 활성 상태 및 순번은 유지된다.
  - 반환된 `CategoryItemResponse`에 두 변경 결과가 함께 반영된다.
- **테스트 형태:** `@Test`

## 검증 범위

- Repository와 EntityManager는 Mock으로 격리한다. `refresh()` 호출은 허용하되 실제 DB 동작을 재현하지 않는다.
- JPA Dirty Checking, SQL 실행, PostgreSQL 잠금, HTTP 응답 및 표준 Bean Validation 자체는 검증하지 않는다.
