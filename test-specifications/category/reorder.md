# 대분류 순번 변경 (Category Reordering)

## 개요

대분류는 전체 Category를 하나의 정렬 범위로 관리하며, 지정한 목표 순번에 따라 재정렬한다.

이동 방향에 맞는 Bulk Update를 호출하고, 동일 순번 요청은 변경 없이 정상 처리한다.

## 테스트 대상

- `CategoryService.reorder()`

## 테스트 시나리오

### 1. 앞에서 뒤로 이동하면 Forward 재정렬을 수행한다

- **대상:** `CategoryService.reorder()`
- **시나리오:** 현재 순번보다 큰 목표 순번을 요청하면 Forward 재정렬을 수행한다.
- **사전 조건:**
    - `id = 10`, 현재 `sortOrder = 2`
    - `ReorderCategoryEntryRequest.sortOrder = 4`
    - `CategoryRepository.findMaxSortOrder()`는 `5`를 반환한다.
    - 최초 조회 시 `sortOrder = 2`, 재조회 시 `sortOrder = 4`인 Category를 반환하도록 Mock을 구성한다.
    - `CategoryOrderLockRepository`, `CategoryRepository`, `EntityManager`는 Mock으로 구성한다.
- **실행:** `CategoryService.reorder(id = 10, body)` 호출
- **기대 결과:**
    - Advisory Lock 획득 후 최대 순번과 대상 Category를 조회한다.
    - `deferOrderConstraint()`를 호출한다.
    - `EntityManager.flush()` 이후 `reorderForward(10, 2, 4)`를 호출한다.
    - Bulk Update 이후 `EntityManager.clear()`를 호출한다.
    - Category를 재조회하고 반환된 `CategoryResponse.sortOrder`가 `4`와 일치한다.
    - 위 작업의 실행 순서를 검증한다.
    - `reorderBackward()`는 호출하지 않는다.
- **테스트 형태:** `@Test`

### 2. 뒤에서 앞으로 이동하면 Backward 재정렬을 수행한다

- **대상:** `CategoryService.reorder()`
- **시나리오:** 현재 순번보다 작은 목표 순번을 요청하면 Backward 재정렬을 수행한다.
- **사전 조건:**
    - `id = 10`, 현재 `sortOrder = 4`
    - `ReorderCategoryEntryRequest.sortOrder = 2`
    - `CategoryRepository.findMaxSortOrder()`는 `5`를 반환한다.
    - 최초 조회 시 `sortOrder = 4`, 재조회 시 `sortOrder = 2`인 Category를 반환하도록 Mock을 구성한다.
    - 관련 Repository와 `EntityManager`는 Mock으로 구성한다.
- **실행:** `CategoryService.reorder(id = 10, body)` 호출
- **기대 결과:**
    - Advisory Lock 획득 후 최대 순번과 대상 Category를 조회한다.
    - `deferOrderConstraint()`를 호출한다.
    - `EntityManager.flush()` 이후 `reorderBackward(10, 4, 2)`를 호출한다.
    - Bulk Update 이후 `EntityManager.clear()`를 호출한다.
    - Category를 재조회하고 반환된 `CategoryResponse.sortOrder`가 `2`와 일치한다.
    - 위 작업의 실행 순서를 검증한다.
    - `reorderForward()`는 호출하지 않는다.
- **테스트 형태:** `@Test`

### 3. 동일 순번 요청은 변경 없이 정상 처리한다

- **대상:** `CategoryService.reorder()`
- **시나리오:** 현재 순번과 목표 순번이 동일하면 재정렬을 수행하지 않는다.
- **사전 조건:**
    - `id = 10`, 현재 `sortOrder = 3`
    - `ReorderCategoryEntryRequest.sortOrder = 3`
    - 최대 순번은 `5`이며 대상 Category가 존재한다.
    - 관련 의존성은 Mock으로 구성한다.
- **실행:** `CategoryService.reorder(id = 10, body)` 호출
- **기대 결과:**
    - Advisory Lock 획득 후 현재 Category를 조회한다.
    - 기존 Category와 일치하는 `CategoryResponse`를 반환한다.
    - `deferOrderConstraint()`, `reorderForward()`, `reorderBackward()`는 호출하지 않는다.
    - `EntityManager.flush()`와 `clear()`는 호출하지 않는다.
- **테스트 형태:** `@Test`

### 4. 목표 순번이 최대 순번을 초과하면 예외를 발생시킨다

- **대상:** `CategoryService.reorder()`
- **시나리오:** 목표 순번이 전체 대분류의 최대 순번보다 크면 요청을 거부한다.
- **사전 조건:**
    - `id = 10`
    - `ReorderCategoryEntryRequest.sortOrder = 6`
    - `CategoryRepository.findMaxSortOrder()`는 `5`를 반환한다.
    - 관련 의존성은 Mock으로 구성한다.
- **실행:** `CategoryService.reorder(id = 10, body)` 호출
- **기대 결과:**
    - Advisory Lock 획득 후 최대 순번을 조회한다.
    - `GrimaultException`이 발생한다.
    - 예외의 `errorCode`가 `ErrorCode.INVALID_CATEGORY_ORDER`와 일치한다.
    - 대상 Category 조회와 순번 변경 작업은 수행하지 않는다.
- **테스트 형태:** `@Test`

### 5. 존재하지 않는 대분류의 순번은 변경할 수 없다

- **대상:** `CategoryService.reorder()`
- **시나리오:** 존재하지 않는 대분류 ID로 순번 변경을 요청하면 예외를 발생시킨다.
- **사전 조건:**
    - `id = 999`
    - `ReorderCategoryEntryRequest.sortOrder = 2`
    - 최대 순번은 `5`다.
    - `CategoryRepository`는 해당 ID 조회 시 빈 결과를 반환한다.
    - 관련 의존성은 Mock으로 구성한다.
- **실행:** `CategoryService.reorder(id = 999, body)` 호출
- **기대 결과:**
    - Advisory Lock 획득 후 최대 순번과 대상 Category를 조회한다.
    - `GrimaultException`이 발생한다.
    - 예외의 `errorCode`가 `ErrorCode.CATEGORY_NOT_FOUND`와 일치한다.
    - `deferOrderConstraint()`, `reorderForward()`, `reorderBackward()`는 호출하지 않는다.
    - `EntityManager.flush()`와 `clear()`는 호출하지 않는다.
- **테스트 형태:** `@Test`