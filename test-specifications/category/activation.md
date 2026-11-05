# 대분류 활성화·비활성화 (Category Activation / Deactivation)

## 개요

대분류의 활성 상태는 별도 API를 통해 변경하며, 해당 Category의 `isActive`만 변경한다.

활성 상태 변경은 멱등성을 보장하고, 다른 속성과 하위 CategoryItem의 상태를 유지한다.

## 테스트 대상

- `CategoryService.changeActiveStatus()`

## 테스트 시나리오

### 1. 비활성 대분류를 활성화한다

- **대상:** `CategoryService.changeActiveStatus()`
- **시나리오:** 비활성 상태인 대분류에 활성화 요청을 수행한다.
- **사전 조건:**
    - 대분류 `id = 10`, `isActive = false`, `sortOrder = 2`
    - 해당 대분류에 소속된 CategoryItem이 존재하며 기존 활성 상태를 가진다.
    - `CategoryOrderLockRepository`, `CategoryRepository`는 Mock으로 구성한다.
    - `CategoryRepository.findById(10)`은 해당 대분류를 반환한다.
- **실행:** `CategoryService.changeActiveStatus(id = 10, active = true)` 호출
- **기대 결과:**
    - Advisory Lock을 획득한 후 대상 Category를 조회한다.
    - Category의 `isActive`가 `true`로 변경된다.
    - 이름, 유형, 순번 등 다른 속성은 변경되지 않는다.
    - 하위 CategoryItem의 활성 상태는 변경되지 않는다.
    - 반환된 `CategoryResponse.isActive`가 `true`와 일치한다.
    - Advisory Lock 획득과 대상 조회의 실행 순서를 검증한다.
- **테스트 형태:** `@Test`

### 2. 활성 대분류를 비활성화한다

- **대상:** `CategoryService.changeActiveStatus()`
- **시나리오:** 활성 상태인 대분류에 비활성화 요청을 수행한다.
- **사전 조건:**
    - 대분류 `id = 10`, `isActive = true`, `sortOrder = 2`
    - 해당 대분류에 소속된 CategoryItem이 존재하며 기존 활성 상태를 가진다.
    - 관련 Repository는 Mock으로 구성한다.
    - `CategoryRepository.findById(10)`은 해당 대분류를 반환한다.
- **실행:** `CategoryService.changeActiveStatus(id = 10, active = false)` 호출
- **기대 결과:**
    - Advisory Lock을 획득한 후 대상 Category를 조회한다.
    - Category의 `isActive`가 `false`로 변경된다.
    - 이름, 유형, 순번 등 다른 속성은 변경되지 않는다.
    - 하위 CategoryItem의 활성 상태는 변경되지 않는다.
    - 반환된 `CategoryResponse.isActive`가 `false`와 일치한다.
    - Advisory Lock 획득과 대상 조회의 실행 순서를 검증한다.
- **테스트 형태:** `@Test`

### 3. 이미 동일한 활성 상태로 변경 요청한다

- **대상:** `CategoryService.changeActiveStatus()`
- **시나리오:** 이미 활성 상태인 대분류에 동일한 활성화 요청을 반복한다.
- **사전 조건:**
    - 대분류 `id = 10`, `isActive = true`, `sortOrder = 2`
    - 관련 Repository는 Mock으로 구성한다.
    - `CategoryRepository.findById(10)`은 동일한 대분류를 반환한다.
- **실행:** `CategoryService.changeActiveStatus(id = 10, active = true)`를 연속 두 번 호출
- **기대 결과:**
    - 두 요청 모두 정상 처리된다.
    - Category의 `isActive`는 `true`로 유지된다.
    - 이름, 유형, 순번 등 다른 속성은 변경되지 않는다.
    - 두 요청의 `CategoryResponse`는 동일한 상태를 나타낸다.
    - 각 요청에서 Advisory Lock 획득 후 대상 Category를 조회한다.
- **테스트 형태:** `@Test`