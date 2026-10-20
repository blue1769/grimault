# Account 기능 명세

> 문서 상태: 검토용  
> 목적: Grimault의 자산 계정(Account) 관리 기능과 API 동작 규약을 정의한다.

## 1. 범위

이 문서는 자산 계정 자체의 관리 기능만 다룬다.

다음 기능은 Account와 연관되어 보이지만 이 문서의 범위에서 제외한다.

- 시작금액 설정
  - 계정 속성으로 저장하지 않는다.
  - 기초잔액을 구성하는 거래(Transaction) 관점에서 추후 별도 논의한다.
- 현재 잔액 조회
  - Account의 저장 속성이 아니라 거래 원장으로부터 계산되는 파생 값으로 본다.
  - 계정 관리 조회가 아닌 자산 현황 조회 기능에서 추후 별도 논의한다.
- 지출, 수입, 자산 이동, 카드대금 납부, 대출 상환 등 거래 입력 방식
  - Transaction 도메인 기능 설계에서 다룬다.

---

## 2. Account 도메인 규칙

### 2.1 분류 체계

Account는 `type`과 `subtype`을 가진다.

- `type`: 회계적 대분류
- `subtype`: 해당 대분류에 종속되는 세부 분류

`subtype`은 항상 하나의 `type`에만 속하며, `subtype`이 결정되면 `type`도 단일하게 결정된다.

즉 다음 관계는 Account 도메인의 invariant로 취급한다.

> `AccountSubtype -> AccountType`

단, `subtype` 필터가 `type` 필터의 모든 조회 의미를 대체한다는 뜻은 아니다.  
현재 요구사항에서는 상위 `type` 기준 조회가 필요하지 않으므로 별도 `type` 필터를 제공하지 않는다.

### 2.2 생명주기

Account의 생명주기는 다음과 같이 단순하게 유지한다.

> 생성 -> 활성 상태로 사용 -> 필요 시 비활성 -> 필요 시 재활성

Account는 과거 거래의 참조 대상이므로 삭제하지 않는다.

- 물리 삭제 미지원
- 논리 삭제(soft delete) 미지원
- `deleted`, `deletedAt` 등의 삭제 상태를 별도로 관리하지 않음

비활성의 의미는 다음과 같다.

> 더 이상 현재의 신규 거래 입력 등에 사용하지 않는 계정

비활성은 과거 거래나 계정의 역사적 존재를 제거하지 않는다.

---

## 3. API 구성

| 기능 | Endpoint | 비고 |
| --- | --- | --- |
| 전체 자산 계정 조회 | `GET /accounts` | 필터 지원 |
| 단일 자산 계정 조회 | `GET /accounts/{id}` | 비활성 계정도 조회 가능 |
| 신규 자산 계정 등록 | `POST /accounts` | 필수값 및 분류 정합성 검증 |
| 자산 계정 정보 수정 | `PUT /accounts/{id}` | 수정 가능 필드 제한 |
| 자산 계정 활성화 | `PUT /accounts/{id}/activate` | payload 없음 |
| 자산 계정 비활성화 | `PUT /accounts/{id}/deactivate` | payload 없음 |
| 자산 계정 삭제 | 미지원 | 물리/논리 삭제 모두 미지원 |

---

## 4. 전체 자산 계정 조회

### `GET /accounts`

Account 관리 화면과 거래 입력 시 계정 선택에 공통으로 사용하는 기본 목록 조회 API다.

별도의 계정 선택용 경량 API는 제공하지 않는다.

### 4.1 활성 상태 필터

```text
isActive=true
isActive=false
```

`isActive` 값으로 활성/비활성 상태를 필터링할 수 있다.

거래 입력 등 현재 사용 가능한 계정만 필요한 UI에서는 다음과 같이 사용할 수 있다.

```text
GET /accounts?isActive=true
```

### 4.2 계정 속성 필터

세부 분류 기준 필터의 외부 API 명칭은 `subtype` 대신 `kinds`를 사용한다.

```text
GET /accounts?kinds=BANK,CASH,PREPAID
```

규칙은 다음과 같다.

- 하나 이상의 세부 분류를 동시에 요청할 수 있다.
- 여러 값은 OR 조건으로 처리한다.
  - 위 예시는 `BANK OR CASH OR PREPAID`
- 요청 값은 case-insensitive로 처리한다.
  - `BANK`, `bank`, `Bank`는 동일하게 해석한다.
- API 문서 및 응답의 canonical value는 `UPPER_SNAKE_CASE`를 사용한다.
- `type` 필터는 제공하지 않는다.

### 4.3 검색

검색어 필터는 제공하지 않는다.

Account는 단일 사용자 기준 데이터 수와 속성 볼륨이 작고, 현재 요구사항에서는 검색 기능의 실익이 낮다.

### 4.4 페이징

페이징은 제공하지 않는다.

Account는 단일 사용자 기준으로 전체 목록을 한 번에 조회해도 충분히 작은 데이터 집합으로 본다.

### 4.5 정렬

클라이언트가 별도 정렬 기준을 지정하는 기능은 제공하지 않는다.

모든 조회 결과에는 다음 기본 정렬을 적용한다.

```sql
ORDER BY name ASC, id ASC
```

`id ASC`는 동일한 이름을 가진 계정 간 결과 순서를 안정적으로 유지하기 위한 보조 정렬 기준이다.

활성 상태 또는 `kinds` 필터를 적용한 경우에도 동일한 정렬 규칙을 유지한다.

---

## 5. 단일 자산 계정 조회

### `GET /accounts/{id}`

단일 Account의 상세 정보를 조회한다.

- 공통 Account 정보를 반환한다.
- 비활성 Account도 조회할 수 있다.
- 신용카드 Account인 경우 `creditCardMetadata`를 함께 반환한다.

신용카드 여부는 `creditCardMetadata != null`인 상태와 연결하여 표현한다.

---

## 6. 신규 자산 계정 등록

### `POST /accounts`

### 6.1 공통 필수 정보

다음 값은 필수다.

- `name`
- `type`
- `subtype`

필수 정보가 누락되면 요청을 실패 처리한다.

### 6.2 분류 정합성

`type`과 `subtype`은 정의된 종속 관계를 만족해야 한다.

예를 들어 특정 `subtype`이 속하지 않는 `type`과 조합되어 요청되면 정합성 오류로 처리한다.

### 6.3 신용카드

`subtype = CREDIT_CARD`인 경우 `creditCardMetadata`는 필수다.

신용카드 등록 요청에 메타데이터가 포함되지 않으면 요청을 실패 처리한다.

---

## 7. 자산 계정 정보 수정

### `PUT /accounts/{id}`

Account의 모든 속성을 자유롭게 수정할 수 있도록 하지 않는다.

### 7.1 활성 Account

수정 가능 여부는 다음과 같다.

| 속성 | 수정 |
| --- | --- |
| `name` | 가능 |
| `type` | 불가 |
| `subtype` | 불가 |
| `creditCardMetadata.issuer` | 불가 |
| `creditCardMetadata.settlementDay` | 가능 |
| `creditCardMetadata.billingPeriod` | 가능 |

`type`과 `subtype`은 Account의 정체성에 해당하는 값으로 보고 생성 이후 변경하지 않는다.

신용카드의 `issuer` 역시 현재 모델에서 계정의 고정적인 식별 정보에 가깝게 취급하므로 변경하지 않는다.

반면 결제일과 이용기간은 실제 사용 과정에서 변경될 수 있는 운영 정보로 보고 수정 가능하게 한다.

### 7.2 비활성 Account

비활성 Account는 `name`만 수정할 수 있다.

신용카드의 결제일/이용기간을 포함한 나머지 속성은 비활성 상태에서 변경하지 않는다.

---

## 8. 활성화 / 비활성화

Account의 활성 상태 변경은 일반 정보 수정 API와 분리한다.

### 활성화

```http
PUT /accounts/{id}/activate
```

### 비활성화

```http
PUT /accounts/{id}/deactivate
```

두 API는 다음 규칙을 따른다.

- request payload를 받지 않는다.
- Account의 활성 상태만 변경한다.
- 이름이나 카드 메타데이터 등 다른 속성은 함께 수정하지 않는다.
- 동일 상태로 반복 호출해도 최종 상태가 동일한 멱등 연산으로 취급한다.

활성/비활성 전환을 별도 command endpoint로 분리하는 이유는 `isActive`를 단순 편집 필드가 아니라 Account의 생명주기 상태 전환으로 취급하기 때문이다.

---

## 9. 삭제 정책

Account 삭제 API는 제공하지 않는다.

```text
DELETE /accounts/{id}  // 미지원
```

삭제를 두지 않는 이유는 다음과 같다.

- Account는 과거 거래 내역과 지속적으로 관계를 유지해야 한다.
- 더 이상 사용하지 않는 계정은 비활성 상태만으로 충분히 표현할 수 있다.
- 삭제와 비활성 상태를 동시에 관리하면 두 상태의 의미와 조회 규칙이 불필요하게 복잡해진다.
- 삭제를 통해 얻는 기능적 이점이 현재 요구사항에는 없다.

따라서 Account는 한 번 생성되면 데이터상 계속 존재하며, 사용 여부는 활성/비활성 상태로만 관리한다.

---

## 10. 현재 제공하지 않는 조회 기능

다음 기능은 현재 `GET /accounts`에 추가하지 않는다.

- 검색어 조회
- 페이징
- 사용자 지정 정렬
- `type` 필터
- 거래 입력용 별도 경량 Account 목록 API
- 현재 잔액 포함 조회

현재 요구사항에서 필요성이 확인되지 않은 기능은 선제적으로 추가하지 않고, 실제 운용 과정에서 필요가 발생할 때 검토한다.
