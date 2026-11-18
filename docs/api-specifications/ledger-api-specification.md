# Ledger 기능 명세

> [!NOTE]
> Grimault의 거래(Transaction) 및 분개(LedgerEntry)에 기반한 원장 등록·수정·삭제·조회·집계 기능과 API 동작 규약을 정의한다.

## 1. 범위

Ledger는 사용자가 기록한 금융 거래를 비즈니스 정보로 관리하고, 각 거래로 인한 자산·부채·손익·자본의 증감을 분개로 기록한다.

- 거래 등록: 일반 소비, 수입, 계정 간 가치 이전·채무 정산, 환불
- 거래 수정 및 삭제
- 거래 목록·단건 조회와 검색
- 손익 집계 및 금융 계정별 누적 잔액 조회

다음 사항은 이 문서의 일반 거래 등록 범위에서 제외한다.

- `OPENING_BALANCE` 생성·수정: Account 도메인의 금융 계정 등록·수정 기능에서 처리
- 거래 일괄 등록: 실제 거래 입력 UI 설계 단계에서 도입 여부 검토
- 통합 보고서: 실제 화면 요구사항 확인 후 검토

Grimault는 단일 사용자 가계부이며, 실시간 자동 수집보다 사용자가 하루 또는 며칠간의 거래를 복기해 직접 기록하는 경험을 우선한다.

---

## 2. Ledger 도메인 규칙

### 2.1 거래 및 분개 구조

- `Transaction`: 사용자가 기록한 금융 거래의 헤더. 거래 유형, 거래일, 거래처·상세 내역 등 비즈니스 정보를 가진다.
- `LedgerEntry`: 거래에 종속된 분개 라인. 금융 계정 또는 손익 분류 등의 변동을 나타낸다.
- 하나의 `Transaction`은 복수의 `LedgerEntry`를 가진다.
- 금융 계정은 `Account`, 손익 분류는 `CategoryItem`을 참조한다.
- 금액은 `NUMERIC(15,2)`로 저장하고 분개 증감 방향은 부호로 구분한다.

모든 거래는 다음 불변식을 만족해야 한다.

```text
SUM(ledger_entry.amount) = 0  // transaction 단위
```

거래 헤더와 분개는 동일한 DB 트랜잭션 안에서 원자적으로 생성·갱신한다.

### 2.2 서버 주도 자동 분개

- 클라이언트는 거래 유형과 실제 거래에 대한 비즈니스 정보를 전달한다.
- 클라이언트가 `LedgerEntry` 목록이나 회계적 부호를 직접 전달하지 않는다.
- 서버가 유형 및 금융 계정·손익 분류의 속성을 해석하여 분개를 생성한다.
- 입력 금액은 절대적 크기이며, 분개 부호는 서버가 결정한다.
- 거래 유형은 분개 조합에서 추론하는 값이 아니라, 거래의 비즈니스적 의미를 나타내는 독립 속성이다.

### 2.3 거래 유형

| `Transaction.type` | 의미 | 대표 사례 |
| --- | --- | --- |
| `SPENDING` | 재화·서비스 이용 등에 대한 지출 | 식비, 세금, 수수료 |
| `INCOME_RECEIPT` | 경제적 수익의 발생 및 수령 | 급여, 이자, 배당, 용돈 |
| `TRANSFER` | 금융 계정 간 가치 이전 또는 채무 정산 | 계좌 이체, 카드 대금 상환, 선불 충전, 대출 원금 상환 |
| `REFUND` | 이전 지출 금액의 반환 | 구매 취소, 전액·부분 환불 |
| `OPENING_BALANCE` | 금융 계정의 원장 시작 잔액 설정 | 신규 관리 계정의 시작 잔액 |

`TRANSFER`는 참여 계정의 종류에 따라 여러 금융 행위를 포괄한다. `CARD_REPAYMENT`, `PREPAID_TOP_UP` 등의 별도 거래 유형을 두지 않는다.

### 2.4 금융 계정·손익 분류의 사용 조건

- 신규 일반 거래는 거래 유형에서 허용하는 **활성 금융 계정**만 참조한다.
- 손익 분류가 필요한 거래는 `CategoryItem`을 지정하며, 상위 `Category`와 자신의 활성 상태가 모두 참인 선택 가능 항목이어야 한다 (`selectableOnly = true`에 해당).
- 손익 분류의 `INCOME` / `EXPENSE` 유형은 거래 유형과 일치해야 한다.
- 일반 거래의 거래일은 **참여하는 모든 금융 계정의 `Account.openingDate` 이상**이어야 한다.
- 거래일에는 별도의 과거·현재·미래 제한을 두지 않는다.

### 2.5 기초 잔액 (`OPENING_BALANCE`)

- 일반 거래 등록·수정·삭제 API의 대상이 아니다. Account 도메인에서 생성·수정한다.
- 허용 계정: `ASSET`의 `BANK`, `CASH`, `PREPAID`; `LIABILITY`의 `CREDIT_CARD`, `LOAN`.
- 상대 분개는 `EQUITY: CAPITAL`을 사용한다.
- `ASSET`: 대상 계정 `+amount`, `CAPITAL` `-amount`.
- `LIABILITY`: 대상 계정 `-amount`, `CAPITAL` `+amount`.
- 초기 잔액이 0원이면 거래를 만들지 않으며, 금융 계정당 기초 잔액 거래는 최대 1건이다.
- `Account.openingDate`와 해당 초기화 거래의 거래일은 일치한다. 계정 원장 시작일은 수정할 수 없다.
- 상세 내역은 시스템이 `시작 잔액`으로 생성한다.
- 계정 생성과 해당 초기화 거래 생성은 동일 DB 트랜잭션으로 처리한다.

### 2.6 환불의 독립성

- 전액 및 부분 환불은 모두 `REFUND` 거래로 신규 등록한다.
- 환불은 새로운 수익이 아니라 비용 감소로 처리한다.
- 환불은 실제 환불 발생일에 기록하며 원래 지출과 별도의 거래 이력으로 유지한다.
- 원거래 ID를 연결하지 않고 원거래 금액과 대조하지 않는다.
- 원 결제 계정과 환불 수령 계정이 달라도 허용한다.

---

## 3. API 구성

| 기능 | Endpoint | 비고 |
| --- | --- | --- |
| 거래 등록 | `POST /transactions` | 일반 거래 4종 등록 |
| 거래 수정 | `PATCH /transactions/{id}` | 거래 유형 변경 불가 |
| 거래 삭제 | `DELETE /transactions/{id}` | Hard Delete |
| 거래 목록 조회 | `GET /transactions` | 기간·유형·계정·분류·키워드 필터, Cursor Pagination |
| 단일 거래 조회 | `GET /transactions/{id}` | 단일 `TransactionResponse` |
| 기초 잔액 설정 | Account API에서 처리 | `OPENING_BALANCE` 생성·수정 |
| 지출 분류별 집계 | `GET /reports/expenses/by-category` | 대·소분류 계층별 집계 |
| 수입 분류별 집계 | `GET /reports/income/by-category` | 대·소분류 계층별 집계 |
| 지출 기간별 집계 | `GET /reports/expenses/by-period` | 월별·연도별 지출 집계 |
| 수입 기간별 집계 | `GET /reports/income/by-period` | 월별·연도별 수입 집계 |
| 계정별 누적 잔액 조회 | `GET /reports/account-balances` | 지정 기준일까지의 계정별 잔액 |

`OPENING_BALANCE`는 일반 거래 등록·수정·삭제 API에서 제외하며, 금융 계정 도메인에서 관리한다.

수입·지출 집계는 각각 독립적인 API로 제공하고, 분류별 집계와 기간별 집계를 별도 엔드포인트로 구분한다.

거래 조회 및 집계 API는 모두 서버에서 확정된 도메인 규칙에 따라 결과를 산출한다.

---

## 4. 거래 응답 모델

### 4.1 공통 Projection

일반 거래 등록 결과, 거래 목록 조회, 단건 조회는 **단일 `TransactionResponse`**를 사용한다.

- `Transaction`과 `LedgerEntry`를 조합한 비즈니스 중심 Projection으로 반환한다.
- `LedgerEntry` 원본 구조와 회계적 부호는 응답에 노출하지 않는다.
- 거래 유형별로 서로 다른 최상위 응답 모델을 사용하지 않는다.
- `OPENING_BALANCE`도 목록 및 단건 조회에서 동일한 응답 모델을 사용한다.

### 4.2 손익 분류

손익 분류는 중첩 객체가 아닌 다음의 평탄화 필드로 제공한다.

```json
{
  "categoryItemId": 15,
  "categoryLabel": "식비>편의점"
}
```

- `categoryItemId`: 기존 분류 선택값 복원에 사용한다.
- `categoryLabel`: 대분류와 소분류를 결합한 조회용 문자열이다.
- 거래 유형별 Nullable 조건은 §4.4의 최종 응답 계약을 따른다.

### 4.3 금융 계정

일반 소비·수입·환불에서는 공통 계정 필드를 사용한다.

```json
{
  "accountId": 3,
  "accountName": "신한은행",
  "transferMetadata": null
}
```

이체 거래에서는 공통 계정 필드 대신 `transferMetadata`를 사용한다.

```json
{
  "accountId": null,
  "accountName": null,
  "transferMetadata": {
    "sourceId": 3,
    "sourceName": "신한은행",
    "targetId": 5,
    "targetName": "지갑"
  }
}
```

- `sourceId`와 `targetId`의 방향은 원 거래의 방향을 유지한다.
- 조회하는 계정에 따라 원계정과 목적계정을 서로 바꾸지 않는다.
- 화면에 표시할 결합 문자열을 서버에서 미리 생성하지 않는다.


### 4.4 최종 응답 계약

거래 등록·목록 조회·단건 조회는 거래 유형에 관계없이 동일한 `TransactionResponse`를 반환한다.

모든 응답 속성은 JSON에 포함하며, 해당 거래 유형에서 사용하지 않는 속성은 생략하지 않고 `null`로 반환한다.

#### TransactionResponse

| 속성 | Kotlin 타입 | Nullable | 설명 |
| --- | --- | --- | --- |
| `id` | `Long` | No | 거래 고유 식별자 |
| `type` | `TransactionType` | No | 거래 유형 |
| `date` | `LocalDate` | No | 거래일 (`yyyy-MM-dd`) |
| `amount` | `BigDecimal` | No | 거래 금액 |
| `merchant` | `String?` | Yes | 거래처 또는 수입처 |
| `description` | `String` | No | 거래 상세 내역 |
| `accountId` | `Int?` | Yes | 단일 금융 계정 ID |
| `accountName` | `String?` | Yes | 금융 계정 표시명 |
| `categoryItemId` | `Int?` | Yes | 손익 소분류 ID |
| `categoryLabel` | `String?` | Yes | 대분류와 소분류를 결합한 표시명 |
| `transferMetadata` | `TransferMetadata?` | Yes | 이체 거래의 원계정·목적계정 정보 |
| `paymentMethod` | `String?` | Yes | 결제 수단 |
| `tags` | `String?` | Yes | 태그 |
| `isWaste` | `Boolean` | No | 낭비 여부 |

- `amount`는 JSON number로 반환하며, 회계적 증감 부호와 관계없이 양수로 표현한다.
- 금액은 `NUMERIC(15,2)` 정밀도를 따른다.
- `merchant`, `paymentMethod`, `tags`는 거래 유형에 따라 `null`을 허용한다.
- `description`은 모든 거래 유형에서 필수로 제공한다.
- `isWaste`는 항상 Boolean 값으로 반환하며, 소비 이외의 거래 유형에서는 `false`를 사용한다.
- `createdAt`은 현재 응답 모델에 포함하지 않는다.

#### TransferMetadata

`TRANSFER` 거래에서만 사용하는 내부 Projection 객체다.

| 속성 | Kotlin 타입 | Nullable | 설명 |
| --- | --- | --- | --- |
| `sourceId` | `Int` | No | 원계정 ID |
| `sourceName` | `String` | No | 원계정 표시명 |
| `targetId` | `Int` | No | 목적계정 ID |
| `targetName` | `String` | No | 목적계정 표시명 |

- `transferMetadata` 자체는 Nullable이지만, 객체가 존재하면 내부 네 속성은 모두 필수다.
- 원계정과 목적계정의 방향은 거래 등록 당시의 방향을 유지한다.
- 계정 ID는 수정 화면의 선택값 복원에, 계정명은 거래 내역 표시에 활용한다.

#### 거래 유형별 응답 속성

다음 표에서 `필수`는 값이 반드시 존재함을, `선택`은 값 또는 `null`을 반환함을, `null`은 해당 유형에서 사용하지 않음을 의미한다.

| 속성 | SPENDING | INCOME_RECEIPT | TRANSFER | REFUND | OPENING_BALANCE |
| --- | --- | --- | --- | --- | --- |
| `merchant` | 필수 | 선택 | null | 선택 | null |
| `accountId` | 필수 | 필수 | null | 필수 | 필수 |
| `accountName` | 필수 | 필수 | null | 필수 | 필수 |
| `categoryItemId` | 필수 | 필수 | null | 필수 | null |
| `categoryLabel` | 필수 | 필수 | null | 필수 | null |
| `transferMetadata` | null | null | 필수 | null | null |
| `paymentMethod` | 선택 | null | null | null | null |
| `tags` | 선택 | 선택 | 선택 | 선택 | null |
| `isWaste` | Boolean | false | false | false | false |

`id`, `type`, `date`, `amount`, `description`는 모든 거래 유형에서 필수로 제공한다.

#### 응답 예시

일반 소비 거래 (`SPENDING`):

```json
{
  "id": 123,
  "type": "SPENDING",
  "date": "2026-11-18",
  "amount": 15000,
  "merchant": "편의점",
  "description": "간식 구매",
  "accountId": 3,
  "accountName": "신한은행",
  "categoryItemId": 15,
  "categoryLabel": "식비>편의점",
  "transferMetadata": null,
  "paymentMethod": null,
  "tags": null,
  "isWaste": false
}
```

계정 간 이체 (`TRANSFER`):

```json
{
  "id": 124,
  "type": "TRANSFER",
  "date": "2026-11-18",
  "amount": 50000,
  "merchant": null,
  "description": "신한은행 → 지갑",
  "accountId": null,
  "accountName": null,
  "categoryItemId": null,
  "categoryLabel": null,
  "transferMetadata": {
    "sourceId": 3,
    "sourceName": "신한은행",
    "targetId": 5,
    "targetName": "지갑"
  },
  "paymentMethod": null,
  "tags": null,
  "isWaste": false
}
```

#### Kotlin DTO

```kotlin
data class TransactionResponse(
    val id: Long,
    val type: TransactionType,
    val date: LocalDate,
    val amount: BigDecimal,
    val merchant: String?,
    val description: String,
    val accountId: Int?,
    val accountName: String?,
    val categoryItemId: Int?,
    val categoryLabel: String?,
    val transferMetadata: TransferMetadata?,
    val paymentMethod: String?,
    val tags: String?,
    val isWaste: Boolean,
)

data class TransferMetadata(
    val sourceId: Int,
    val sourceName: String,
    val targetId: Int,
    val targetName: String,
)
```

응답 DTO는 거래의 비즈니스 정보와 화면 구성에 필요한 식별자·표시 정보를 제공한다. 내부 분개 원본이나 회계적 부호는 노출하지 않는다.

구체적인 화면 설계 과정에서 추가 요구사항이 확인되면 필요한 범위에서 응답 계약을 조정한다.


---

## 5. 신규 거래 등록

### `POST /transactions`

클라이언트는 실제 거래의 비즈니스 정보만 전달한다. 서버가 거래 헤더와 분개를 자동 생성한다.

일반 거래 등록 API에서 지원하는 유형은 `SPENDING`, `INCOME_RECEIPT`, `TRANSFER`, `REFUND`다. `OPENING_BALANCE`는 지원하지 않는다.

### 5.1 요청 DTO 계층

`sealed class` 기반 다형성 요청 모델을 사용한다. JSON의 `type` 속성으로 하위 DTO를 구분한다.

```text
CreateTransactionRequest (sealed class)
├── SpendingRequest        // SPENDING
├── IncomeReceiptRequest   // INCOME_RECEIPT
├── TransferRequest        // TRANSFER
└── RefundRequest          // REFUND
```

공통 요청 속성은 `date`, `amount`, `description`이다.

- Controller는 공통 상위 타입으로 요청을 받는다.
- Jackson `@JsonTypeInfo`를 통해 `type`에 대응하는 하위 DTO로 역직렬화한다.
- 지원하지 않는 `type`, 해당 유형의 미사용 필드, 알 수 없는 속성은 `400 Bad Request`로 거부한다.
- 기존 Jackson 설정 `fail-on-unknown-properties: true`를 유지한다.

### 5.2 공통 등록 규칙

| 속성·규칙 | 확정된 정책 |
| --- | --- |
| `type` | 필수, 네 가지 일반 거래 유형만 허용 |
| `date` | 필수, 참여 계정의 `openingDate` 이상; 미래 날짜 포함 허용 |
| `amount` | 필수, 부호 없는 절대적 크기, **0 초과** |
| 금액 정밀도 | DB `NUMERIC(15,2)` |
| `description` | 공통 필수 입력, 공백만 있는 문자열 불허 |
| `merchant` | 유형별 필수 또는 선택, 최대 100자 저장 |
| `description` 길이 | 최대 255자 저장 |
| `paymentMethod` | 사용 유형에서만 선택 입력, 최대 50자 저장 |
| `tags` | 사용 유형에서만 선택 입력, `TEXT` |
| `isWaste` | 사용 유형의 낭비 여부, 기본값 `false` |
| 금융 계정 | 유형별 허용 subtype·활성 조건 만족 |
| 손익 분류 | 해당 유형에서 필요할 때만, 선택 가능 조건 및 유형 일치 |
| 분개 | 서버 생성, 거래 단위 합계 0 |

### 5.3 일반 소비 `SPENDING`

| 항목 | 규칙 |
| --- | --- |
| 필수 거래 정보 | `merchant`, `description`, `accountId`, `categoryItemId`, 공통 필수 속성 |
| 허용 금융 계정 | `BANK`, `CASH`, `PREPAID`, `CREDIT_CARD` |
| 허용 손익 분류 | `EXPENSE` |
| `paymentMethod` | 체크카드 식별 용도로 선택 입력 |
| `isWaste` | 소비 거래에서 사용 |
| 자동 분개 | 금융 계정 `-amount`, 비용 분류 `+amount` |

`merchant`와 `description`에는 `null`, 빈 문자열, 공백만 있는 문자열을 허용하지 않는다.

### 5.4 일반 수입 `INCOME_RECEIPT`

| 항목 | 규칙 |
| --- | --- |
| 필수 거래 정보 | `description`, `accountId`, `categoryItemId`, 공통 필수 속성 |
| 선택 거래 정보 | `merchant`(수입처), `tags` |
| 허용 금융 계정 | `ASSET`: `BANK`, `CASH`, `PREPAID` |
| 허용 손익 분류 | `INCOME` |
| `paymentMethod`, `isWaste` | 사용하지 않음 |
| 자동 분개 | 금융 계정 `+amount`, 수입 분류 `-amount` |

`description`에는 `null`, 빈 문자열, 공백만 있는 문자열을 허용하지 않는다.

### 5.5 자산 이체 `TRANSFER`

| 항목 | 규칙 |
| --- | --- |
| 필수 거래 정보 | `sourceAccountId`, `targetAccountId`, `description`, 공통 필수 속성 |
| 선택 거래 정보 | `tags` |
| 허용 금융 계정 | `ASSET`: `BANK`, `CASH`, `PREPAID`; `LIABILITY`: `CREDIT_CARD`, `LOAN` |
| 허용 계정 조합 | `ASSET → ASSET`, `ASSET → LIABILITY`, `LIABILITY → ASSET` |
| 불허 계정 조합 | `LIABILITY → LIABILITY`, 동일 계정 간 이체 |
| 손익 분류·`merchant`·`paymentMethod`·`isWaste` | 사용하지 않음 |
| 자동 분개 | 원계정 `-amount`, 목적계정 `+amount` |

- `EQUITY` 계정은 이체에 사용하지 않는다.
- `description`은 필수이며 `null`, 빈 문자열, 공백만 있는 문자열을 허용하지 않는다.
- 화면은 `원계정명 → 목적계정명`을 기본 문구로 제안할 수 있지만, 서버는 클라이언트가 전달한 문자열 그대로 저장한다.

### 5.6 환불 `REFUND`

| 항목 | 규칙 |
| --- | --- |
| 필수 거래 정보 | `accountId`, `categoryItemId`, `description`, 공통 필수 속성 |
| 선택 거래 정보 | `merchant`, `tags` |
| 허용 금융 계정 | `ASSET`: `BANK`, `CASH`, `PREPAID`; `LIABILITY`: `CREDIT_CARD` |
| 허용 손익 분류 | `EXPENSE` |
| `paymentMethod`, `isWaste` | 사용하지 않음 |
| 원거래 참조 | 사용하지 않음 |
| 자동 분개 | 환불 계정 `+amount`, 비용 분류 `-amount` |

- 환불 금액은 원거래의 일부 또는 전액일 수 있으며 대조하지 않는다.
- 환불 계정은 기존 결제 계정과 달라도 된다.
- `description`에는 `null`, 빈 문자열, 공백만 있는 문자열을 허용하지 않는다.

### 5.7 최종 요청 JSON 계약

거래 등록 요청은 `CreateTransactionRequest`를 상위 타입으로 하는 다형성 DTO 구조를 사용한다.

JSON의 `type` 속성으로 거래 유형을 지정하며, 서버는 해당 유형에 대응하는 하위 DTO로 역직렬화한다.

#### 공통 요청 속성

| 속성 | Kotlin 타입 | 필수 여부 | 검증 규칙 |
| --- | --- | --- | --- |
| `type` | `TransactionType` | 필수 | `SPENDING`, `INCOME_RECEIPT`, `TRANSFER`, `REFUND` |
| `date` | `LocalDate` | 필수 | `yyyy-MM-dd`, 참여 계정의 `openingDate` 이상 |
| `amount` | `BigDecimal` | 필수 | 0 초과, `NUMERIC(15,2)` 범위 |
| `description` | `String` | 필수 | `null`, 빈 문자열, 공백만 있는 문자열 불허, 최대 255자 |

- `type`은 JSON discriminator로 사용하며, 요청 DTO의 공통 속성으로 처리한다.
- `date`는 과거·현재·미래 날짜를 모두 허용한다.
- `amount`는 회계적 부호와 독립적인 양수로 전달한다.
- `amount`의 정수부는 최대 13자리, 소수부는 최대 2자리까지 허용한다.
- 금액의 범위 또는 정밀도를 초과하면 반올림하지 않고 `400 Bad Request`로 거부한다.
- 필수 속성의 생략 또는 명시적 `null`은 허용하지 않는다.

#### 거래 유형별 요청 속성

| 속성 | Kotlin 타입 | SPENDING | INCOME_RECEIPT | TRANSFER | REFUND |
| --- | --- | --- | --- | --- | --- |
| `accountId` | `Int` | 필수 | 필수 | - | 필수 |
| `categoryItemId` | `Int` | 필수 | 필수 | - | 필수 |
| `sourceAccountId` | `Int` | - | - | 필수 | - |
| `targetAccountId` | `Int` | - | - | 필수 | - |
| `merchant` | `String` / `String?` | 필수 | 선택 | - | 선택 |
| `paymentMethod` | `String?` | 선택 | - | - | - |
| `tags` | `String?` | 선택 | 선택 | 선택 | 선택 |
| `isWaste` | `Boolean?` | 선택 | - | - | - |

- `필수`: 생략 및 명시적 `null`을 허용하지 않는다.
- `선택`: 생략할 수 있으며, 별도 규칙이 없는 경우 명시적 `null`도 허용한다.
- `-`: 해당 거래 유형에서 사용하지 않는 속성으로, 전달하면 `400 Bad Request`로 거부한다.
- 계정 및 분류 ID는 기존 Account·Category 도메인의 식별자 자료형을 따른다.

#### 선택 속성 및 기본값

선택 문자열은 기존 Account API의 DTO 선언 관례에 따라 Nullable 타입과 기본값을 사용한다.

```kotlin
val merchant: String? = null
val paymentMethod: String? = null
val tags: String? = null
```

- 선택 문자열을 생략하거나 `null`로 전달하면 `null`로 처리한다.
- 문자열이 전달된 경우 빈 문자열 또는 공백만 있는 문자열은 허용하지 않는다.
- 문자열의 공백 유효성은 `trim()` 후 빈 문자열 여부를 기준으로 판단한다.
- `merchant`는 최대 100자, `paymentMethod`는 최대 50자이며, `tags`는 DB `TEXT` 타입을 따른다.
- 필수 문자열에도 동일한 공백 검증 규칙을 적용한다.

`isWaste`는 소비 거래에서만 사용하며, 생략 시 `false`를 적용한다.

```kotlin
@field:NotNull
val isWaste: Boolean? = false
```

- `true`: 낭비 지출
- `false`: 일반 지출
- 생략: `false`
- 명시적 `null`: `400 Bad Request`

`INCOME_RECEIPT`, `TRANSFER`, `REFUND`는 `isWaste`를 요청 속성으로 사용하지 않는다.

#### 유형별 JSON 요청 예시

**일반 소비 — `SPENDING`**

```json
{
  "type": "SPENDING",
  "date": "2026-11-18",
  "amount": 15000,
  "description": "간식 구매",
  "accountId": 3,
  "categoryItemId": 15,
  "merchant": "편의점",
  "paymentMethod": null,
  "tags": null,
  "isWaste": false
}
```

- `merchant`, `accountId`, `categoryItemId`는 필수다.
- `paymentMethod`, `tags`, `isWaste`는 선택 입력이다.
- `isWaste`를 생략하면 `false`를 적용한다.
- 금융 계정은 `BANK`, `CASH`, `PREPAID`, `CREDIT_CARD`만 허용한다.
- 손익 분류는 `EXPENSE` 유형이어야 한다.

**일반 수입 — `INCOME_RECEIPT`**

```json
{
  "type": "INCOME_RECEIPT",
  "date": "2026-11-18",
  "amount": 3000000,
  "description": "11월 급여",
  "accountId": 3,
  "categoryItemId": 21,
  "merchant": "회사",
  "tags": null
}
```

- `accountId`, `categoryItemId`는 필수다.
- `merchant`, `tags`는 선택 입력이다.
- 금융 계정은 `ASSET` 유형의 `BANK`, `CASH`, `PREPAID`만 허용한다.
- 손익 분류는 `INCOME` 유형이어야 한다.
- `paymentMethod`, `isWaste`는 사용하지 않는다.

**자산 이체 — `TRANSFER`**

```json
{
  "type": "TRANSFER",
  "date": "2026-11-18",
  "amount": 50000,
  "description": "신한은행 → 지갑",
  "sourceAccountId": 3,
  "targetAccountId": 5,
  "tags": null
}
```

- `sourceAccountId`, `targetAccountId`는 필수다.
- `tags`는 선택 입력이다.
- 원계정과 목적계정은 서로 달라야 한다.
- `ASSET → ASSET`, `ASSET → LIABILITY`, `LIABILITY → ASSET` 조합만 허용한다.
- `LIABILITY → LIABILITY` 및 `EQUITY` 계정의 사용은 허용하지 않는다.
- `accountId`, `categoryItemId`, `merchant`, `paymentMethod`, `isWaste`는 사용하지 않는다.

**환불 — `REFUND`**

```json
{
  "type": "REFUND",
  "date": "2026-11-18",
  "amount": 15000,
  "description": "상품 구매 취소",
  "accountId": 3,
  "categoryItemId": 15,
  "merchant": "온라인 쇼핑몰",
  "tags": null
}
```

- `accountId`, `categoryItemId`는 필수다.
- `merchant`, `tags`는 선택 입력이다.
- 금융 계정은 `BANK`, `CASH`, `PREPAID`, `CREDIT_CARD`만 허용한다.
- 손익 분류는 `EXPENSE` 유형이어야 한다.
- 원거래 ID를 요구하지 않는다.
- 환불 금액을 기존 지출 금액과 대조하지 않는다.
- `paymentMethod`, `isWaste`는 사용하지 않는다.

#### 역직렬화 및 요청 검증

- Jackson `@JsonTypeInfo`를 사용하여 `type`에 대응하는 하위 DTO를 결정한다.
- 지원하지 않는 거래 유형인 `OPENING_BALANCE`를 일반 등록 API에 전달하면 `400 Bad Request`로 거부한다.
- 지원하지 않는 `type` 또는 누락된 `type`은 `400 Bad Request`로 거부한다.
- 기존 `fail-on-unknown-properties: true` 설정을 유지한다.
- 해당 거래 유형에서 정의하지 않은 속성을 전달하면 `400 Bad Request`로 거부한다.
- 필수 속성의 누락, 자료형 불일치 및 유효성 검증 실패는 `400 Bad Request`로 처리한다.
- 기본 자료형·길이·금액 검증은 Bean Validation을 사용한다.
- 금융 계정과 손익 분류의 존재 여부, 활성 상태 및 유형 정합성은 서비스의 도메인 규칙으로 검증한다.
- 거래 등록 시 참여하는 모든 금융 계정의 `openingDate`를 확인한다.
- 유효한 요청에 대해서만 거래 헤더와 분개를 동일 DB 트랜잭션에서 생성한다.

Swagger(OpenAPI)에서는 공통 요청 타입을 기준으로 네 가지 하위 DTO가 `oneOf`로 표현되고, JSON `type`이 discriminator로 노출되어야 한다.

다형성 역직렬화, 하위 DTO의 Bean Validation 및 Swagger 스키마 표현은 구현 단계에서 실제 동작을 검증한다.

### 5.8 생성 성공 응답

- 거래 등록 성공 시 `201 Created`를 반환한다.
- `Location` 헤더에 생성된 거래의 URI `/transactions/{id}`를 제공한다.
- 응답 본문은 §4에서 정의한 `TransactionResponse`를 사용한다.
- `Location` 헤더는 기존 Account API와 동일하게 절대 URI로 생성한다.

---

## 6. 거래 정보 수정

### 6.1 수정 기본 정책

- 거래 `id` 및 `type`은 유지하며 유형 변경은 허용하지 않는다 (`400 Bad Request`).
- 같은 거래 유형 내에서 허용된 속성만 변경한다.
- 수정 전후 모두 해당 유형의 필수 입력 조건을 만족해야 한다.
- 금액과 결제 계정은 직접 수정할 수 있다. `ASSET`과 `LIABILITY` 유형 사이의 허용된 계정 변경도 가능하다.
- 금액 오입력 정정과 카드사 결제일 할인에 따른 금액 변경을 같은 수정 기능으로 처리한다.
- `OPENING_BALANCE`는 일반 거래 수정 API에서 제외한다.

### 6.2 수정 가능한 속성과 값 처리

수정 가능한 속성은 유형에서 사용하는 범위에 한정한다.

- `date`, `amount`, `description`
- `accountId` 또는 `sourceAccountId`·`targetAccountId`
- `categoryItemId`
- `merchant`, `tags`, `paymentMethod`, `isWaste`

요청에서 생략한 속성은 기존 값을 유지한다. 선택 속성에 명시적으로 `null`을 전달하면 값을 제거한다. 필수 속성에 `null`을 전달하면 `400 Bad Request`로 거부한다.

참조 대상 계정·분류가 실제로 변경된 경우에만 신규 참조의 활성·선택 가능 상태를 확인한다. 기존 참조를 유지한 수정에서는 현재 비활성 상태를 이유로 거부하지 않는다. 다만 거래 유형별 계정·분류 정합성은 유지해야 한다.

변경한 거래일은 참여하는 모든 계정의 `openingDate` 이상이어야 한다.

### 6.3 분개 갱신

- 금액·계정·분류가 **실제로 변경**되면 기존 `LedgerEntry` 전체를 삭제하고 새로운 분개를 생성한다.
- 기존 `Transaction.id`는 유지한다. 새 `LedgerEntry.id`가 달라지는 것은 허용한다.
- 거래일·거래처·상세 내역·태그·낭비 여부·결제 수단만 변경되면 `Transaction`만 갱신한다.
- 수정과 분개 재생성은 동일 DB 트랜잭션으로 수행하고 0-Sum 불변식을 유지한다.
- `TRANSFER`는 양쪽 계정 모두 수정할 수 있으며, 변경 후에도 계정 조합·상이한 계정 ID 조건을 만족해야 한다.

### 6.4 HTTP 계약

#### 거래 수정 엔드포인트

```http
PATCH /transactions/{id}
```

- 수정 대상 거래는 URI의 `{id}`로 식별한다.
- 요청에서 `type`을 전달하지 않는다.
- 서버는 기존 거래의 `Transaction.type`을 기준으로 수정 가능한 속성과 도메인 규칙을 결정한다.
- 거래 유형은 수정할 수 없다.
- 수정 성공 시 `200 OK`와 §4의 `TransactionResponse`를 반환한다.

#### 수정 요청 DTO

수정 요청은 거래 유형별 DTO를 분리하지 않고 단일 `UpdateTransactionRequest`를 사용한다.

거래 유형별 필드명과 자료형은 등록 요청 계약을 계승하되, 변경 가능한 속성만 요청 DTO에 포함한다.

| 속성 | Kotlin 값 타입 | 수정 대상 거래 유형 |
| --- | --- | --- |
| `date` | `LocalDate` | 전체 |
| `amount` | `BigDecimal` | 전체 |
| `description` | `String` | 전체 |
| `accountId` | `Int` | SPENDING, INCOME_RECEIPT, REFUND |
| `categoryItemId` | `Int` | SPENDING, INCOME_RECEIPT, REFUND |
| `sourceAccountId` | `Int` | TRANSFER |
| `targetAccountId` | `Int` | TRANSFER |
| `merchant` | `String` | SPENDING, INCOME_RECEIPT, REFUND |
| `paymentMethod` | `String` | SPENDING |
| `tags` | `String` | SPENDING, INCOME_RECEIPT, TRANSFER, REFUND |
| `isWaste` | `Boolean` | SPENDING |

- 요청에서 생략한 속성은 기존 값을 유지한다.
- 해당 거래 유형에서 사용하지 않는 속성이 전달되면 `400 Bad Request`로 거부한다.
- 필수 속성의 명시적 `null`은 허용하지 않는다.
- 선택 속성의 명시적 `null`은 기존 값을 제거한다.
- 속성의 유효성 검증은 등록 요청의 규칙을 계승한다.
- `OPENING_BALANCE`는 일반 거래 수정 API에서 제외한다.

#### 속성 생략과 명시적 null의 구분

PATCH 요청에서는 JSON 속성의 존재 여부와 전달값을 구분한다.

| JSON 요청 상태 | 처리 |
| --- | --- |
| 속성 생략 | 기존 값 유지 |
| 명시적 `null` | 선택 속성 제거, 필수 속성이면 400 Bad Request |
| 유효한 값 전달 | 기존 값 변경 |

일반적인 Kotlin Nullable 선언인 `String? = null`만으로는 생략과 명시적 `null`을 구분할 수 없다.

따라서 수정 요청 DTO에는 `JsonNullable<T>`를 사용한다.

```kotlin
data class UpdateTransactionRequest(
    val date: JsonNullable<LocalDate> = JsonNullable.undefined(),
    val amount: JsonNullable<BigDecimal> = JsonNullable.undefined(),
    val description: JsonNullable<String> = JsonNullable.undefined(),

    val accountId: JsonNullable<Int> = JsonNullable.undefined(),
    val categoryItemId: JsonNullable<Int> = JsonNullable.undefined(),
    val sourceAccountId: JsonNullable<Int> = JsonNullable.undefined(),
    val targetAccountId: JsonNullable<Int> = JsonNullable.undefined(),

    val merchant: JsonNullable<String> = JsonNullable.undefined(),
    val paymentMethod: JsonNullable<String> = JsonNullable.undefined(),
    val tags: JsonNullable<String> = JsonNullable.undefined(),
    val isWaste: JsonNullable<Boolean> = JsonNullable.undefined(),
)
```

`JsonNullable<T>`는 다음 세 상태를 표현한다.

- `undefined()`: JSON 속성이 전달되지 않음
- `of(null)`: JSON 속성이 명시적으로 `null`로 전달됨
- `of(value)`: JSON 속성에 실제 값이 전달됨

수정 요청의 모든 속성은 생략 가능하므로 `undefined()`를 기본값으로 사용한다.

필드의 존재 여부를 판단한 뒤, 기존 거래 유형과 등록 규칙에 따라 변경 가능 여부 및 값의 유효성을 검증한다.

#### Jackson 설정

`JsonNullable<T>`의 JSON 역직렬화를 지원하기 위해 `JsonNullableModule`을 등록한다.

라이브러리:

```text
org.openapitools:jackson-databind-nullable
```

설정 위치는 기존 프로젝트의 전역 설정 계층(`global/config`)을 사용한다.

```kotlin
@Configuration
class JacksonConfig {

    @Bean
    fun jsonNullableModule(): JsonNullableModule =
        JsonNullableModule()
}
```

- Spring Boot의 Jackson 설정을 통해 JSON 역직렬화에 모듈을 적용한다.
- Controller와 Service에서 `ObjectMapper`를 개별 생성하거나 모듈을 직접 등록하지 않는다.
- 기존 `fail-on-unknown-properties: true` 설정을 유지한다.
- Kotlin DTO 생성자와 `JsonNullable`의 역직렬화 호환성 및 Bean Validation 동작은 구현 단계에서 검증한다.

#### 수정 요청 예시

금액과 상세 내역 변경:

```json
{
  "amount": 12500,
  "description": "할인 적용 후 금액"
}
```

기존 태그 제거:

```json
{
  "tags": null
}
```

이체 원계정 변경:

```json
{
  "sourceAccountId": 8
}
```

- 요청에 포함되지 않은 속성은 변경하지 않는다.
- 변경된 금융 계정과 손익 분류는 §6의 기존 도메인 검증 규칙을 따른다.
- 금액·계정·분류 변경에 따른 분개 재생성 정책은 §6의 기존 규칙을 적용한다.

#### 성공 응답

- 정상적인 수정 요청은 `200 OK`를 반환한다.
- 응답 본문은 §4의 단일 `TransactionResponse`를 사용한다.
- 응답에는 수정 후의 거래 정보를 반환한다.
- 거래 ID와 거래 유형은 기존 값을 유지한다.

개별 `ErrorCode`의 정의 및 세부 예외 매핑은 구현 단계에서 처리한다.

---

## 7. 거래 삭제

- 일반 거래 유형 `SPENDING`, `INCOME_RECEIPT`, `TRANSFER`, `REFUND`는 삭제할 수 있다.
- `OPENING_BALANCE`는 일반 거래 삭제 API 대상에서 제외한다.
- 삭제는 물리 삭제(Hard Delete)다. 논리 삭제·복구는 제공하지 않는다.
- `Transaction` 삭제 시 FK `ON DELETE CASCADE`로 해당 `LedgerEntry`를 함께 삭제한다.
- 삭제는 단일 DB 트랜잭션에서 수행한다.
- 삭제 시 기존 계정·분류의 활성 상태를 다시 검증하지 않는다.
- 삭제 거래는 원장 잔액 및 손익 집계에서 완전히 제외한다.
- 환불 거래는 원 지출 거래와 독립적으로 삭제하며 연쇄 삭제하지 않는다.
- 삭제 대상이 없으면 `404 Not Found`, 삭제 성공 시 `204 No Content`다.

### 7.1 HTTP 계약

#### 거래 삭제 엔드포인트

```http
DELETE /transactions/{id}
```

- 삭제 대상 거래는 URI의 `{id}`로 식별한다.
- `SPENDING`, `INCOME_RECEIPT`, `TRANSFER`, `REFUND` 거래의 삭제를 허용한다.
- `OPENING_BALANCE`는 일반 거래 삭제 대상에서 제외하며, Account 도메인에서 관리한다.
- Request Body는 사용하지 않는다.
- 삭제는 §7의 Hard Delete 및 분개 정합성 규칙을 따른다.

#### HTTP 응답

| 처리 결과 | HTTP Status |
| --- | --- |
| 정상 삭제 | `204 No Content` |
| 존재하지 않는 거래 | `404 Not Found` |
| `OPENING_BALANCE` 삭제 요청 | `400 Bad Request` |

- 삭제 성공 시 응답 본문은 반환하지 않는다.
- 오류 응답은 프로젝트 공통 `ErrorResponse`를 사용한다.
- 개별 `ErrorCode` 및 세부 예외 매핑은 구현 단계에서 정의한다.

---

## 8. 거래 조회 및 탐색

### 8.1 목록 및 단건 조회

- `GET /transactions`: 거래 목록 조회
- `GET /transactions/{id}`: 단일 거래 조회
- 모두 §4의 단일 `TransactionResponse`를 사용한다.
- `OPENING_BALANCE`도 조회 및 유형 필터에 포함한다.
- 조회 시 손익 분류명·계정명은 해당 참조의 현재 표시명을 사용한다.

### 8.2 기간

- 목록 조회의 `startDate`, `endDate`는 필수다.
- 시작일과 종료일을 모두 포함한다.
- `startDate > endDate`는 `400 Bad Request`로 거부한다.
- 백엔드의 기본 기간 또는 최대 기간 제한을 두지 않는다.
- 프론트엔드의 월간 목록 기본 범위는 해당 월 1일부터 말일까지다.

### 8.3 정렬 및 페이지네이션

- Cursor 기반 Keyset Pagination을 사용한다.
- 기본 정렬: `date ASC, id ASC`.
- 역순 정렬: `date DESC, id DESC`도 지원한다.
- 기본 페이지 크기 250건, 최대 250건이다.
- 250보다 작은 양수 페이지 크기를 허용하고 초과값은 250으로 제한한다.
- 0 이하의 페이지 크기는 `400 Bad Request`다.
- 커서는 HTTP 응답 헤더로 전달한다.
- 본문은 별도 Page Wrapper 없이 `List<TransactionResponse>`다.
- 페이지 번호 및 임의 페이지 이동은 지원하지 않는다.

### 8.4 조회 필터

모든 필터는 선택 입력이고, 서로 다른 필터는 `AND` 조건으로 결합한다. 일치 항목이 없으면 `200 OK`와 빈 목록을 반환한다.

| 필터 | 규칙 |
| --- | --- |
| 금융 계정 | 해당 계정이 참여한 거래. `TRANSFER`는 원계정 또는 목적계정 일치 시 포함 |
| 거래 유형 | `SPENDING`, `INCOME_RECEIPT`, `TRANSFER`, `REFUND`, `OPENING_BALANCE` |
| 손익 분류 | 해당 소분류를 참조하는 거래만 반환. 분류 없는 유형은 결과에서 제외 |
| 통합 키워드 | 거래처·상세 내역·태그 중 하나라도 부분 일치 (`OR`), 대소문자 구분 없음 |

- 통합 키워드는 다른 필터와 `AND`로 결합한다.
- 계정명·분류명은 키워드 검색 범위에 포함하지 않는다.
- `paymentMethod`와 `isWaste`에 대한 독립 조회 필터는 제공하지 않는다.
- 금융 계정 필터는 이체의 원계정·목적계정 표시 방향을 바꾸지 않는다.

### 8.5 카드 명세서 대사

- 카드 명세서 대사를 위한 별도 Ledger 엔드포인트는 제공하지 않는다.
- 프론트엔드가 Account API의 카드 메타데이터를 활용해 이용기간을 계산한다.
- 계산된 기간과 카드 계정 ID로 기존 거래 목록 API를 호출한다.

### 8.6 HTTP 계약

#### 조회 엔드포인트

```http
GET /transactions
GET /transactions/{id}
```

#### 목록 조회 Query Parameters

| 파라미터 | HTTP 자료형 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| `startDate` | Date (`yyyy-MM-dd`) | Yes | - | 조회 시작일 (포함) |
| `endDate` | Date (`yyyy-MM-dd`) | Yes | - | 조회 종료일 (포함) |
| `accountId` | Integer | No | - | 금융 계정 필터 |
| `type` | String (Enum) | No | - | 거래 유형 필터 |
| `categoryItemId` | Integer | No | - | 손익 소분류 필터 |
| `keyword` | String | No | - | 통합 키워드 검색 |
| `sort` | String (`asc`, `desc`) | No | `asc` | 정렬 방향 |
| `size` | Integer | No | `250` | 페이지 크기 (최대 250) |
| `cursor` | String | No | - | 다음 페이지 조회 위치 |

- 필터의 적용 조건과 검색 범위는 §8의 기존 조회 규칙을 따른다.
- 필터 간에는 `AND`, 통합 키워드의 검색 대상 간에는 `OR` 조건을 적용한다.
- `cursor`를 생략하면 첫 페이지를 조회한다.

#### 정렬

| `sort` | 정렬 기준 |
| --- | --- |
| `asc` (기본값) | `date ASC, id ASC` |
| `desc` | `date DESC, id DESC` |

- 정렬 필드는 거래일(`date`)과 거래 ID(`id`)로 고정한다.
- 두 필드의 정렬 방향은 동일하게 적용한다.

#### Cursor 기반 페이지네이션

- Keyset Pagination을 사용한다.
- Cursor는 마지막 조회 위치의 `date`, `id`를 기준으로 구성한다.
- Cursor는 Base64URL 인코딩된 불투명 토큰으로 전달한다.
- 검색 조건은 Cursor에 포함하지 않고 기존 Query Parameter로 유지한다.
- 후속 페이지 요청에는 기존 필터 및 정렬 조건과 함께 Cursor를 전달한다.
- Cursor의 내부 직렬화 형식, 공통 페이지네이션 추상화 및 Spring Data 활용 여부는 구현 단계에서 결정한다.

#### 다음 페이지 응답

- 다음 페이지가 존재하면 HTTP `Link` 헤더에 `rel="next"` 링크를 제공한다.
- 링크에는 기존 검색·정렬 조건과 다음 페이지의 Cursor를 포함한다.
- 마지막 페이지에서는 `rel="next"` 링크를 생략한다.
- 응답 본문은 페이지네이션 메타데이터 없이 `List<TransactionResponse>`를 반환한다.

요청 예시:

```http
GET /transactions?startDate=2026-01-01&endDate=2026-12-31&accountId=3&sort=desc&size=250
```

응답 예시:

```http
HTTP/1.1 200 OK
Content-Type: application/json
Link: </transactions?startDate=2026-01-01&endDate=2026-12-31&accountId=3&sort=desc&size=250&cursor=eyJ...>; rel="next"
```

응답 본문은 §4의 `TransactionResponse` 배열을 사용한다.

#### HTTP 응답

| 처리 결과 | HTTP Status | Response Body |
| --- | --- | --- |
| 단건 조회 성공 | `200 OK` | `TransactionResponse` |
| 목록 조회 성공 | `200 OK` | `List<TransactionResponse>` |
| 목록 조회 결과 없음 | `200 OK` | `[]` |
| 존재하지 않는 거래 단건 조회 | `404 Not Found` | 공통 `ErrorResponse` |
| 유효하지 않은 조회 조건 또는 Cursor | `400 Bad Request` | 공통 `ErrorResponse` |

- 날짜 범위 및 필터 값 검증은 §8의 기존 조회 규칙을 따른다.
- `startDate`, `endDate` 누락 및 잘못된 날짜 형식은 `400 Bad Request`로 처리한다.
- 지원하지 않는 정렬 방향, 유효하지 않은 페이지 크기 및 해석할 수 없는 Cursor는 `400 Bad Request`로 처리한다.
- 개별 `ErrorCode` 및 세부 예외 매핑은 구현 단계에서 정의한다.

---

## 9. 거래 집계 및 계정별 잔액

### 9.1 공통 집계 원칙

- 집계 원천은 `LedgerEntry`다.
- 손익 집계는 `EXPENSE`, `REVENUE` 분개 기준이다.
- 금융 계정 잔액은 `ASSET`, `LIABILITY` 계정의 분개를 기준으로 산출한다.
- 수입과 지출은 각각 독립적인 집계·조회 계약으로 제공한다.
- 집계 기능마다 필요한 기간·손익 분류·금융 계정 차원을 적용한다.

### 9.2 기간별 분류 집계

- 지정 기간의 수입 또는 지출을 대분류·소분류 계층으로 집계한다.
- 요청한 시작일과 종료일을 모두 포함한다.
- 대분류 필터는 선택 입력이며 금융 계정 필터는 제공하지 않는다.
- 집계 대상 분개가 존재하는 소분류만 포함한다.
- 순집계 금액이 0 또는 음수여도 대상 분개가 있다면 포함한다.
- 대분류 금액은 포함된 소분류의 금액 합계와 일치한다.
- Category API DTO를 재사용하지 않고 Report 전용 DTO를 사용한다.

분류별 집계 응답 예시는 다음과 같다.

```json
[
  {
    "categoryId": 10,
    "categoryName": "식비",
    "amount": 650000,
    "items": [
      { "categoryItemId": 101, "categoryItemName": "담배", "amount": 280000 },
      { "categoryItemId": 102, "categoryItemName": "외식", "amount": 370000 }
    ]
  }
]
```

### 9.3 기간 단위 집계

- `MONTH`, `YEAR` 단위의 수입·지출 집계를 제공한다. 일 단위는 지원하지 않는다.
- 시작·종료일을 포함하고 해당 기간과 겹치는 모든 월·연도 단위를 결과에 포함한다.
- 집계 대상 분개가 없으면 금액 0으로 반환한다.
- 월·연도 중간 날짜가 지정되면 요청 기간에 속하는 분개만 합산한다.
- 집계 기간 오름차순으로 정렬한다.
- 페이지네이션을 적용하지 않는다.

기간 단위 집계 응답 예시:

```json
[
  { "period": "2025-03", "amount": 350000 },
  { "period": "2025-04", "amount": 420000 },
  { "period": "2025-05", "amount": 0 }
]
```

### 9.4 계정별 누적 잔액 조회

- 기준일 `asOfDate`까지 각 계정의 `openingDate` 이후 분개를 누적한다.
- `OPENING_BALANCE` 분개도 합산한다.
- 자산 계정은 양수 보유 잔액·음수 부족 잔액, 부채 계정은 음수 채무 잔액·양수 초과 상환 상태로 표기한다.
- 계정별 분개 금액의 부호를 그대로 누적해 결과의 부호를 유지한다.
- 활성·비활성 계정을 모두 포함하며, 잔액 0원 또는 분개 이력 없는 계정도 포함한다.
- `openingDate`가 요청 기준일보다 늦은 계정만 결과에서 제외한다.
- 일부 계정이 제외되어도 전체 요청은 정상 처리한다.
- 별도의 자산 현황 집계 API는 현재 범위에서 제공하지 않는다.

### 9.5 HTTP 계약

#### 집계 엔드포인트

| 집계 기능 | HTTP Method | URI |
| --- | --- | --- |
| 분류별 지출 | `GET` | `/reports/expenses/by-category` |
| 분류별 수입 | `GET` | `/reports/income/by-category` |
| 기간별 지출 | `GET` | `/reports/expenses/by-period` |
| 기간별 수입 | `GET` | `/reports/income/by-period` |
| 계정별 누적 잔액 | `GET` | `/reports/account-balances` |

모든 집계 API는 `200 OK`와 JSON 응답 본문을 반환하며, 집계 기준 및 결과 포함 규칙은 §9의 기존 정책을 따른다.

#### 요청 Query Parameters

**분류별 집계**

| 파라미터 | HTTP 자료형 | 필수 | 설명 |
| --- | --- | --- | --- |
| `startDate` | Date (`yyyy-MM-dd`) | Yes | 집계 시작일 (포함) |
| `endDate` | Date (`yyyy-MM-dd`) | Yes | 집계 종료일 (포함) |
| `categoryId` | Integer | No | 대분류 필터 |

**기간 단위 집계**

| 파라미터 | HTTP 자료형 | 필수 | 설명 |
| --- | --- | --- | --- |
| `startDate` | Date (`yyyy-MM-dd`) | Yes | 집계 시작일 (포함) |
| `endDate` | Date (`yyyy-MM-dd`) | Yes | 집계 종료일 (포함) |
| `unit` | String (`MONTH`, `YEAR`) | Yes | 집계 단위 |

**계정별 누적 잔액**

| 파라미터 | HTTP 자료형 | 필수 | 설명 |
| --- | --- | --- | --- |
| `asOfDate` | Date (`yyyy-MM-dd`) | Yes | 잔액 산출 기준일 (포함) |

- 수입과 지출은 URI로 구분하며 별도 거래 유형 파라미터를 사용하지 않는다.
- 분류별 집계에는 금융 계정 필터를 제공하지 않는다.
- 계정별 누적 잔액은 각 금융 계정의 `openingDate`부터 `asOfDate`까지 집계한다.

#### 응답 DTO

**분류별 집계 — `CategoryReportResponse`**

응답 타입: `List<CategoryReportResponse>`

| 필드 | Kotlin 자료형 | Nullable |
| --- | --- | --- |
| `categoryId` | `Int` | No |
| `categoryName` | `String` | No |
| `amount` | `BigDecimal` | No |
| `items` | `List<CategoryItemReportResponse>` | No |

`CategoryItemReportResponse`

| 필드 | Kotlin 자료형 | Nullable |
| --- | --- | --- |
| `categoryItemId` | `Int` | No |
| `categoryItemName` | `String` | No |
| `amount` | `BigDecimal` | No |

응답 예시:

```json
[
  {
    "categoryId": 10,
    "categoryName": "식비",
    "amount": 650000,
    "items": [
      {
        "categoryItemId": 101,
        "categoryItemName": "외식",
        "amount": 370000
      },
      {
        "categoryItemId": 102,
        "categoryItemName": "담배",
        "amount": 280000
      }
    ]
  }
]
```

- 집계 대상 분개가 존재하는 분류만 포함한다.
- 상계 결과가 0원이거나 음수인 분류도 기존 집계 규칙에 따라 포함한다.
- 대분류의 `amount`는 하위 소분류 `amount`의 합계와 일치한다.

**기간 단위 집계 — `PeriodReportResponse`**

응답 타입: `List<PeriodReportResponse>`

| 필드 | Kotlin 자료형 | Nullable |
| --- | --- | --- |
| `period` | `String` | No |
| `amount` | `BigDecimal` | No |

`period`의 형식:

| `unit` | `period` 형식 | 예시 |
| --- | --- | --- |
| `MONTH` | `yyyy-MM` | `2026-03` |
| `YEAR` | `yyyy` | `2026` |

응답 예시:

```json
[
  { "period": "2026-01", "amount": 350000 },
  { "period": "2026-02", "amount": 420000 },
  { "period": "2026-03", "amount": 0 }
]
```

- 요청 기간에 포함되는 모든 월 또는 연도를 반환한다.
- 거래가 없는 집계 단위는 `amount: 0`으로 반환한다.
- 집계 기간 오름차순으로 정렬하며 페이지네이션은 적용하지 않는다.

**계정별 누적 잔액 — `AccountBalanceResponse`**

응답 타입: `List<AccountBalanceResponse>`

| 필드 | Kotlin 자료형 | Nullable |
| --- | --- | --- |
| `accountId` | `Int` | No |
| `accountName` | `String` | No |
| `type` | `AccountType` | No |
| `balance` | `BigDecimal` | No |

응답 예시:

```json
[
  {
    "accountId": 3,
    "accountName": "신한은행",
    "type": "ASSET",
    "balance": 1500000
  },
  {
    "accountId": 7,
    "accountName": "신한카드",
    "type": "LIABILITY",
    "balance": -350000
  }
]
```

- `balance`는 분개 부호를 유지한 누적 잔액이다.
- 활성·비활성 계정과 잔액 0원 계정을 모두 포함한다.
- `openingDate`가 `asOfDate`보다 늦은 계정은 제외한다.
- 응답 속성은 향후 화면 설계에서 필요한 경우 조정할 수 있다.

#### 입력값 검증

| 검증 조건 | HTTP Status |
| --- | --- |
| 필수 날짜 파라미터 누락 | `400 Bad Request` |
| 잘못된 날짜 형식 또는 유효하지 않은 날짜 | `400 Bad Request` |
| `startDate > endDate` | `400 Bad Request` |
| `unit` 누락 또는 `MONTH`, `YEAR` 이외의 값 | `400 Bad Request` |
| `categoryId`의 자료형 오류 | `400 Bad Request` |
| 존재하지 않는 `categoryId` | `200 OK` + `[]` |

- 날짜 범위의 양 끝을 포함하며, 조회 기간의 길이나 과거·미래 날짜에 별도 제한을 두지 않는다.
- 월·연도 중간 날짜를 요청해도 실제 지정한 범위만 집계한다.
- 존재하지 않는 `categoryId`는 리소스 조회 실패가 아닌 필터 조건 불일치로 처리한다.
- 잘못된 입력에 대한 오류 본문은 프로젝트 공통 `ErrorResponse`를 사용한다.

#### HTTP 응답

| 처리 결과 | HTTP Status | Response Body |
| --- | --- | --- |
| 분류별 집계 성공 | `200 OK` | `List<CategoryReportResponse>` |
| 분류별 집계 결과 없음 | `200 OK` | `[]` |
| 기간 단위 집계 성공 | `200 OK` | `List<PeriodReportResponse>` |
| 집계 대상 거래가 없는 기간 | `200 OK` | 해당 기간의 `amount: 0` |
| 계정별 누적 잔액 조회 성공 | `200 OK` | `List<AccountBalanceResponse>` |
| 계정별 잔액 조회 대상 없음 | `200 OK` | `[]` |
| 유효하지 않은 요청 파라미터 | `400 Bad Request` | 공통 `ErrorResponse` |

- 모든 응답은 별도 Page Wrapper 없이 최상위 JSON 배열을 사용한다.
- 개별 `ErrorCode` 및 세부 예외 매핑은 구현 단계에서 정의한다.

---

## 10. 검증 및 예외 처리

Ledger API는 기존 Account·Category API와 공통 오류 처리 규약을 유지한다. 전용 오류 응답 구조를 도입하지 않으며 프로젝트의 공통 `ErrorResponse`를 사용한다.

### 10.1 기존 정책으로 확정된 검증

- 필수 입력 누락, 자료형 오류, 공백만 있는 필수 문자열, 허용 길이 초과
- `amount > 0` 및 `NUMERIC(15,2)` 범위·정밀도 충족
- 거래 유형별 허용 금융 계정과 소분류 유형 검사
- 신규 거래의 참조 금융 계정 활성 상태 및 손익 분류 선택 가능 상태 검사
- 참여하는 모든 금융 계정의 `openingDate`와 거래일 비교
- `TRANSFER`의 원계정·목적계정 상이 여부 및 허용 조합 검사
- 지원하지 않는 `type` 및 유형에서 사용하지 않는 필드 거부
- 거래 단위 분개 금액 합계 0 보장

### 10.2 확정된 HTTP 상태

| 상황 | HTTP Status |
| --- | --- |
| 미지원 거래 유형 또는 잘못된 요청 필드 | `400 Bad Request` |
| 수정 요청에 `type` 속성 전달 | `400 Bad Request` |
| 삭제 대상 거래 없음 | `404 Not Found` |
| 거래 삭제 성공 | `204 No Content` |
| 목록 조회 기간 역전 또는 페이지 크기 0 이하 | `400 Bad Request` |
| 존재하지 않거나 사용 불가능한 계정·분류 참조 및 허용되지 않는 계정 조합 | `400 Bad Request` |

### 10.3 세부 오류 매핑

- 입력값 검증 및 도메인 규칙 위반에 대한 HTTP 상태는 각 API의 기존 계약을 따른다.
- 오류 응답은 프로젝트 공통 `ErrorResponse` 구조를 사용한다.
- 개별 `ErrorCode`의 명명과 세부 예외 매핑은 구현 단계에서 결정한다.
- 기존 Account·Category API의 예외 처리 패턴과 오류 코드 명명 관례를 우선 참고하며, 필요한 경우 확장한다.
- 코드 리뷰에서는 개별 오류 코드의 사전 정의 여부가 아닌, 명세에 확정된 비즈니스 규칙과 HTTP 오류 계약의 충족 여부를 검증한다.

---

## 11. Swagger 및 구현 단계 계약 검증

구현 및 코드 리뷰 과정에서 다음 항목이 본 명세의 API 계약과 일치하는지 확인한다.

- `sealed class` 요청 DTO와 JSON `type` 판별자의 다형성 역직렬화
- 거래 유형별 Bean Validation 및 미사용 필드 입력 시 `400 Bad Request` 처리
- `JsonNullable`을 통한 PATCH 요청의 속성 생략·명시적 `null`·값 변경 구분
- Swagger/OpenAPI의 `oneOf` 및 `discriminator` 표현과 실제 요청 계약의 일치 여부
- 거래 등록 응답의 `201 Created`, `Location` 헤더 및 `TransactionResponse` 본문
- 공통 `ErrorResponse`와 HTTP 오류 상태의 일치 여부

---

## 12. 현재 제공하지 않는 기능

- 일반 거래 API를 통한 `OPENING_BALANCE` 등록·수정·삭제
- 클라이언트가 직접 작성하는 회계 분개 또는 부호 입력
- 거래 유형의 사후 변경
- 수정 시 보정 거래를 별도로 생성하는 기본 방식
- 환불과 원거래의 ID 연결·원거래 금액 자동 검증
- 삭제된 거래의 논리 삭제·복구
- 페이지 번호 기반 조회 및 임의 페이지 이동
- 거래 입력 전용 별도 경량 Category 목록 API
- 카드 명세서 대사만을 위한 전용 Ledger 조회 API
- `paymentMethod`·`isWaste` 독립 검색 필터
- 통합 보고서 및 별도의 자산 현황 집계 API
- 거래 일괄 등록 (UI 설계 단계에서 필요성 검토)
