# 비신용카드 계정의 신용카드 전용 정보 수정 검증 (Invalid Credit Card Metadata Update)

## 개요

신용카드가 아닌 금융 계정은 신용카드 전용 메타정보를 수정할 수 없다.

비신용카드 계정에 신용카드 전용 정보의 수정을 요청하면, 계정의 기존 상태를 변경하지 않고 요청을 거부한다.

## 테스트 대상

- `AccountService.update()`

## 테스트 시나리오

### 1. 비신용카드 계정의 신용카드 전용 정보 수정을 거부한다

- **대상:** `AccountService.update()`
- **시나리오:** 신용카드가 아닌 금융 계정에 신용카드 전용 메타정보 수정을 요청하면 비즈니스 예외가 발생한다.
- **사전 조건:**
    - ID가 `1`인 활성 은행 계정이 존재한다.
    - 계정의 `type = ASSET`, `subtype = BANK`, `isActive = true`이다.
    - 계정명은 `"테스트 은행 계정"`이다.
    - `AccountRepository`는 해당 ID 조회 시 준비된 계정을 반환한다.
    - `UpdateAccountRequest`에는 `settlementDay = 15`를 지정한다.
    - 나머지 수정 요청 필드는 모두 `null`이다.
- **실행:** `update(id = 1, body = request)` 호출
- **기대 결과:**
    - `GrimaultException`이 발생한다.
    - 예외의 `errorCode`가 `ErrorCode.INVALID_ACCOUNT_UPDATE`와 일치한다.
    - 계정명과 활성 상태 등 기존 계정 속성이 변경되지 않는다.
- **테스트 형태:** `@Test`