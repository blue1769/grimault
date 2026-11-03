# 잘못된 소분류 수정 요청 (Invalid Category Item Update)

## 개요

소분류 수정은 존재하는 소분류를 대상으로 해야 하며, 다른 대분류로 이동할 때 목적지의 존재 여부, 현재 소속과의 차이, 분류 유형 일치 조건을 검증한다.
거부된 요청은 기존 소분류의 소속, 속성 및 정렬 상태를 변경하지 않는다.

## 테스트 대상

- `CategoryItemService.update()`

## 테스트 시나리오

### 1. 존재하지 않는 소분류는 수정할 수 없다

- **대상:** `CategoryItemService.update()`
- **시나리오:** 수정 대상이 존재하지 않으면 작업을 중단한다.
- **사전 조건:**
  - 소분류 ID 999는 존재하지 않는다.
  - `CategoryItemRepository.findById(999)`는 빈 조회 결과를 반환한다.
  - `UpdateCategoryItemRequest(name="변경명")`을 사용한다.
- **실행:** `CategoryItemService.update(999, body)` 호출.
- **기대 결과:**
  - `GrimaultException` 발생, `errorCode=ErrorCode.CATEGORY_ITEM_NOT_FOUND`.
  - 대분류 잠금 조회, `entityManager.refresh()`, 순번 조회 및 이동 로직은 수행되지 않는다.
- **테스트 형태:** `@Test`

### 2. 존재하지 않는 대분류로 이동할 수 없다

- **대상:** `CategoryItemService.update()`
- **시나리오:** 목적지가 없으면 기존 소분류를 변경하지 않는다.
- **사전 조건:**
  - 소분류 ID 10은 대분류 1에 속한다.
  - 대분류 1의 잠금 조회는 성공하지만 대분류 999의 잠금 조회는 `null`이다.
  - `UpdateCategoryItemRequest(categoryId=999)`를 사용한다.
- **실행:** `CategoryItemService.update(10, body)` 호출.
- **기대 결과:**
  - `GrimaultException` 발생, `errorCode=ErrorCode.CATEGORY_NOT_FOUND`.
  - 소분류의 소속과 속성은 유지된다.
  - 목적지 최대 순번 조회, 순번 재정렬, `deferOrderConstraint()`는 수행되지 않는다.
- **테스트 형태:** `@Test`

### 3. 현재 소속과 동일한 대분류로 이동할 수 없다

- **대상:** `CategoryItemService.update()`
- **시나리오:** 동일한 소속을 명시적으로 지정한 이동 요청은 무효로 처리한다.
- **사전 조건:**
  - 소분류 ID 10은 대분류 1에 소속되어 있고 `refresh()` 이후에도 소속은 1이다.
  - `UpdateCategoryItemRequest(categoryId=1)`을 사용한다.
  - 대분류 1에 대한 잠금 조회는 성공한다.
- **실행:** `CategoryItemService.update(10, body)` 호출.
- **기대 결과:**
  - `GrimaultException` 발생, `errorCode=ErrorCode.INVALID_CATEGORY_ITEM_MOVE`.
  - 기존 소속·순번·속성은 유지된다.
  - 최대 순번 조회, 순번 재정렬, `deferOrderConstraint()`는 수행되지 않는다.
- **테스트 형태:** `@Test`

### 4. 유형이 다른 대분류 사이에서는 이동할 수 없다

- **대상:** `CategoryItemService.update()`
- **시나리오:** `EXPENSE`와 `INCOME` 사이의 소속 이동을 거부한다.
- **사전 조건:**
  - 소분류 ID 10은 대분류 1(`EXPENSE`)에 소속되어 있다.
  - 목적지 대분류 2는 `INCOME`이며 양쪽 잠금 조회가 성공한다.
  - 최초/`refresh()` 이후 소속은 대분류 1이다.
  - `UpdateCategoryItemRequest(categoryId=2)`를 사용한다.
- **실행:** `CategoryItemService.update(10, body)` 호출.
- **기대 결과:**
  - `GrimaultException` 발생, `errorCode=ErrorCode.INVALID_CATEGORY_TYPE`.
  - 소분류 ID, 소속, 이름, 활성 상태, 즐겨찾기 및 순번은 유지된다.
  - 목적지 최대 순번 조회 및 `deferOrderConstraint()`는 수행되지 않는다.
- **테스트 형태:** `@Test`

## 검증 범위

- 해당 예외는 서비스 비즈니스 규칙을 기준으로 검증한다.
- 이름 중복의 실제 DB UNIQUE 위반, HTTP 400/404/409 변환, `@Positive` 및 `@Pattern` 등의 표준 Bean Validation 동작은 단위 테스트 범위에서 제외한다.
- Mock 기반 예외 검증은 트랜잭션 롤백 자체를 보증하지 않는다.
