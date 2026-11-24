# 신규 거래 등록 및 분개 정합성 (Create Transaction)

## 개요

신규 거래 등록 시 거래 유형 및 참조 금융 계정과 손익 분류에 따라
거래 헤더와 분개를 생성한다.

생성된 분개는 거래 단위 0-Sum invariant를 충족해야 하며,
거래 등록 결과는 비즈니스 중심의 응답 모델로 변환되어야 한다.

## 테스트 대상

- `TransactionService.create()`
- `TransactionMapper.toEntity()`
- `TransactionMapper.toResponse()`

## 테스트 시나리오

### 1. 거래 유형과 참여 계정에 따라 올바른 분개를 생성하고 응답을 반환한다

- **대상:** `TransactionService.create()`
- **시나리오:** 유효한 거래 등록 요청에 대해 거래 유형별 회계 규칙을
  충족하는 거래 및 분개를 생성하고 비즈니스 응답 모델을 반환한다.
- **사전 조건:**
    - 참조 금융 계정은 존재하며 활성 상태다.
    - 참조 소분류는 존재하며 상위 대분류와 자신의 활성 상태가 모두 참이다.
    - 거래일은 참여 금융 계정의 원장 시작일 이상이다.
    - 거래 금액은 양수다.
    - Repository는 Mock으로 구성한다.
    - 거래 저장 시 식별자가 부여된 거래를 반환하도록 구성한다.
    - `TransactionMapper`는 실제 구현을 사용한다.
- **실행:** `create(request)` 호출
- **기대 결과:**
    - `TransactionRepository.save()`가 한 번 호출된다.
    - 저장 대상 거래의 유형, 거래일, 거래처, 상세 내역,
      결제 수단, 태그 및 낭비 여부가 요청과 거래 유형별 규칙에 일치한다.
    - 거래에 정확히 두 개의 분개가 구성된다.
    - 각 분개는 생성 대상 거래를 참조한다.
    - 분개 유형, 참조 대상 및 금액의 부호가 입력 조건별 기대 결과와 일치한다.
    - 금융 계정 분개는 금융 계정만 참조한다.
    - 손익 분개는 손익 소분류만 참조한다.
    - 각 분개 금액의 절댓값은 요청 금액과 일치한다.
    - 두 분개 금액의 합계는 0이다.
    - 응답의 거래 ID는 저장 결과의 거래 ID와 일치한다.
    - 응답의 거래 유형, 거래일, 상세 내역 및 선택 속성은
      거래 헤더와 거래 유형별 응답 계약에 일치한다.
    - 응답 금액은 요청 금액과 동일한 양수다.
    - 소비, 수입, 환불 거래는 금융 계정 ID와 이름,
      손익 소분류 ID 및 대분류>소분류 형태의 표시명을 반환한다.
    - 소비, 수입, 환불 거래의 `transferMetadata`는 null이다.
    - 이체 거래는 `transferMetadata`에 원계정과 목적계정의
      ID 및 이름을 올바른 방향으로 반환한다.
    - 이체 거래의 단일 계정 및 손익 분류 관련 속성은 null이다.
    - 소비 이외의 거래는 `paymentMethod`가 null이며
      `isWaste`가 false다.
    - 이체 거래의 `merchant`는 null이다.
- **테스트 형태:** `@ParameterizedTest`

#### 입력 조건

| 거래 유형 | 금융 계정 | 손익 분류 | 양수 분개 | 음수 분개 |
|---|---|---|---|---|
| SPENDING | BANK | EXPENSE | EXPENSE | ASSET |
| SPENDING | CREDIT_CARD | EXPENSE | EXPENSE | LIABILITY |
| INCOME_RECEIPT | BANK | INCOME | ASSET | REVENUE |
| TRANSFER | BANK → CASH | - | 목적계정 ASSET | 원계정 ASSET |
| TRANSFER | BANK → CREDIT_CARD | - | 목적계정 LIABILITY | 원계정 ASSET |
| TRANSFER | LOAN → BANK | - | 목적계정 ASSET | 원계정 LIABILITY |
| REFUND | BANK | EXPENSE | ASSET | EXPENSE |
| REFUND | CREDIT_CARD | EXPENSE | LIABILITY | EXPENSE |

#### 추가 입력 및 검증 조건

| 입력 조건 | 기대 결과 |
|---|---|
| BANK 소비 거래의 소수부 2자리 금액 | 분개 및 응답에서 금액 정밀도 유지 |
| BANK 소비 거래의 paymentMethod, tags 입력 및 isWaste = true | 거래 헤더와 응답에 동일한 값 반영 |
| CREDIT_CARD 소비 거래의 선택 속성 생략 | paymentMethod, tags는 null이며 isWaste는 false |
| 수입 거래의 merchant 생략 | 거래 헤더 및 응답의 merchant는 null |
| 자산 계정 환불의 merchant 입력 | 거래 헤더 및 응답에 입력값 반영 |
| 신용카드 환불의 merchant = null | 거래 헤더 및 응답의 merchant는 null |