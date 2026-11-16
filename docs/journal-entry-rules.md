# 분개 전략

## 복식부기 부호(Sign) 절대 공식 리마인드
- 합계 불변식: 하나의 `transaction_id`에 묶인 모든 `ledger_entry.amount`의 총 합계는 0
- 지출(-)과 수입(+):
  - 지출 혹은 부채 발생 시: -amount (현금 자산 감소, 카드 부채 증가 등)
  - 수입 혹은 부채 상환 시: +amount (현금 자산 증가, 카드 부채 감소 등)
  - 손익 상대 계정: 비용 발생은 +amount (`EXPENSE`), 수익 발생은 -amount (`REVENUE`)

## 원장에 적재될 거래 유형
```text
transaction (거래 헤더)
    │
    ├── ledger_entry (분개 1)
    └── ledger_entry (분개 2)
             │
             └── SUM(amount) = 0
```

## 거래 유형별 분개 규약
### 1. 일반 소비 거래 `SPENDING` (전체 거래 이력 내 약 90%)
```text
☑️ 일상적인 식비, 쇼핑, 카페, 공과금 등 가장 흔한 이력.
☑️ 체크카드의 경우 출금 은행 통장이 `account_id`에 대응하고 카드명은 `payment_method`로 대응
```
- 거래 헤더 (`transaction`):
  - `type`: `SPENDING`
  - `transaction_date`: 소비일자
  - `merchant`: 소비점 상호
  - `description`: 소비 내역 상세
  - `payment_method`: 체크카드명, 현금 등 (신용카드의 경우 `null`)
- 분개 라인 (`ledger_entry`):

  | **Line No.** | **`account_id`** | **`category_item_id`** | **`amount`** |
  |---|---|---|---|
  | 1 | 신한은행 | `null` | -30,000 |
  | 2 | `null` | `식비>외식` | +30,000 |

### 2. 취소 및 환불 거래 `REFUND`
```text
☑️ 기존 소비 거래의 취소 또는 환불로 인한 자산 회복 및 부채 감소
☑️ 일반 소비 거래와 반대 부호로 분개하며, 손익 분류는 기존 지출 분류를 활용
```
- 거래 헤더 (`transaction`):
  - `type`: `REFUND`
  - `transaction_date`: 취소 혹은 환불 발생일
  - `merchant`: 환불처 혹은 기존 결제 가맹점
  - `description`: 취소 혹은 환불 내역 상세
- 분개 라인 (`ledger_entry`): 현금성 자산 환불

  | **Line No.** | **`account_id`** | **`category_item_id`** | **`amount`** |
  |---|---|---|---|
  | 1 | 신한은행 | `null` | +30,000 |
  | 2 | `null` | `식비>외식` | -30,000 |

- 분개 라인 (`ledger_entry`): 신용카드 결제 취소

  | **Line No.** | **`account_id`** | **`category_item_id`** | **`amount`** |
  |---|---|---|---|
  | 1 | 신한카드 | `null` | +179,000 |
  | 2 | `null` | `생활용품>전자기기` | -179,000 |

### 3. 일반 수입 거래 `INCOME_RECEIPT` (급여, 이자, 중고판매 등)
- 거래 헤더 (`transaction`):
  - `type`: `INCOME_RECEIPT` 
  - `merchant`: 수입처 또는 거래 상대방
  - `description`: 수입 내역 상세
  - `payment_method`: `null`
- 분개 라인 (`ledger_entry`):

  | **Line No.** | **`account_id`** | **`category_item_id`** | **`amount`** |
  |---|---|---|---|
  | 1 | 신한은행 | `null` | +1,000,000 |
  | 2 | `null` | `주수입>급여` | -1,000,000 |

### 4. 계정 간 이체 및 부채 상환 `TRANSFER` (손익 없음, 순수 잔액 이동 취급)
```text
☑️ 현금 자산 간의 이동, 카드 청구대금 출금, 전세 대출 원금 상환 등
☑️ 지출이 아니므로 `category_item_id`는 `null`
```
- 거래 헤더 (`transaction`):
  - `type`: `TRANSFER` 
  - `merchant`: '이체/대체' 혹은, '카드대금' 
  - `description`: '신한카드 결제 대금 출금', '현금 인출 (신한은행 → 지갑)'
- 분개 라인 (`ledger_entry`):

  | **Line No.** | **`account_id`** | **`category_item_id`** | **`amount`** |
  |---|---|---|---|
  | 1 | 신한은행 | `null` | -300,000 |
  | 2 | 신한카드 | `null` | +300,000 |

### 5. 선불 자산 자금 이동 `TRANSFER`
```text
☑️ 레거시 장부에서의 [카드지출] + [선불수입] 이중 장부 기록 대상 단일화
```
- 거래 헤더 (`transaction`):
  - `type`: `TRANSFER`
  - `merchant`: '네이버머니', '스타벅스 카드'
  - `description`: '네이버머니 충전', '스타벅스 카드 충전'
- 분개 라인 (`ledger_entry`):

  | **Line No.** | **`account_id`** | **`category_item_id`** | **`amount`** |
  |---|---|---|---|
  | 1 | 신한카드 | `null` | -50,000 |
  | 2 | 스타벅스 카드 | `null` | +50,000 |

### 6. 기초 잔액 설정 `OPENING_BALANCE`
```text
☑️ 레거시 장부 작성 시작 (2012년 12월) 시 설정된 금액
```
- 거래 헤더 (`transaction`):
  - `type`: `OPENING_BALANCE`
  - `merchant`: '장부 시작 기초 잔액'
  - `description`: '신한은행 시작 금액'
- 분개 라인 (`ledger_entry`):

  | **Line No.** | **`account_id`** | **`category_item_id`** | **`amount`** |
  |---|---|---|---|
  | 1 | 신한은행 | `null` | +100,382 |
  | 2 | 기초자본 | `null` | -100,382 |

> [!IMPORTANT]
> `public.account`에 기초자본 계정 편입 (type = `EQUITY`)