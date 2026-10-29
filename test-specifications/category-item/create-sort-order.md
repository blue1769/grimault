# 소분류 생성 시 최초 순번 정책 검증 (Category Item Creation Sort Order)

## 개요

신규 소분류는 소속 대분류 내부의 마지막 순번에 추가한다.

소속 대분류에 기존 소분류가 없으면 `sortOrder = 1`로 시작하고, 기존 소분류가 있다면 활성 상태와 관계없이 해당 대분류의 최대 순번 다음 값을 사용한다.

신규 소분류는 `isActive = true`, `isFavorite = false`로 생성한다.

## 테스트 대상

- `CategoryItemService.create()`

## 테스트 시나리오

### 1. 신규 소분류는 소속 대분류의 마지막 순번에 추가된다

- **대상:** `CategoryItemService.create()`
- **시나리오:** 소속 대분류의 최대 순번에 따라 신규 소분류의 순번을 결정하고 기본 상태로 생성한다.
- **사전 조건:**
    - `categoryId = 1`
    - `CreateCategoryItemRequest.name = "테스트 소분류"`
    - `CategoryRepository`, `CategoryItemRepository`는 Mock으로 구성한다.
    - `CategoryRepository.findByIdForUpdate(1)`은 존재하는 Category를 반환한다.
    - `CategoryItemRepository.findMaxSortOrder(1)`은 입력 조건에 따른 값을 반환한다.
    - `CategoryItemRepository.save()`는 전달받은 CategoryItem에 ID가 부여된 저장 결과를 반환하도록 설정한다.
- **실행:** `CategoryItemService.create(categoryId, body)` 호출
- **기대 결과:**
    - `CategoryRepository.findByIdForUpdate(1)`을 통해 소속 대분류를 조회한다.
    - 최대 순번 조회 시 `categoryId = 1`이 전달된다.
    - 저장되는 CategoryItem의 `category.id`가 `1`이다.
    - 저장되는 CategoryItem의 `sortOrder`가 입력 조건별 기대 순번과 일치한다.
    - 저장되는 CategoryItem의 `isActive`는 `true`, `isFavorite`는 `false`다.
    - `CategoryItemResponse`에 저장 결과의 ID, 대분류 ID, 이름, 활성 상태, 순번, 즐겨찾기 상태가 반영된다.
    - 부모 행 잠금 조회 → 최대 순번 조회 → 저장 순서로 호출된다.
- **테스트 형태:** `@ParameterizedTest`

#### 입력 조건

| `findMaxSortOrder(1)` 반환값 | 기대 신규 `sortOrder` |
|---|---|
| `null` | 1 |
| `1` | 2 |
| `5` | 6 |

## 검증 범위

- 소속 대분류 ID를 기준으로 최대 순번을 조회하도록 호출하는지 검증한다.
- 실제 Repository의 조회 조건, 비활성 소분류 포함 여부 및 PostgreSQL 행 잠금 동작은 검증하지 않는다.
- 실제 DB 트랜잭션과 동시 요청 직렬화는 검증하지 않는다.