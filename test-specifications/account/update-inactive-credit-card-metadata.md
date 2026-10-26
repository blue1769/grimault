# 비활성 신용카드의 상세정보 수정 검증 (Inactive Credit Card Metadata Update)

## 개요

비활성 상태의 신용카드 계정은 신용카드 전용 메타정보를 수정할 수 없다.

비활성 신용카드에 결제일 또는 청구 기간 변경을 요청하면 기존 계정 상태를 유지하고 요청을 거부한다.

## 테스트 대상

- `AccountService.update()`

## 테스트 시나리오

### 1. 비활성 신용카드의 상세정보 수정을 거부한다

- **대상:** `AccountService.update()`
- **시나리오:** 비활성 상태의 신용카드 계정에 카드 전용 메타정보 수정을 요청하면 비즈니스 예외가 발생한다.
- **사전 조건:**
    - ID가 `1`인 비활성 신용카드 계정이 존재한다.
    - 계정의 `type = LIABILITY`, `subtype = CREDIT_CARD`, `isActive = false`이다.
    - 계정명은 `"테스트 신용카드"`이다.
    - 초기 신용카드 메타정보:
        - `issuer = SHINHAN`
        - `settlementDay = 15`
        - `billingPeriod = (-1, 1) ~ (0, 15)`
    - `AccountRepository`는 해당 ID 조회 시 준비된 계정을 반환한다.
    - `UpdateAccountRequest`에는 `settlementDay = 20`을 지정한다.
    - 나머지 수정 요청 필드는 모두 `null`이다.
- **실행:** `update(id = 1, body = request)` 호출
- **기대 결과:**
    - `GrimaultException`이 발생한다.
    - 예외의 `errorCode`가 `ErrorCode.INACTIVE_ACCOUNT_UPDATE`와 일치한다.
    - 계정명, 활성 상태, 신용카드 메타정보가 모두 기존 값으로 유지된다.
- **테스트 형태:** `@Test`