# 소분류 활성화·비활성화 (Category Item Activation / Deactivation)

## 개요

소분류의 활성 상태는 소속 대분류의 상태와 독립적으로 변경하며, 해당 CategoryItem의 `isActive`만 변경한다.

활성 상태 변경은 멱등성을 보장하고, 소속 대분류 및 다른 속성의 상태를 유지한다.

상위 Category의 잠금 획득 과정에서 CategoryItem의 소속이 변경되었다면 충돌로 처리한다.

## 테스트 대상

- `CategoryItemService.changeActiveStatus()`

## 테스트 시나리오

### 1. 비활성 소분류를 활성화한다

- **대상:** `CategoryItemService.changeActiveStatus()`
- **시나리오:** 비활성 상태인 소분류에 활성화 요청을 수행한다.
- **사전 조건:**
    - 소분류 `id = 10`, `category.id = 1`, `isActive = false`, `sortOrder = 2`, `isFavorite = true`
    - 소속 Category는 활성 상태이며 기존 속성을 가진다.
    - `CategoryItemRepository.findById(10)`은 해당 소분류를 반환한다.
    - `CategoryRepository.findByIdForUpdate(1)`은 소속 대분류를 반환한다.
    - `EntityManager.refresh(item)` 이후에도 소분류의 소속 및 기존 속성은 유지된다.
    - 관련 Repository와 `EntityManager`는 Mock으로 구성한다.
- **실행:** `CategoryItemService.changeActiveStatus(id = 10, active = true)` 호출
- **기대 결과:**
    - 최초 소분류 조회 후 상위 Category의 잠금을 획득하고 `refresh(item)`을 호출한다.
    - 잠금 전후 소속 대분류가 동일함을 확인한다.
    - CategoryItem의 `isActive`가 `true`로 변경된다.
    - 이름, 소속, 순번, 즐겨찾기 등 다른 속성은 변경되지 않는다.
    - 소속 Category의 활성 상태 및 다른 속성은 변경되지 않는다.
    - 반환된 `CategoryItemResponse.isActive`가 `true`와 일치한다.
    - 소분류 조회, 상위 Category 잠금, `refresh()`의 실행 순서를 검증한다.
- **테스트 형태:** `@Test`

### 2. 활성 소분류를 비활성화한다

- **대상:** `CategoryItemService.changeActiveStatus()`
- **시나리오:** 활성 상태인 소분류에 비활성화 요청을 수행한다.
- **사전 조건:**
    - 소분류 `id = 10`, `category.id = 1`, `isActive = true`, `sortOrder = 2`, `isFavorite = true`
    - 소속 Category는 활성 상태이며 기존 속성을 가진다.
    - `CategoryItemRepository.findById(10)`은 해당 소분류를 반환한다.
    - `CategoryRepository.findByIdForUpdate(1)`은 소속 대분류를 반환한다.
    - `EntityManager.refresh(item)` 이후에도 소분류의 소속 및 기존 속성은 유지된다.
    - 관련 Repository와 `EntityManager`는 Mock으로 구성한다.
- **실행:** `CategoryItemService.changeActiveStatus(id = 10, active = false)` 호출
- **기대 결과:**
    - 최초 소분류 조회 후 상위 Category의 잠금을 획득하고 `refresh(item)`을 호출한다.
    - 잠금 전후 소속 대분류가 동일함을 확인한다.
    - CategoryItem의 `isActive`가 `false`로 변경된다.
    - 이름, 소속, 순번, 즐겨찾기 등 다른 속성은 변경되지 않는다.
    - 소속 Category의 활성 상태 및 다른 속성은 변경되지 않는다.
    - 반환된 `CategoryItemResponse.isActive`가 `false`와 일치한다.
    - 소분류 조회, 상위 Category 잠금, `refresh()`의 실행 순서를 검증한다.
- **테스트 형태:** `@Test`

### 3. 이미 동일한 활성 상태로 변경 요청한다

- **대상:** `CategoryItemService.changeActiveStatus()`
- **시나리오:** 이미 활성 상태인 소분류에 동일한 활성화 요청을 반복한다.
- **사전 조건:**
    - 소분류 `id = 10`, `category.id = 1`, `isActive = true`, `sortOrder = 2`
    - `CategoryItemRepository.findById(10)`은 동일한 소분류를 반환한다.
    - `CategoryRepository.findByIdForUpdate(1)`은 소속 대분류를 반환한다.
    - `EntityManager.refresh(item)` 이후에도 소속과 활성 상태는 유지된다.
    - 관련 의존성은 Mock으로 구성한다.
- **실행:** `CategoryItemService.changeActiveStatus(id = 10, active = true)`를 연속 두 번 호출
- **기대 결과:**
    - 두 요청 모두 정상 처리된다.
    - CategoryItem의 `isActive`는 `true`로 유지된다.
    - 소속 대분류 및 다른 속성은 변경되지 않는다.
    - 두 요청의 `CategoryItemResponse`는 동일한 상태를 나타낸다.
    - 각 요청에서 소분류 조회, 상위 Category 잠금, `refresh()`를 수행한다.
- **테스트 형태:** `@Test`

### 4. 잠금 획득 중 소속 대분류가 변경되면 충돌로 처리한다

- **대상:** `CategoryItemService.changeActiveStatus()`
- **시나리오:** 최초 조회한 소속과 잠금 획득 후 확인한 소속이 다르면 활성 상태 변경을 거부한다.
- **사전 조건:**
    - 소분류 `id = 10`, 최초 조회 시 `category.id = 1`, `isActive = false`
    - `CategoryItemRepository.findById(10)`은 해당 소분류를 반환한다.
    - `CategoryRepository.findByIdForUpdate(1)`은 대분류 1을 반환한다.
    - `EntityManager.refresh(item)` 이후 소분류의 소속이 대분류 2인 상태가 되도록 Mock을 구성한다.
    - `refresh()`에 따른 소속 변경은 테스트의 상태 전환 모형으로 구성한다.
    - 관련 Repository와 `EntityManager`는 Mock으로 구성한다.
- **실행:** `CategoryItemService.changeActiveStatus(id = 10, active = true)` 호출
- **기대 결과:**
    - 최초 소분류 조회 후 대분류 1의 잠금을 획득한다.
    - `EntityManager.refresh(item)`을 호출한다.
    - 변경된 소속을 확인하고 `GrimaultException`을 발생시킨다.
    - 예외의 `errorCode`가 `ErrorCode.CATEGORY_ITEM_UPDATE_CONFLICT`와 일치한다.
    - 활성 상태 변경은 수행하지 않으며, `isActive`는 기존 상태로 유지된다.
    - 소분류 조회, 상위 Category 잠금, `refresh()`의 실행 순서를 검증한다.
- **테스트 형태:** `@Test`