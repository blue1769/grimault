# 네이버 가계부 자산화 프로젝트 “Grimault”

# 진행 배경

- 네이버의 주력 서비스가 아닌 만큼, 장기적인 운영 지속에 대한 불확실성
- 기본적인 사용 편의성의 한계: 전체 거래 내역 탐색의 제약, 모바일 UI 부재 등
- 2012년부터 작성해 온, 삶의 궤적이라고 볼 수도 있는 중요 자료에 대한 자산화 필요성 인지

# 목표

> [!NOTE]
> **“네이버 가계부를 완전히 대체할 수 있는 가계부를 완성한다”**

## 주요 목표

- 2012년부터 누적된 네이버 가계부의 전체 데이터를 성공적으로 개인 자산화
- 기존 데이터와의 연속성을 유지하면서 새로운 거래를 기록할 수 있는 인터페이스 개발
- 네이버 가계부 내 주요 활용 기능을 비롯해 검색과 모바일 환경 등 일부 기초적 불편 사항 개선
- 복식부기 기반의 거래·분개 구조를 통해 자산 및 부채의 정합성 보장

## 완료 기준

- 가계부 전용 데이터베이스 구축: 전체 탐색을 고려한 인덱스 최적화
- 가계부 작성 및 조회 인터페이스 완성: 웹·모바일 환경에서 거래 입력, 검색, 자산 현황 조회 지원

## 목표가 아닌 것

- 편의 기능 고도화: 소비 패턴 분석 및 고급 시각화 대시보드 등 (초기 버전 이후 확장 고려)
- 수려한 디자인: 시각적 완성도보다 사용성에 집중하되, 익숙하고 자연스러운 UX 수준에서 완성 (익숙한 맛)

# 의존성

- 백엔드 기술 스택: Kotlin / Spring Boot
- 프론트엔드 기술 스택 사전 선정 (중요도 낮음, 러닝커브 최소화 우선)

# 프로젝트 명명

> [!NOTE]
> **“Grimault”**
>
> — The Sovereign Financial Ledger & Private Archive
>
> - 장기간 누적한 삶의 궤적이 응축된 금융 지식의 비전서(Grimoire)이자,
> - 외부 플랫폼에 종속하지 않고 홈랩에 단단히 봉인한 금고(Vault)라는 의미의 합성 조어

# 프로젝트 문서

## 데이터 및 회계 설계
- [원천 데이터 역공학 및 사용 패턴 분석](docs/raw-data-reverse-engineering.md)
- [PostgreSQL 선정 근거 및 데이터베이스 스키마](docs/data-and-schema.md)
- [데이터 클렌징 및 마이그레이션 전략](docs/data-cleansing-and-migration.md)
- [복식부기 분개 전략 및 회계 규칙](docs/journal-entry-rules.md)

## 백엔드 설계 및 API 명세
- [백엔드 아키텍처 및 패키지 전략](docs/backend-package-strategy.md)
- [Account API 기능 명세](docs/api-specifications/account-api-specification.md)
- [Category API 기능 명세](docs/api-specifications/category-api-specification.md)

## 테스트 전략
- [백엔드 단위 테스트 작성 규약](docs/backend-unit-test-convention.md)

# 마일스톤 및 우선순위

## Phase 1: 데이터 추출, 역공학, 정합성 검증

- 네이버 가계부 전체 RAW data 다운로드 및 구조 분석
- 데이터 추출 및 스키마 설계
- 데이터 클렌징 및 마이그레이션 스크립트 작성

## Phase 2: 백엔드 API 구현

- Spring Data JPA 기반 CRUD API 구현
- 자산 계정 및 수입·지출 카테고리 관리 API 구현
- 거래 유형별 복식부기 자동 분개 및 Zero-Sum 정합성 보장
- 과거 내역 고속 검색(기간, 키워드, 결제 수단 등) 최적화
- Docker 기반 환경 검증: NAS의 Container Manager를 이용한 수동 Compose

## Phase 3: 웹/모바일 인터페이스

- 모바일 반응형 작성 인터페이스: 키보드 중심의 빠른 거래 입력 UX
- 전체 내역 탐색/검색 뷰 및 필터링 UI 구현
- 네이버 가계부 핵심 편의 기능(자주 쓰는 항목 중심) 반영
- 수입·지출 및 자산 현황을 확인할 수 있는 통합 요약 화면 구현

## Phase 4: 배포 및 실사용 전환

- 실사용 데이터 추가 입력 및 정합성 최종 검증
- 홈 NAS 배포 후 실사용 전환
