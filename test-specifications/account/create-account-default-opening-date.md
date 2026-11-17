# 계정 원장 시작일 기본값 검증 (Default Account Opening Date)

## 개요

금융 계정 생성 시 원장 시작일(`openingDate`)은 선택 입력 속성이다.

원장 시작일을 지정하지 않으면 계정 등록일을 기본값으로 사용해야 한다.

## 테스트 대상

- `CreateAccountRequest.toEntity()`

## 테스트 시나리오

### 1. 원장 시작일을 생략하면 계정 등록일을 기본값으로 사용한다

- **대상:** `CreateAccountRequest.toEntity()`
- **시나리오:** 계정 생성 요청에서 `openingDate`를 생략하면 생성된 계정의 원장 시작일에 현재 날짜가 적용된다.
- **사전 조건:**
    - `CreateAccountRequest.name = "테스트 은행 계정"`
    - `type = "ASSET"`
    - `subtype = "BANK"`
    - `openingDate = null`
    - 신용카드 전용 메타정보는 모두 `null`이다.
- **실행:** `toEntity()` 호출
- **기대 결과:**
    - 생성된 `Account.openingDate`는 `toEntity()` 호출 시점의 현재 날짜와 일치한다.
    - 날짜 경계에서 테스트가 실행될 수 있으므로 호출 직전과 직후의 날짜 범위로 검증한다.
- **테스트 형태:** `@Test`