# 소분류 순번 변경 (Category Item Reordering)

## 개요

소분류는 소속 대분류별로 독립된 정렬 범위를 가지며, 지정한 목표 순번에 따라 재정렬한다.

이동 방향에 맞는 Bulk Update를 호출하고, 동일 순번 요청은 변경 없이 정상 처리한다.

잠금 획득 과정에서 소분류의 소속이 변경되었다면 충돌로 처리한다.

## 테스트 대상

- `CategoryItemService.reorder()`

## 테스트 시나리오

### 1. 앞에서 뒤로 이동하면 Forward 재정렬을 수행한다

- **대상:** `CategoryItemService.reorder()`
- **시나리오:** 현재 순번보다 큰 목표 순번을 요청하면 소속 대분류 내부에서 Forward 재정렬을 수행한다.
- **사전 조건:**
    - 소분류 `id = 10`, `category.id = 1`, 현재 `sortOrder = 2`
    - `ReorderCategoryEntryRequest.sortOrder = 4`
    - `CategoryRepository.findByIdForUpdate(1)`은 대분류 1을 반환한다.
    - `CategoryItemRepository.findMaxSortOrder(1)`은 `5`를 반환한다.
    - 최초 조회 시 `sortOrder = 2`, 재조회 시 `sortOrder = 4`인 CategoryItem을 반환하도록 Mock을 구성한다.
    - `EntityManager.refresh(item)` 이후에도 소속 대분류는 1로 유지된다.
    - 관련 Repository와 `EntityManager`는 Mock으로 구성한다.
- **실행:** `CategoryItemService.reorder(id = 10, body)` 호출
- **기대 결과:**
    - 최초 소분류 조회 후 상위 Category의 잠금을 획득하고 `refresh(item)`을 호출한다.
    - 소속 변경이 없음을 확인한 뒤 최대 순번을 조회한다.
    - `deferOrderConstraint()`를 호출한다.
    - `EntityManager.flush()` 이후 `reorderForward(10, 1, 2, 4)`를 호출한다.
    - Bulk Update 이후 `EntityManager.clear()`를 호출한다.
    - 소분류를 재조회하고 반환된 `CategoryItemResponse.sortOrder`가 `4`와 일치한다.
    - 위 작업의 실행 순서를 검증한다.
    - `reorderBackward()`는 호출하지 않는다.
- **테스트 형태:** `@Test`

### 2. 뒤에서 앞으로 이동하면 Backward 재정렬을 수행한다

- **대상:** `CategoryItemService.reorder()`
- **시나리오:** 현재 순번보다 작은 목표 순번을 요청하면 소속 대분류 내부에서 Backward 재정렬을 수행한다.
- **사전 조건:**
    - 소분류 `id = 10`, `category.id = 1`, 현재 `sortOrder = 4`
    - `ReorderCategoryEntryRequest.sortOrder = 2`
    - `CategoryRepository.findByIdForUpdate(1)`은 대분류 1을 반환한다.
    - `CategoryItemRepository.findMaxSortOrder(1)`은 `5`를 반환한다.
    - 최초 조회 시 `sortOrder = 4`, 재조회 시 `sortOrder = 2`인 CategoryItem을 반환하도록 Mock을 구성한다.
    - `EntityManager.refresh(item)` 이후에도 소속 대분류는 1로 유지된다.
    - 관련 의존성은 Mock으로 구성한다.
- **실행:** `CategoryItemService.reorder(id = 10, body)` 호출
- **기대 결과:**
    - 최초 소분류 조회, 상위 Category 잠금, `refresh(item)`, 최대 순번 조회 순서를 검증한다.
    - `deferOrderConstraint()`를 호출한다.
    - `EntityManager.flush()` 이후 `reorderBackward(10, 1, 4, 2)`를 호출한다.
    - Bulk Update 이후 `EntityManager.clear()`를 호출한다.
    - 소분류를 재조회하고 반환된 `CategoryItemResponse.sortOrder`가 `2`와 일치한다.
    - 위 작업의 실행 순서를 검증한다.
    - `reorderForward()`는 호출하지 않는다.
- **테스트 형태:** `@Test`

### 3. 동일 순번 요청은 변경 없이 정상 처리한다

- **대상:** `CategoryItemService.reorder()`
- **시나리오:** 현재 순번과 목표 순번이 동일하면 재정렬을 수행하지 않는다.
- **사전 조건:**
    - 소분류 `id = 10`, `category.id = 1`, 현재 `sortOrder = 3`
    - `ReorderCategoryEntryRequest.sortOrder = 3`
    - 상위 Category 잠금 및 소분류 `refresh()` 이후에도 소속과 순번은 유지된다.
    - `CategoryItemRepository.findMaxSortOrder(1)`은 `5`를 반환한다.
    - 관련 의존성은 Mock으로 구성한다.
- **실행:** `CategoryItemService.reorder(id = 10, body)` 호출
- **기대 결과:**
    - 상위 Category 잠금 및 현재 소분류 확인 후 기존 상태와 일치하는 `CategoryItemResponse`를 반환한다.
    - `deferOrderConstraint()`, `reorderForward()`, `reorderBackward()`는 호출하지 않는다.
    - `EntityManager.flush()`와 `clear()`는 호출하지 않는다.
- **테스트 형태:** `@Test`

### 4. 목표 순번이 최대 순번을 초과하면 예외를 발생시킨다

- **대상:** `CategoryItemService.reorder()`
- **시나리오:** 목표 순번이 소속 대분류의 최대 순번보다 크면 요청을 거부한다.
- **사전 조건:**
    - 소분류 `id = 10`, `category.id = 1`, 현재 `sortOrder = 2`
    - `ReorderCategoryEntryRequest.sortOrder = 6`
    - 상위 Category 잠금과 `refresh()` 이후에도 소속은 유지된다.
    - `CategoryItemRepository.findMaxSortOrder(1)`은 `5`를 반환한다.
    - 관련 의존성은 Mock으로 구성한다.
- **실행:** `CategoryItemService.reorder(id = 10, body)` 호출
- **기대 결과:**
    - 상위 Category 잠금 및 소속 확인 후 최대 순번을 조회한다.
    - `GrimaultException`이 발생한다.
    - 예외의 `errorCode`가 `ErrorCode.INVALID_CATEGORY_ORDER`와 일치한다.
    - `deferOrderConstraint()`, `reorderForward()`, `reorderBackward()`는 호출하지 않는다.
    - `EntityManager.flush()`와 `clear()`는 호출하지 않는다.
- **테스트 형태:** `@Test`

### 5. 존재하지 않는 소분류의 순번은 변경할 수 없다

- **대상:** `CategoryItemService.reorder()`
- **시나리오:** 존재하지 않는 소분류 ID로 순번 변경을 요청하면 예외를 발생시킨다.
- **사전 조건:**
    - `id = 999`
    - `ReorderCategoryEntryRequest.sortOrder = 2`
    - `CategoryItemRepository.findById(999)`는 빈 결과를 반환한다.
    - 관련 의존성은 Mock으로 구성한다.
- **실행:** `CategoryItemService.reorder(id = 999, body)` 호출
- **기대 결과:**
    - `GrimaultException`이 발생한다.
    - 예외의 `errorCode`가 `ErrorCode.CATEGORY_ITEM_NOT_FOUND`와 일치한다.
    - 상위 Category 잠금 및 최대 순번 조회는 수행되지 않는다.
    - 순번 변경, 제약 지연, `flush()`, `clear()`는 호출하지 않는다.
- **테스트 형태:** `@Test`

### 6. 잠금 획득 중 소속 대분류가 변경되면 충돌로 처리한다

- **대상:** `CategoryItemService.reorder()`
- **시나리오:** 최초 조회한 소속과 잠금 획득 후 확인한 소속이 다르면 순번 변경을 거부한다.
- **사전 조건:**
    - 소분류 `id = 10`, 최초 조회 시 `category.id = 1`, `sortOrder = 2`
    - `ReorderCategoryEntryRequest.sortOrder = 3`
    - `CategoryRepository.findByIdForUpdate(1)`은 대분류 1을 반환한다.
    - `EntityManager.refresh(item)` 이후 소분류의 소속이 대분류 2인 상태가 되도록 Mock을 구성한다.
    - `refresh()`에 따른 소속 변경은 테스트의 상태 전환 모형으로 구성한다.
    - 관련 의존성은 Mock으로 구성한다.
- **실행:** `CategoryItemService.reorder(id = 10, body)` 호출
- **기대 결과:**
    - 최초 소분류 조회 후 대분류 1의 잠금을 획득한다.
    - `EntityManager.refresh(item)`을 호출한다.
    - 변경된 소속을 확인하고 `GrimaultException`을 발생시킨다.
    - 예외의 `errorCode`가 `ErrorCode.CATEGORY_ITEM_UPDATE_CONFLICT`와 일치한다.
    - 최대 순번 조회, 제약 지연, 순번 변경은 수행하지 않는다.
    - `EntityManager.flush()`와 `clear()`는 호출하지 않는다.
- **테스트 형태:** `@Test`