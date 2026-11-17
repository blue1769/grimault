# 기초자본 계정 목록 조회 제외 검증 (Exclude Capital from Account List)

## 개요

금융 계정 목록 조회에서는 기초자본 계정(`subtype = CAPITAL`)을 반환하지 않는다.

`CAPITAL` 제외 조건은 활성 상태 및 소분류 필터와 관계없이 항상 적용한다.

## 테스트 대상

- `AccountSpecifications.excludeCapital()`

## 테스트 시나리오

### 1. 계정 목록 조회 조건에서 CAPITAL 소분류를 제외한다

- **대상:** `AccountSpecifications.excludeCapital()`
- **시나리오:** 계정 목록 조회에 사용하는 Specification이 `CAPITAL` 소분류를 제외하는 조건을 생성한다.
- **사전 조건:**
    - `Specification.toPredicate()` 호출에 필요한 Criteria API 객체를 Mock으로 구성한다.
    - Account의 `subtype` 경로를 반환하도록 설정한다.
    - `subtype != CAPITAL` 조건에 대응하는 Predicate를 준비한다.
- **실행:** `excludeCapital()`에서 반환한 Specification의 `toPredicate()` 호출
- **기대 결과:**
    - Account의 `subtype` 속성을 대상으로 조건을 생성한다.
    - `AccountSubtype.CAPITAL`과 같지 않다는 조건을 사용한다.
    - 생성한 Predicate를 반환한다.
- **테스트 형태:** `@Test`