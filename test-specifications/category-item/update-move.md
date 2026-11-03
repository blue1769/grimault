# 소분류 소속 이동과 순번 정합성 (Category Item Move and Ordering)

## 개요

소분류는 동일한 `CategoryType`의 다른 대분류로 이동할 수 있다. 이동한 항목은 목적지 마지막 순번에 배치되고, 출발지의 남은 항목은 1부터 연속된 순번을 유지한다.
잠금 대기 중 소분류의 소속이 변경되었다면, 최초 관찰한 소속을 기준으로 작업하지 않고 충돌 예외를 발생시킨다.

## 테스트 대상

- `CategoryItemService.update()`
- `CategoryItemReorderer.compact()`

## 테스트 시나리오

### 1. 다른 대분류로 이동하면 목적지의 마지막 순번에 배치된다

- **대상:** `CategoryItemService.update()`
- **시나리오:** 이동 요청은 같은 유형의 대상 대분류에 소분류를 마지막 순번으로 배치한다.
- **사전 조건:**
  - 소분류 ID 10은 대분류 1(`EXPENSE`)에 소속되어 있으며 `sortOrder=2`, `name="식자재"`, `isActive=false`, `isFavorite=true`다.
  - 목적지 대분류 2도 `EXPENSE` 유형이다.
  - 대분류 1과 2는 ID 오름차순으로 잠금 조회에 성공한다.
  - `CategoryItemRepository.findMaxSortOrder(2)`는 `3`을 반환한다.
  - 출발지 소분류 목록에는 ID 10이 포함되며, 순번 압축이 가능한 상태다.
  - `UpdateCategoryItemRequest(categoryId=2)`를 사용하며, `refresh()` 이후 소속도 1이다.
- **실행:** `CategoryItemService.update(10, body)` 호출.
- **기대 결과:**
  - 대분류 잠금 조회는 ID `1 → 2` 순서로 수행된다.
  - ID 10의 소속이 2로 변경되고 순번은 4가 된다.
  - ID, 이름, 활성 상태 및 즐겨찾기는 유지된다.
  - 반환된 `CategoryItemResponse`가 이동 후 상태와 일치한다.
  - 출발지 순번 압축을 적용하며 이동 시 `deferOrderConstraint()`를 호출한다.
- **테스트 형태:** `@Test`

### 2. 소분류가 없는 목적지로 이동하면 순번은 1부터 시작한다

- **대상:** `CategoryItemService.update()`
- **시나리오:** 대상 대분류가 비어 있어도 정상 이동하며 첫 순번을 부여한다.
- **사전 조건:**
  - ID 10이 대분류 1에 속해 있고, 동일 유형의 목적지 대분류 2에 소분류가 없다.
  - `findMaxSortOrder(2)`는 `null`을 반환한다.
  - `UpdateCategoryItemRequest(categoryId=2)`를 사용하며 `refresh()` 이후 소속은 1로 유지된다.
- **실행:** `CategoryItemService.update(10, body)` 호출.
- **기대 결과:**
  - 이동한 소분류의 `category.id=2`, `sortOrder=1`이다.
  - 변경하지 않은 속성이 보존된다.
  - 출발지에는 이동 대상이 남아 있지 않다.
- **테스트 형태:** `@Test`

### 3. 소분류 제거 후 남은 순번은 상대적 순서를 유지하며 압축된다

- **대상:** `CategoryItemReorderer.compact()`
- **시나리오:** 앞·중간·마지막 항목 또는 단일 항목을 제거해도 나머지 항목의 ID별 순번이 정확하다.
- **사전 조건:**
  - 입력 순서는 `(ID, sortOrder)` 쌍으로 전달한다.
  - 기본 입력: `[(10,1), (20,2), (30,3), (40,4)]`.
  - 단일 항목 입력: `[(10,1)]`.
- **실행:** 입력 조건별로 `CategoryItemReorderer.compact(pairs, removedItemId)` 호출.
- **기대 결과:**
  - 제거 대상 ID는 반환 Map에 포함되지 않는다.
  - 나머지 항목의 ID별 순번은 아래 표와 일치한다.
- **테스트 형태:** `@ParameterizedTest`

#### 입력 조건

| 입력 | 제거 ID | 기대 `Map<ID, sortOrder>` |
|---|---:|---|
| 기본 입력 | 10 | `{20=1, 30=2, 40=3}` |
| 기본 입력 | 20 | `{10=1, 30=2, 40=3}` |
| 기본 입력 | 40 | `{10=1, 20=2, 30=3}` |
| 단일 항목 입력 | 10 | `{}` |

### 4. 중간 소분류를 이동하면 출발지 형제 항목의 순번이 갱신된다

- **대상:** `CategoryItemService.update()`
- **시나리오:** 순번 압축 계산 결과가 실제 형제 엔티티 상태에 반영된다.
- **사전 조건:**
  - 출발지 대분류 1(`EXPENSE`)에 소분류 A(ID 10, 순번 1), B(ID 20, 순번 2), C(ID 30, 순번 3)가 있다.
  - B를 동일 유형의 대분류 2로 이동한다.
  - 목적지 최대 순번은 2이며, 최초/`refresh()` 이후 B의 소속은 1이다.
- **실행:** `CategoryItemService.update(20, UpdateCategoryItemRequest(categoryId=2))` 호출.
- **기대 결과:**
  - 출발지 A는 순번 1, C는 순번 2를 갖는다.
  - B는 대분류 2로 이동하고 순번 3을 갖는다.
  - 나머지 항목의 소속은 변경되지 않는다.
  - 소분류의 ID와 수정 요청에 없는 이름·활성 상태·즐겨찾기는 보존된다.
  - `deferOrderConstraint()`가 호출된다.
- **테스트 형태:** `@Test`

### 5. 잠금 대기 중 소속이 바뀌면 기존 소속을 기준으로 수정을 진행하지 않는다

- **대상:** `CategoryItemService.update()`
- **시나리오:** 최초 조회와 `refresh()` 이후 소속이 다르면 변경 충돌을 반환한다.
- **사전 조건:**
  - 최초 조회된 소분류 ID 10의 소속은 대분류 1이다.
  - 대분류 1에 대한 잠금 조회가 완료된 후 `EntityManager.refresh(item)` 호출 시 소분류의 소속이 대분류 2인 상태가 되도록 Mock을 구성한다.
  - 요청은 `UpdateCategoryItemRequest(name="변경명", isFavorite=true)`로 지정한다.
  - 동일한 Mock을 통한 소속 변경은 **테스트의 상태 전환 모형**일 뿐 실제 DB 경합 재현은 아니다.
- **실행:** `CategoryItemService.update(10, body)` 호출.
- **기대 결과:**
  - `GrimaultException`이 발생하고 `errorCode=ErrorCode.CATEGORY_ITEM_UPDATE_CONFLICT`다.
  - `refresh()` 이후 이름·즐겨찾기·순번 변경 로직 및 목적지 이동 로직이 수행되지 않는다.
  - 이동 대상 최대 순번 조회나 제약 지연 호출이 발생하지 않는다.
- **테스트 형태:** `@Test`

## 검증 범위

- 단위 테스트는 MockK 및 메모리 엔티티로 서비스 판단과 순번 계산 결과를 검증한다.
- 비관적 잠금의 실제 순서, PostgreSQL 동시 요청 직렬화, UNIQUE 제약 지연 검사 및 JPA Dirty Checking 결과는 단위 테스트에서 보증하지 않는다.
- 실제 잠금 경합과 교차 이동의 정합성은 별도로 수행한 수동 DB/HTTP 검증 결과를 따른다.
