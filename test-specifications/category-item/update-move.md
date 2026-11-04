# 소분류 소속 이동과 순번 정합성 (Category Item Move and Ordering)

## 개요

소분류는 동일한 `CategoryType`의 다른 대분류로 이동할 수 있다. 이동한 항목은 목적지 마지막 순번에 배치되고, 출발지의 남은 항목은 1부터 연속된 순번을 유지한다.

잠금 대기 중 소분류의 소속이 변경되었다면 최초 관찰한 소속을 기준으로 작업하지 않고 충돌 예외를 발생시킨다.

소분류 이동 시 출발지의 순번 압축은 `CategoryItemRepository.compact()`의 JPQL Bulk Update로 수행한다. 서비스는 이동 대상의 변경을 먼저 `flush()`하고, 출발지 순번 압축을 실행한 다음 영속성 컨텍스트를 `clear()`한다.

## 테스트 대상

- `CategoryItemService.update()`
- `CategoryItemRepository.compact()` 호출 계약

## 테스트 시나리오

### 1. 다른 대분류로 이동하면 목적지의 마지막 순번에 배치된다

- **대상:** `CategoryItemService.update()`
- **시나리오:** 이동 요청은 같은 유형의 대상 대분류에 소분류를 마지막 순번으로 배치한다.
- **사전 조건:**
  - 소분류 ID 10은 대분류 1(`EXPENSE`)에 소속되어 있으며 `sortOrder=2`, `name="식자재"`, `isActive=false`, `isFavorite=true`다.
  - 목적지 대분류 2도 `EXPENSE` 유형이다.
  - 대분류 1과 2는 ID 오름차순으로 잠금 조회에 성공한다.
  - `CategoryItemRepository.findMaxSortOrder(2)`는 `3`을 반환한다.
  - `UpdateCategoryItemRequest(categoryId=2)`를 사용하며 `refresh()` 이후 소속도 1이다.
- **실행:** `CategoryItemService.update(10, body)` 호출.
- **기대 결과:**
  - 대분류 잠금 조회는 ID `1 → 2` 순서로 수행된다.
  - ID 10의 소속이 2로 변경되고 순번은 4가 된다.
  - ID, 이름, 활성 상태 및 즐겨찾기는 유지된다.
  - 반환된 `CategoryItemResponse`가 이동 후 상태와 일치한다.
  - `deferOrderConstraint()`를 호출한다.
  - 이동 대상의 변경을 `flush()`한 후 `compact(1, 2)`를 호출한다.
  - Bulk Update 실행 이후 `clear()`를 호출한다.
- **테스트 형태:** `@Test`

### 2. 소분류가 없는 목적지로 이동하면 순번은 1부터 시작한다

- **대상:** `CategoryItemService.update()`
- **시나리오:** 대상 대분류가 비어 있어도 정상 이동하며 첫 순번을 부여한다.
- **사전 조건:**
  - ID 10이 대분류 1에 속해 있고 `sortOrder=1`이다.
  - 동일 유형의 목적지 대분류 2에는 소분류가 없다.
  - `findMaxSortOrder(2)`는 `null`을 반환한다.
  - `UpdateCategoryItemRequest(categoryId=2)`를 사용하며 `refresh()` 이후 소속은 1로 유지된다.
- **실행:** `CategoryItemService.update(10, body)` 호출.
- **기대 결과:**
  - 이동한 소분류의 `category.id=2`, `sortOrder=1`이다.
  - 변경하지 않은 속성이 보존된다.
  - `compact(1, 1)`을 호출한다.
  - `flush()` → `compact()` → `clear()` 순서로 실행된다.
- **테스트 형태:** `@Test`

### 3. 제거 위치에 따라 올바른 순번 압축 조건을 전달한다

- **대상:** `CategoryItemService.update()`
- **시나리오:** 서로 다른 대분류 간에 앞·중간·마지막 항목 또는 단일 항목을 이동할 때, 출발지 대분류와 제거 대상의 기존 순번을 기준으로 Bulk Update를 요청한다.
- **사전 조건:**
  - 출발지와 목적지 대분류는 입력 조건별로 지정하며, 서로 다른 ID를 갖는다.
  - 두 대분류는 동일한 `CategoryType`이다.
  - 이동 요청은 각 입력 조건의 목적지 ID를 `categoryId`로 지정한다.
  - `refresh()` 이후에도 이동 대상의 소속은 각 입력 조건의 출발지 대분류로 유지된다.
  - 이동 대상의 기존 순번은 아래 입력 조건을 따른다.
- **실행:** 입력 조건별로 `CategoryItemService.update(itemId, body)` 호출.
- **기대 결과:**
  - 출발지와 목적지 대분류를 ID 오름차순으로 잠금 조회한다.
  - 잠금 획득 순서와 관계없이 출발지 대분류 ID를 순번 압축 대상으로 사용한다.
  - 이동 대상의 기존 `sortOrder`를 압축 기준으로 사용한다.
  - 모든 입력 조건에서 기대하는 `compact(originGroupId, removedOrder)`를 정확히 한 번 호출한다.
  - 이동 대상은 목적지의 마지막 순번으로 배치된다.
  - 형제 엔티티의 메모리 상태 변경 여부는 검증하지 않는다.
- **테스트 형태:** `@ParameterizedTest`

#### 입력 조건

| 출발지 | 목적지 | 이동 대상 | 기존 순번 | 기대 `compact()` 호출 |
|---|---|---|---:|---|
| 1 | 2 | ID 10 (첫 항목) | 1 | `compact(1, 1)` |
| 2 | 1 | ID 20 (중간 항목) | 2 | `compact(2, 2)` |
| 3 | 5 | ID 40 (마지막 항목) | 4 | `compact(3, 4)` |
| 5 | 3 | ID 10 (단일 항목) | 1 | `compact(5, 1)` |

### 4. 중간 소분류를 이동하면 Bulk Update를 올바른 순서로 실행한다

- **대상:** `CategoryItemService.update()`
- **시나리오:** 중간 소분류의 소속과 순번을 변경한 다음, 출발지 순번 압축을 Bulk Update로 요청하고 영속성 컨텍스트를 정리한다.
- **사전 조건:**
  - 출발지 대분류 1(`EXPENSE`)에 소분류 A(ID 10, 순번 1), B(ID 20, 순번 2), C(ID 30, 순번 3)가 있다.
  - B를 동일 유형의 대분류 2로 이동한다.
  - 목적지 최대 순번은 2이며, 최초 조회 및 `refresh()` 이후 B의 소속은 1이다.
  - `UpdateCategoryItemRequest(categoryId=2, name="변경된 B", isFavorite=false)`를 사용한다.
- **실행:** `CategoryItemService.update(20, body)` 호출.
- **기대 결과:**
  - B의 소속은 대분류 2, 순번은 3으로 변경된다.
  - B의 이름과 즐겨찾기도 요청한 값으로 변경된다.
  - 변경하지 않은 ID와 활성 상태는 유지된다.
  - `deferOrderConstraint()`가 호출된다.
  - B의 소속·순번·속성 변경 이후 `entityManager.flush()`가 호출된다.
  - `repository.compact(1, 2)`가 정확히 한 번 호출된다.
  - `entityManager.clear()`가 Bulk Update 이후 호출된다.
  - 실행 순서는 `deferOrderConstraint()` → 이동 및 속성 변경 → `flush()` → `compact(1, 2)` → `clear()`를 만족한다.
  - 반환된 `CategoryItemResponse`는 이동 후 B의 상태와 일치한다.
- **테스트 형태:** `@Test`

### 5. 잠금 대기 중 소속이 바뀌면 기존 소속을 기준으로 수정을 진행하지 않는다

- **대상:** `CategoryItemService.update()`
- **시나리오:** 최초 조회와 `refresh()` 이후 소속이 다르면 변경 충돌을 반환하고 후속 변경을 수행하지 않는다.
- **사전 조건:**
  - 최초 조회된 소분류 ID 10의 소속은 대분류 1이다.
  - 대분류 1에 대한 잠금 조회가 완료된 후 `EntityManager.refresh(item)` 호출 시 소분류의 소속이 대분류 2인 상태가 되도록 Mock을 구성한다.
  - 요청은 `UpdateCategoryItemRequest(name="변경명", isFavorite=true)`로 지정한다.
  - 동일한 Mock을 통한 소속 변경은 **테스트의 상태 전환 모형**일 뿐 실제 DB 경합 재현은 아니다.
- **실행:** `CategoryItemService.update(10, body)` 호출.
- **기대 결과:**
  - `GrimaultException`이 발생하고 `errorCode=ErrorCode.CATEGORY_ITEM_UPDATE_CONFLICT`다.
  - `refresh()` 이후 이름·즐겨찾기·순번 변경 및 목적지 이동 로직이 수행되지 않는다.
  - 이동 대상 최대 순번 조회나 제약 지연 호출이 발생하지 않는다.
  - `compact()`, `flush()`, `clear()`가 호출되지 않는다.
- **테스트 형태:** `@Test`

## 검증 범위

- 단위 테스트는 MockK 및 메모리 엔티티로 서비스의 이동 판단, 목적지 순번 계산, 요청 속성 변경, Repository 호출 인수 및 실행 순서를 검증한다.
- JPQL Bulk Update는 영속성 컨텍스트를 우회하므로, Mock 기반 단위 테스트에서 출발지 형제 엔티티의 `sortOrder` 변경을 기대하거나 검증하지 않는다.
- `compact()`의 영향 행 수와 실제 DB 변경 결과는 Mock 기반 서비스 단위 테스트의 검증 대상이 아니다.
- 비관적 잠금의 실제 순서, PostgreSQL 동시 요청 직렬화, UNIQUE 제약 지연 검사 및 Bulk Update의 실제 SQL 실행 결과는 단위 테스트에서 보증하지 않는다.
- 실제 JPQL 순번 압축 결과는 PostgreSQL에서 별도로 수동 검증한다.
- 실제 잠금 경합과 교차 이동의 정합성은 별도로 수행한 수동 DB/HTTP 검증을 기준으로 한다.