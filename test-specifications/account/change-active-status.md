# 계정 활성 상태 변경 (Account Active Status)

## 개요

금융 계정은 활성 상태에서 비활성 상태로 변경할 수 있다.

계정을 비활성화하면 해당 계정의 활성 상태가 변경되며, 변경된 상태는 API 응답에도 반영되어야 한다.

## 테스트 대상

- `AccountService.changeActiveStatus()`

## 테스트 시나리오

### 1. 활성 계정을 비활성화할 수 있다

- **대상:** `AccountService.changeActiveStatus()`
- **시나리오:** 활성 상태의 금융 계정에 비활성화를 요청하면 해당 계정이 비활성 상태로 변경된다.
- **사전 조건:**
    - ID가 `1`인 금융 계정이 존재한다.
    - 계정의 초기 상태는 `isActive = true`이다.
    - `AccountRepository`는 해당 ID 조회 시 준비된 계정을 반환한다.
- **실행:** `changeActiveStatus(id = 1, active = false)` 호출
- **기대 결과:**
    - 조회된 계정의 `isActive`가 `false`로 변경된다.
    - 반환된 `AccountResponse.id`는 `1`이다.
    - 반환된 `AccountResponse.isActive`는 `false`이다.
- **테스트 형태:** `@Test`