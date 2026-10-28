# Category 기능 명세

> [!NOTE]  
> 목적: Grimault의 수입·지출 분류(Category / CategoryItem) 관리 기능과 API 동작 규약을 정의한다.

## 1. 범위

이 문서는 수입·지출 분류 자체의 관리 기능만 다룬다.

Category는 대분류와 소분류의 2단계 체계로 구성하며, 네이버 가계부에서 제공하는 분류 관리 및 거래 입력 시 분류 선택 기능을 기준으로 한다.

다음 기능은 Category와 연관되어 보이지만 이 문서의 범위에서 제외한다.

- 거래 내역의 생성 및 수정
    - 거래와 소분류 간 참조 관계 및 분류 유형의 정합성 원칙만 정의한다.
    - 실제 거래 입력·수정 API의 구체적인 요청 계약은 Transaction 도메인에서 별도 논의한다.
- 기존 거래의 일괄 재분류
    - `기타` 등의 포괄적 분류를 정리하거나 기존 거래를 일괄적으로 다른 분류에 연결하는 기능은 현재 제공하지 않는다.
- 사용자별 분류 체계
    - Grimault는 단일 사용자 프로젝트이므로 다중 사용자별 분류 소유권과 즐겨찾기 분리는 고려하지 않는다.

---

## 2. Category 도메인 규칙

### 2.1 분류 체계

Category는 다음 두 단계로 구성한다.

- `Category`: 대분류
- `CategoryItem`: 대분류에 종속되는 소분류

각 CategoryItem은 반드시 하나의 Category에 소속된다.

반면 Category는 CategoryItem 없이 단독으로 존재할 수 있다.

손익 분류를 결정하는 `type`은 Category에만 저장한다.

- `EXPENSE`: 지출
- `INCOME`: 수입

CategoryItem은 소속 Category를 통해 자신의 `type`을 결정한다.

즉, 다음 관계는 Category 도메인의 invariant로 취급한다.

> `CategoryItem -> Category -> CategoryType`

`type`은 대분류의 고정적인 성격으로 취급하며 생성 이후 변경할 수 없다.

수입 분류와 지출 분류는 서로 독립적인 체계로 유지한다.

### 2.2 거래 참조 관계

거래 원장(`ledger_entry`)은 Category가 아닌 CategoryItem을 참조한다.

- 대분류를 거래에 직접 연결할 수 없다.
- 신규 거래에는 현재 선택 가능한 소분류만 지정한다.
- 거래와 분류의 유형 정합성은 `INCOME` / `EXPENSE`를 기준으로 판단한다.
- 분류의 선택 가능 상태와 거래 유형의 정합성은 별개의 개념으로 취급한다.

기존 거래에 연결된 CategoryItem은 이후 활성 상태가 변경되어도 참조 관계를 유지한다.

Category와 CategoryItem의 이름 변경은 기존 거래 조회에도 반영한다.

CategoryItem의 소속 대분류를 변경한 경우에도 기존 거래 조회에서는 변경된 대분류를 사용한다.

따라서 거래 입력 당시의 분류 이름이나 계층 구조는 별도 이력으로 보존하지 않는다.

### 2.3 생명주기

Category와 CategoryItem은 각각 독립적인 생명주기를 가진다.

> 생성 -> 활성 상태로 사용 -> 필요 시 비활성 -> 필요 시 재활성

각 리소스는 독립적인 `isActive`를 가진다.

대분류의 활성 상태를 변경하더라도 소속된 소분류의 `isActive`는 변경하지 않는다.

반대로 소분류의 활성 상태 변경 역시 상위 대분류의 상태를 변경하지 않는다.

Category와 CategoryItem은 과거 거래의 참조 관계를 유지해야 하므로 삭제하지 않는다.

- 물리 삭제 미지원
- 논리 삭제(soft delete) 미지원
- `deleted`, `deletedAt` 등의 삭제 상태를 별도로 관리하지 않음

비활성 상태는 해당 분류의 현재 사용 여부를 나타내며, 기존 거래에서 해당 분류를 조회하거나 관리하는 것을 금지하지 않는다.

### 2.4 선택 가능 상태

CategoryItem의 실제 선택 가능 여부는 상위 Category와 자신의 활성 상태를 조합해 판단한다.

```text
isSelectable = category.isActive && categoryItem.isActive
```

`isSelectable`은 DB에 별도 저장하지 않는 파생 조건이다.

- 두 분류가 모두 활성 상태이면 선택 가능하다.
- 어느 한쪽이라도 비활성 상태이면 선택할 수 없다.
- 선택 가능 여부는 신규 거래 입력 시 분류 선택에 사용한다.
- 기존 거래에 연결된 분류의 정합성을 소급해서 변경하지 않는다.
- 거래 유형의 정합성은 `isSelectable`과 별개로 판단한다.

`isSelectable`은 API 응답 속성으로 제공하지 않는다.

목록 조회에서는 선택 가능한 항목만 반환하기 위한 `selectableOnly` 옵션을 사용한다.

### 2.5 이름 및 소속 관리

Category와 CategoryItem은 각각 이름을 변경할 수 있다.

CategoryItem은 소속 Category를 변경할 수 있다.

소속 이동은 다음 규칙을 따른다.

- 이동 전후 Category의 `type`이 같아야 한다.
- `INCOME`과 `EXPENSE` 사이의 이동은 허용하지 않는다.
- 이동한 CategoryItem의 ID는 유지한다.
- 기존 거래의 참조 관계는 유지한다.
- 이동 과정에서 Category 및 CategoryItem의 `isActive`는 변경하지 않는다.
- 활성·비활성 상태와 관계없이 소속 이동을 허용한다.

이름의 중복 제약은 다음과 같다.

| 리소스 | UNIQUE 제약 |
| --- | --- |
| Category | `UNIQUE(name)` |
| CategoryItem | `UNIQUE(category_id, name)` |

비활성 분류도 중복 검사에 포함한다.

---

## 3. API 구성

| 기능 | Endpoint | 비고 |
| --- | --- | --- |
| 전체 분류 계층 조회 | `GET /categories` | `type`, `selectableOnly` 필터 지원 |
| 단일 대분류 조회 | `GET /categories/{id}` | 비활성 대분류도 조회 가능 |
| 신규 대분류 등록 | `POST /categories` | 이름 및 유형 검증 |
| 대분류 정보 수정 | `PATCH /categories/{id}` | 이름 변경 |
| 대분류 순번 변경 | `PATCH /categories/{id}/order` | Dense Ordering |
| 대분류 활성화 | `POST /categories/{id}/activate` | payload 없음 |
| 대분류 비활성화 | `POST /categories/{id}/deactivate` | payload 없음 |
| 단일 소분류 조회 | `GET /category-items/{id}` | 비활성 소분류도 조회 가능 |
| 신규 소분류 등록 | `POST /categories/{id}/items` | 소속 대분류는 URI로 지정 |
| 소분류 정보 수정 | `PATCH /category-items/{id}` | 이름·소속·즐겨찾기 수정 |
| 소분류 순번 변경 | `PATCH /category-items/{id}/order` | 소속 대분류 내 순번 변경 |
| 소분류 활성화 | `POST /category-items/{id}/activate` | payload 없음 |
| 소분류 비활성화 | `POST /category-items/{id}/deactivate` | payload 없음 |
| 대분류·소분류 삭제 | 미지원 | 물리/논리 삭제 모두 미지원 |

---

## 4. 전체 분류 계층 조회

### `GET /categories`

Category 관리 화면과 거래 입력 시 분류 선택에 공통으로 사용하는 기본 목록 조회 API다.

별도의 거래 입력용 경량 분류 조회 API는 제공하지 않는다.

응답은 Category를 최상위 요소로 하고, 소속 CategoryItem 목록을 `items`에 중첩한 계층형 구조로 제공한다.

CategoryItem이 없는 Category도 전체 조회 결과에 포함한다.

### 4.1 분류 유형 필터

```text
type=INCOME
type=EXPENSE
```

`type`으로 수입 또는 지출 분류를 필터링할 수 있다.

규칙은 다음과 같다.

- `type`을 지정하지 않으면 수입·지출 전체를 조회한다.
- 단일 값만 허용한다.
- 여러 유형을 동시에 전달하는 요청은 지원하지 않는다.
- 유효한 값은 `INCOME`, `EXPENSE`다.

예를 들어 지출 분류만 필요한 경우 다음과 같이 요청한다.

```text
GET /categories?type=EXPENSE
```

### 4.2 선택 가능 분류 필터

```text
selectableOnly=true
selectableOnly=false
```

`selectableOnly`는 현재 선택 가능한 CategoryItem만 조회할지 지정하는 옵션이다.

규칙은 다음과 같다.

- 기본값은 `false`다.
- 미지정 또는 `false`이면 활성 상태와 관계없이 전체 분류를 조회한다.
- `true`이면 `isSelectable=true`인 CategoryItem만 반환한다.
- `true` 적용 후 CategoryItem이 하나도 남지 않는 Category는 결과에서 제외한다.
- `type` 필터와 함께 사용할 수 있다.

분류 관리 화면에서는 다음과 같이 조회한다.

```text
GET /categories
```

거래 입력 시 선택 가능한 지출 분류만 필요한 경우에는 다음과 같이 조회한다.

```text
GET /categories?type=EXPENSE&selectableOnly=true
```

`selectableOnly=false`는 선택 불가능한 분류만 조회하는 의미가 아니다.

이 옵션은 선택 가능한 항목만 필요하다는 조회 목적을 표현하며, 개별 분류의 활성 상태를 변경하지 않는다.

### 4.3 응답 모델

Category 응답은 다음 속성을 포함한다.

- `id`
- `type`
- `name`
- `isActive`
- `sortOrder`
- `items`

`items`에 포함된 CategoryItem 응답은 다음 속성을 포함한다.

- `id`
- `name`
- `isActive`
- `sortOrder`
- `isFavorite`

중첩된 CategoryItem은 상위 Category를 통해 소속 관계를 확인할 수 있으므로 별도의 `categoryId`를 포함하지 않는다.

`isSelectable`은 응답에 포함하지 않는다.

전체 조회와 필터 조회는 동일한 응답 구조를 사용한다.

### 4.4 검색

검색어 필터는 제공하지 않는다.

현재 분류 체계의 규모와 사용 목적을 고려하면 계층형 목록을 직접 탐색하는 방식으로 충분하다.

### 4.5 페이징

페이징은 제공하지 않는다.

Grimault는 단일 사용자 기준의 분류 데이터 집합을 관리하며, 전체 계층을 한 번에 조회하는 방식으로 사용한다.

### 4.6 정렬

클라이언트가 목록 조회 요청에서 별도의 정렬 기준을 지정하는 기능은 제공하지 않는다.

대신 분류 관리 기능에서 사용자가 지정한 `sortOrder`를 기본 정렬 기준으로 사용한다.

대분류와 소분류에 각각 다음 정렬을 적용한다.

```sql
ORDER BY sort_order ASC, id ASC
```

- 대분류는 전체 Category의 `sortOrder`에 따라 정렬한다.
- 소분류는 소속 Category 내부의 `sortOrder`에 따라 정렬한다.
- 활성 상태와 관계없이 동일한 정렬 기준을 사용한다.
- 필터 적용 시에도 남은 항목의 상대적 순서를 유지한다.

`id ASC`는 결과 순서를 안정적으로 유지하기 위한 보조 정렬 기준이다.

---

## 5. 단일 분류 조회

### `GET /categories/{id}`

단일 Category의 상세 정보를 조회한다.

- Category 자신의 속성을 반환한다.
- `id`, `type`, `name`, `isActive`, `sortOrder`를 포함한다.
- 소속 CategoryItem 목록은 포함하지 않는다.
- 비활성 Category도 조회할 수 있다.

### `GET /category-items/{id}`

단일 CategoryItem의 상세 정보를 조회한다.

- CategoryItem 자신의 속성을 반환한다.
- `id`, `categoryId`, `name`, `isActive`, `sortOrder`, `isFavorite`를 포함한다.
- 비활성 CategoryItem도 조회할 수 있다.
- 상위 Category의 활성 상태와 무관하게 조회할 수 있다.

단건 조회에서는 현재 활성 상태나 선택 가능 여부를 이유로 해당 분류를 조회 대상에서 제외하지 않는다.

---

## 6. 신규 분류 등록

### 6.1 대분류 등록

### `POST /categories`

다음 값은 필수다.

- `name`
- `type`

`type`은 `INCOME` 또는 `EXPENSE` 중 하나여야 한다.

이름이 누락되거나 공백 문자열이면 요청을 실패 처리한다.

동일한 이름의 Category가 이미 존재하면 중복 오류로 처리한다.

신규 Category는 다음 기본 속성을 갖는다.

- `isActive = true`
- `sortOrder = MAX(sortOrder) + 1`

기존 Category가 없으면 최초 순번은 `1`이다.

생성 요청에서는 `sortOrder`를 받지 않는다.

### 6.2 소분류 등록

### `POST /categories/{id}/items`

Request Body의 필수값은 다음과 같다.

- `name`

소속 Category는 URI의 `{id}`로 지정한다.

- 지정된 Category가 존재해야 한다.
- CategoryItem의 `type`은 소속 Category를 통해 결정한다.
- 같은 Category 내부에서 이름이 중복되면 요청을 실패 처리한다.
- 서로 다른 Category 사이에서는 같은 이름을 사용할 수 있다.

신규 CategoryItem은 다음 기본 속성을 갖는다.

- `isActive = true`
- `isFavorite = false`
- `sortOrder = MAX(sortOrder) + 1` (소속 Category 내부)

소속 Category에 기존 CategoryItem이 없으면 최초 순번은 `1`이다.

생성 요청에서는 `sortOrder`, `isFavorite`, `isActive`를 받지 않는다.

### 6.3 최초 순번 정책

대분류와 소분류 모두 신규 생성 시 해당 정렬 범위의 마지막에 추가한다.

순번 계산에는 비활성 분류도 포함한다.

생성과 원하는 위치로의 순번 변경은 별개의 비즈니스 행위로 취급한다.

따라서 생성 API는 특정 위치에 항목을 삽입하기 위한 순번 재정렬을 수행하지 않는다.

---

## 7. 분류 정보 수정

분류의 모든 속성을 자유롭게 수정할 수 있도록 하지 않는다.

### 7.1 대분류 수정

### `PATCH /categories/{id}`

수정 가능 여부는 다음과 같다.

| 속성 | 수정 |
| --- | --- |
| `name` | 가능 |
| `type` | 불가 |
| `isActive` | 별도 API |
| `sortOrder` | 별도 API |

`type`은 Category의 고정적인 도메인 속성으로 보고 생성 이후 변경하지 않는다.

반면 `name`은 화면 표시 및 기존 거래의 분류 인식을 위한 관리 속성으로 보고 변경을 허용한다.

비활성 Category도 이름을 변경할 수 있다.

### 7.2 소분류 수정

### `PATCH /category-items/{id}`

수정 가능 여부는 다음과 같다.

| 속성 | 수정 |
| --- | --- |
| `name` | 가능 |
| `categoryId` | 가능 |
| `isFavorite` | 가능 |
| `isActive` | 별도 API |
| `sortOrder` | 별도 API |

CategoryItem의 이름, 소속 대분류, 즐겨찾기 지정 여부를 변경할 수 있다.

비활성 CategoryItem에도 동일한 수정 규칙을 적용한다.

### 7.3 소속 대분류 이동

`categoryId`를 변경하면 CategoryItem의 소속 대분류가 변경된다.

- 이동 대상 Category가 존재해야 한다.
- 이동 전후 Category의 `type`이 같아야 한다.
- 이동 대상 Category 내부에서 이름 중복이 발생해서는 안 된다.
- CategoryItem의 ID는 변경하지 않는다.
- 기존 거래의 CategoryItem 참조 관계는 유지한다.
- 이동 과정에서 `isActive`, `isFavorite`는 변경하지 않는다.
- 대분류와 소분류의 활성 상태에 관계없이 이동을 허용한다.

이동한 CategoryItem은 대상 Category의 마지막 순번에 배치한다.

출발 Category에서는 해당 항목이 빠진 위치 이후의 순번을 당겨 연속성을 유지한다.

소속 이동과 이동 이후의 사용자 지정 순번 배치는 별개의 작업으로 취급한다.

출발지와 대상지의 순번 변경은 하나의 트랜잭션으로 처리한다.

### 7.4 즐겨찾기 지정

`isFavorite`는 CategoryItem의 일반 수정 속성으로 취급한다.

```json
{
  "isFavorite": true
}
```

- `true`: 자주 쓰는 분류로 등록
- `false`: 자주 쓰는 분류에서 해제
- 즐겨찾기는 CategoryItem에만 적용한다.
- 분류의 활성 상태와 독립적으로 유지한다.
- 비활성 분류도 즐겨찾기 지정 및 해제가 가능하다.
- 별도의 즐겨찾기 순번은 관리하지 않는다.

즐겨찾기 목록은 기존 Category 및 CategoryItem의 `sortOrder`를 따른다.

즐겨찾기 변경을 위한 별도 엔드포인트는 제공하지 않는다.

---

## 8. 활성화 / 비활성화

Category 및 CategoryItem의 활성 상태 변경은 일반 정보 수정 API와 분리한다.

### 8.1 대분류 활성 상태 변경

활성화:

```http
POST /categories/{id}/activate
```

비활성화:

```http
POST /categories/{id}/deactivate
```

두 API는 다음 규칙을 따른다.

- request payload를 받지 않는다.
- 해당 Category의 `isActive`만 변경한다.
- 소속 CategoryItem의 `isActive`는 변경하지 않는다.
- 이름, 순번 등 다른 속성은 변경하지 않는다.
- 동일 상태로 반복 호출해도 최종 상태가 동일한 멱등 연산으로 취급한다.

대분류가 비활성화되면 하위 CategoryItem은 자신의 `isActive`가 `true`여도 선택 불가능한 상태가 된다.

대분류를 다시 활성화하면 하위 CategoryItem의 기존 활성 상태에 따라 선택 가능 여부가 결정된다.

### 8.2 소분류 활성 상태 변경

활성화:

```http
POST /category-items/{id}/activate
```

비활성화:

```http
POST /category-items/{id}/deactivate
```

두 API는 다음 규칙을 따른다.

- request payload를 받지 않는다.
- 해당 CategoryItem의 `isActive`만 변경한다.
- 소속 Category의 `isActive`는 변경하지 않는다.
- 다른 CategoryItem의 상태에도 영향을 주지 않는다.
- 이름, 소속, 순번, 즐겨찾기 상태는 변경하지 않는다.
- 동일 상태로 반복 호출해도 최종 상태가 동일한 멱등 연산으로 취급한다.

상위 Category가 비활성 상태이더라도 CategoryItem의 활성화·비활성화 요청은 정상 처리한다.

예를 들어 상위 Category가 비활성이고 CategoryItem을 활성화한 경우:

- Category의 `isActive`는 `false`로 유지한다.
- CategoryItem의 `isActive`는 `true`로 변경한다.
- 실제 `isSelectable`은 여전히 `false`다.
- 정상적인 활성화 요청은 `200 OK`로 처리한다.

활성 상태 변경을 별도 command endpoint로 분리하는 이유는 `isActive`를 단순 정보 수정 필드가 아니라 분류의 사용 상태 전환으로 취급하기 때문이다.

---

## 9. 분류 순번 관리

대분류와 소분류는 사용자 지정 순서를 지원한다.

순번 관리에는 Dense Ordering을 적용한다.

### 9.1 순번의 정합성

각 정렬 범위에 N개의 분류가 존재하면 `sortOrder`는 다음 조건을 만족해야 한다.

```text
1, 2, 3, ..., N
```

- 순번은 1부터 시작한다.
- 중복을 허용하지 않는다.
- 중간에 공백이 없는 연속된 정수를 유지한다.
- 비활성 분류도 정렬 범위에 포함한다.
- 활성화·비활성화는 저장된 순번을 변경하지 않는다.

대분류는 전체 Category를 하나의 정렬 범위로 관리한다.

소분류는 각 Category 내부에서 독립된 정렬 범위를 갖는다.

### 9.2 대분류 순번 변경

### `PATCH /categories/{id}/order`

변경 대상은 URI에서 식별한다.

Request Body는 목표 순번을 전달한다.

```json
{
  "sortOrder": 2
}
```

서버는 현재 순번과 목표 순번을 비교하고, 영향을 받는 구간을 재정렬한다.

- 앞쪽으로 이동하면 중간 구간의 순번을 1씩 증가시킨다.
- 뒤쪽으로 이동하면 중간 구간의 순번을 1씩 감소시킨다.
- 동일 순번을 요청하면 순서를 변경하지 않는다.
- 목표 순번은 현재 정렬 범위의 `1..N` 이내여야 한다.

대분류의 순번 변경은 하위 CategoryItem의 `sortOrder`를 변경하지 않는다.

### 9.3 소분류 순번 변경

### `PATCH /category-items/{id}/order`

Request Body는 목표 순번을 전달한다.

```json
{
  "sortOrder": 2
}
```

대분류와 동일한 Dense Ordering 규칙을 적용한다.

단, 재정렬 범위는 해당 CategoryItem이 소속된 Category 내부로 제한한다.

- 다른 Category에 속한 CategoryItem의 순번에는 영향을 주지 않는다.
- 비활성 CategoryItem도 재정렬 대상에 포함한다.
- 상위 Category가 비활성이어도 순번 변경 API의 실행을 허용한다.

### 9.4 DB 정합성 제약

순번 중복은 DB UNIQUE 제약으로 방지한다.

| 리소스 | UNIQUE 제약 |
| --- | --- |
| Category | `UNIQUE(sort_order)` |
| CategoryItem | `UNIQUE(category_id, sort_order)` |

UNIQUE 제약은 중복을 방지하며, 공백 없는 연속성은 애플리케이션의 재정렬 로직에서 보장한다.

PostgreSQL에서는 순번 재배치 중 일시적으로 발생할 수 있는 유일성 충돌을 처리하기 위해 `DEFERRABLE INITIALLY IMMEDIATE` 제약을 활용한다.

### 9.5 트랜잭션 및 동시성

순번 변경은 정렬 범위 단위의 Pessimistic Lock으로 동시성을 제어한다.

- 같은 정렬 범위에 대한 변경 요청은 직렬화한다.
- 서로 다른 Category 내부의 소분류 순번은 독립적으로 관리한다.
- 소분류 정렬에는 상위 Category를 잠금 기준으로 사용한다.
- 대분류 정렬에는 전체 대분류 순번을 보호하는 공통 잠금 기준을 사용한다.
- 소분류 소속 이동 시 출발지와 대상지의 잠금 획득 순서를 일관되게 유지한다.

잠금 획득, 현재 순번 조회, 순번 계산 및 갱신은 하나의 트랜잭션으로 처리한다.

### 9.6 Bulk Update 및 영속성 컨텍스트

순번 변경 시 영향받는 구간은 범위 기반 Bulk Update로 갱신한다.

개별 엔티티에 대한 UPDATE를 반복하기보다, 이동 구간을 하나의 Bulk Update로 처리한다.

JPA 영속성 컨텍스트와 실제 DB 상태의 불일치를 방지하기 위해 다음 실행 순서를 적용한다.

```text
Flush
  -> JPQL Bulk Update
  -> Clear
```

- `Flush`: 기존 영속성 컨텍스트의 미반영 변경사항을 DB에 반영
- `JPQL Bulk Update`: 순번 변경 대상 구간을 DB에서 직접 갱신
- `Clear`: Bulk Update 이후 기존 영속 엔티티의 상태가 재사용되지 않도록 초기화

이 처리는 순번 변경 트랜잭션 내부에서 수행한다.

---

## 10. 검증 및 예외 처리

Category API는 기존 Account API의 예외 처리 기준과 일관성을 유지한다.

### 10.1 요청 검증

다음 조건을 검증한다.

- 생성 요청의 필수값 누락 여부
- 분류 이름의 공백 문자열 여부
- `type`의 유효성
- 대분류·소분류의 이름 중복
- 요청 대상 리소스의 존재 여부
- 소분류 이동 시 `type` 일치 여부
- 수정이 허용되지 않은 속성의 변경 여부
- 순번 변경 시 목표 순번의 유효 범위

이름 중복 검사는 활성 상태와 관계없이 적용한다.

### 10.2 예외 응답

| 오류 상황 | HTTP Status |
| --- | --- |
| 잘못된 요청 형식 또는 필수값 누락 | `400 Bad Request` |
| 잘못된 분류 유형 | `400 Bad Request` |
| 허용되지 않은 속성 변경 | `400 Bad Request` |
| 소분류 이동 시 유형 불일치 | `400 Bad Request` |
| 유효하지 않은 목표 순번 | `400 Bad Request` |
| 존재하지 않는 대분류·소분류 | `404 Not Found` |
| 대분류 이름 중복 | `409 Conflict` |
| 동일 대분류 내 소분류 이름 중복 | `409 Conflict` |

이름 중복에 대한 `409 Conflict`는 기존 Account API의 UNIQUE 제약 위반 처리와 동일한 기준을 따른다.

오류 응답의 기본 구조는 프로젝트의 공통 `ErrorResponse`를 사용한다.

별도의 Category 전용 오류 응답 형식은 정의하지 않는다.

---

## 11. 삭제 정책

Category와 CategoryItem의 삭제 API는 제공하지 않는다.

```text
DELETE /categories/{id}      // 미지원
DELETE /category-items/{id}   // 미지원
```

삭제를 두지 않는 이유는 다음과 같다.

- CategoryItem은 과거 거래 내역과 지속적으로 참조 관계를 유지해야 한다.
- 분류의 이름이나 소속 변경도 기존 거래 조회에 반영된다.
- 더 이상 신규 거래에서 사용하지 않는 분류는 비활성 상태로 관리한다.
- 삭제와 비활성 상태를 동시에 관리하면 조회 및 참조 관계의 의미가 불필요하게 복잡해진다.

따라서 생성된 분류는 데이터상 계속 존재하며, 현재 사용 여부는 활성·비활성 상태로 관리한다.

---

## 12. 현재 제공하지 않는 조회 및 관리 기능

다음 기능은 현재 Category API에서 제공하지 않는다.

- 검색어 조회
- 페이징
- 조회 요청별 사용자 지정 정렬 기준
- 다중 `type` 필터
- `isSelectable=false`인 항목만을 추출하는 역방향 필터
- 별도의 거래 입력용 경량 Category 목록 API
- 사용자별 분류 체계 및 즐겨찾기 관리
- 즐겨찾기 전용 순번 변경
- 분류의 물리 삭제 또는 논리 삭제
- 기존 거래의 일괄 재분류
- 거래 입력 당시의 분류 이름 및 계층 이력 보존

현재 요구사항에서 필요성이 확인되지 않은 기능은 선제적으로 추가하지 않고, 실제 운용 과정에서 필요가 발생할 때 검토한다.