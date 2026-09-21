# 분개 전략

## 복식부기 부호(Sign) 절대 공식 리마인드
- 합계 불변식: 하나의 `transaction_id`에 묶인 모든 `ledger_entry.amount`의 총 합계는 0
- 지출(-)과 수입(+):
  - 지출 혹은 부채 발생 시: -amount (현금 자산 감소, 카드 부채 증가 등)
  - 수입 혹은 부채 상환 시: +amount (현금 자산 증가, 카드 부채 감소 등)
  - 손익 상대 계정: 비용 발생은 +amount (`EXPENSE`), 수익 발생은 -amount (`REVENUE`)

## 원장에 적재될 거래 5개 유형
```text
[원천 데이터] ─── (변환 파이프라인) ───► [public.transaction] (거래 헤더 1건)
                                              │
                                              ├──► [ledger_entry Line 1] (자산/부채 증감)
                                              └──► [ledger_entry Line 2] (손익인식 or 상대계정)
                                                   └─► SUM(amount) = 0
```

### 1. 일반 소비 거래 (전체 거래 이력 내 약 90%)
```text
☑️ 일상적인 식비, 쇼핑, 카페, 공과금 등 가장 흔한 이력.
☑️ 체크카드의 경우 출금 은행 통장이 `account_id`에 대응하고 카드명은 `payment_method`로 대응
```
- 원천 조건: `stage.outgo_curated` 중 category 대분류 NOT IN ('이체/대체', '카드대금')
- 거래 헤더 (`transaction`):
  - `transaction_date`: 소비일자
  - `merchant`: 소비점 상호
  - `description`: 소비 내역 상세
  - `payment_method`: 체크카드명, 현금 등 (신용카드의 경우 `null`)
- 분개 라인 (`ledger_entry`):

  | **Line No.** | **`account_id`** | **`category_id`** | **`amount`** |
  |---|---|---|---|
  | 1 | 신한은행 | `null` | -30,000 |
  | 2 | `null` | `식비>외식` | +30,000 |

> [!WARNING]
> 카드 취소나 환불 내역은 부호가 반대

### 2. 일반 수입 거래 (급여, 이자, 중고판매 등)
- 원천 조건: `stage.income_curated` 중 category 대분류 != '전월이월'
- 거래 헤더 (`transaction`):
  - `merchant`: `stage.income.description` 혹은 송금처
  - `description`: 수입 내역 상세 (원천 데이터 기준 `merchant`와 일괄 일치)
  - `payment_method`: `null`
- 분개 라인 (`ledger_entry`):

  | **Line No.** | **`account_id`** | **`category_id`** | **`amount`** |
  |---|---|---|---|
  | 1 | 신한은행 | `null` | +1,000,000 |
  | 2 | `null` | `주수입>급여` | -1,000,000 |

### 3. 계정 간 이체 및 부채 상환 (손익 없음, 순수 잔액 이동 취급)
```text
☑️ 현금 자산 간의 이동, 카드 청구대금 출금, 전세 대출 원금 상환 등
☑️ 지출이 아니므로 `category_id`는 `null`
```
- 원천 조건: `stage.outgo_curated` 중 category ILIKE '이체/대체>%' OR '카드대금>%'
- 거래 헤더 (`transaction`):
  - `merchant`: '이체/대체' 혹은, '카드대금' 
  - `description`: '신한카드 결제 대금 출금', '현금 인출 (신한은행 → 지갑)'
- 분개 라인 (`ledger_entry`):

  | **Line No.** | **`account_id`** | **`category_id`** | **`amount`** |
  |---|---|---|---|
  | 1 | 신한은행 | `null` | -300,000 |
  | 2 | 신한카드 | `null` | +300,000 |

### 4. 선불 자산 자금 이동
```text
☑️ 레거시 장부에서의 [카드지출] + [선불수입] 이중 장부 기록 대상 단일화
```
- 원천 조건: `stage.income_curated`의 선불 계정 수입과 짝이 맞는 `stage.outgo_curated` 카드/통장 결제
- 거래 헤더 (`transaction`):
  - `merchant`: '네이버머니', '스타벅스 카드'
  - `description`: '네이버머니 충전', '스타벅스 카드 충전'
- 분개 라인 (`ledger_entry`):

  | **Line No.** | **`account_id`** | **`category_id`** | **`amount`** |
  |---|---|---|---|
  | 1 | 신한카드 | `null` | -50,000 |
  | 2 | 스타벅스 카드 | `null` | +50,000 |

### 5. 기초 잔액 설정 (Opening Balance, 내역 내 총 4건)
```text
☑️ 레거시 장부 작성 시작 (2012년 12월) 시 설정된 금액
```
- 원천 조건: `stage.income_curated` 중 category ILIKE '전월이월>시작금액'
- 거래 헤더 (`transaction`):
  - `merchant`: '장부 시작 기초 잔액'
  - `description`: '신한은행 시작 금액'
- 분개 라인 (`ledger_entry`):

  | **Line No.** | **`account_id`** | **`category_id`** | **`amount`** |
  |---|---|---|---|
  | 1 | 신한은행 | `null` | +100,382 |
  | 2 | 기초자본 | `null` | -100,382 |

> [!IMPORTANT]
> `public.account`에 기초자본 계정 편입 (type = `EQUITY`)