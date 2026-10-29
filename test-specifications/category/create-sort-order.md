# 대분류 생성 시 최초 순번 정책 검증 (Category Creation Sort Order)

## 개요

신규 대분류는 전체 대분류 정렬 범위의 마지막 순번에 추가한다.

기존 대분류가 없으면 `sortOrder = 1`로 시작하고, 기존 대분류가 있다면 활성 상태와 관계없이 가장 큰 순번의 다음 값을 사용한다.

신규 대분류의 `isActive`는 `true`로 설정한다.

## 테스트 대상

- `CategoryService.create()`

## 테스트 시나리오

### 1. 신규 대분류는 전체 정렬 범위의 마지막 순번에 추가된다

- **대상:** `CategoryService.create()`
- **시나리오:** 기존 대분류의 최대 순번에 따라 신규 대분류의 순번을 결정하고 활성 상태로 생성한다.
- **사전 조건:**
  - `CreateCategoryRequest.name = "테스트 대분류"`
  - `CreateCategoryRequest.type = "EXPENSE"`
  - `CategoryOrderLockRepository`, `CategoryRepository`는 Mock으로 구성한다.
  - `CategoryRepository.findMaxSortOrder()`는 입력 조건에 따른 값을 반환한다.
  - `CategoryRepository.save()`는 전달받은 Category에 ID가 부여된 저장 결과를 반환하도록 설정한다.
- **실행:** `CategoryService.create(body)` 호출
- **기대 결과:**
  - `CategoryOrderLockRepository.lock()`이 호출된다.
  - 저장되는 Category의 `sortOrder`가 입력 조건별 기대 순번과 일치한다.
  - 저장되는 Category의 `isActive`가 `true`다.
  - `CategoryResponse`에 저장 결과의 ID, 유형, 이름, 활성 상태, 순번이 반영된다.
- **테스트 형태:** `@ParameterizedTest`

#### 입력 조건

| `findMaxSortOrder()` 반환값 | 기대 신규 `sortOrder` |
|---|---|
| `null` | 1 |
| `1` | 2 |
| `5` | 6 |

## 검증 범위

- 대분류 전체 정렬 범위의 최대 순번을 조회하는 Repository 메서드를 사용한다.
- 잠금 획득 후 최대 순번 조회, 저장 순서로 호출되는지 확인한다.
- 실제 PostgreSQL Advisory Lock의 동작 및 트랜잭션 경계는 검증하지 않는다.
- 비활성 대분류의 순번 포함 여부는 Repository 쿼리의 책임이므로 이 단위 테스트에서 직접 검증하지 않는다.