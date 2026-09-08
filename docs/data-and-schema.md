# 데이터 추출 및 스키마 설계

## DBMS 및 IDE 선정

> [!IMPORTANT]
> **“PostgreSQL”** & **“DataGrip”**

### PostgreSQL의 강점
1. 원장(Ledger) 모델링 및 계산 쿼리의 성숙도
    - 누적 잔액(Running Balance) 연산 최적화 및 메모리 처리 방식에서의 성숙도와 유연성
    - 계층형 카테고리 트리 구조 탐색 시 재귀 쿼리나 ltree 확장 모듈 직접적 활용성

2. 강력한 준정형 데이터 타입 `JSONB` 의 존재
    - RAW 데이터 적재 후 재가공 시점에 GIN Index를 이용
    - JSON 속성에 대한 쿼리 및 데이터 가공에서의 자유도 확보

3. 데이터 정합성 보장에서의 엄격성
    - CHECK 제약 조건, Partial Index, 조건부 고유 제약 등 설계에서 MySQL 대비 강력

### 프로젝트 부수 목적에 부합
- 기존에 다뤄온 RDBMS의 핵심 원리를 크게 벗어나지 않는 러닝 커브
- 추가적인 RDBMS의 실질적인 경험치 누적의 계기
- NAS 컨테이너를 통한 손쉬운 운영 환경 가용성 확보

### DataGrip

> [!NOTE]
> [DataGrip, 이제 비상업적 용도로 무료 사용 가능](https://blog.jetbrains.com/ko/datagrip/2025/10/04/datagrip-is-now-free-for-non-commercial-use/) (2025년 10월 4일)

## 릴리즈 시점 데이터 컷오프 플로우 (Delta Migration)
1. 개발 및 검증 기간
    - 네이버 가계부에서 원본 데이터 다운로드
    - 원본 데이터를 이용한 스키마 검증 및 마이그레이션 스크립트 작성 완료
    - 기간 내 9월 가계부 작성은 네이버 가계부에서 수행
2. 최종 릴리즈 직전 (Cut-off 시점)
    - 네이버 가계부에서 원본 데이터 재다운로드
    - 개발 기간에 이미 내려받은 데이터를 필터링(신규 자료에서 9월 이후만 유효)
    - 필터링이 완료된 신규 작성 데이터만 이미 검증한 스크립트로 마이그레이션 수행
    - 이 후 가계부는 신규 서비스에서 작성 시작

## 태스크 목록
- PostgreSQL 컨테이너 구동 (via Synology NAS)
- 네이버 가계부 데이터 다운로드 (전체 약 2만 건, 2012. 12 - 2026. 8)
- 원본 데이터 가져오기 (DataGrip > Import)

    > [!WARNING]
    > Import 전 단계에서 XLS → TSV 후 헤더(제목, 조회기간 및 빈 줄) 전처리 필요 
    > ```bash
    > sed -i '' '1,3d' moneybook_outgo_*.tsv
    > sed -i '' '1,3d' moneybook_income_*.tsv
    > ```

- [RAW 데이터 기반 가계부 사용성 분석](raw-data-reverse-engineering.md)
- [네이버 가계부 주요 기능 분석과 선별 및 명세](requirements-specification.md)
- 신규 가계부 대상 스키마 정규화 및 설계
- RAW 데이터 클렌징 및 마이그레이션 스크립트 작성

## 스키마 명세 ([DDL](../sql/public-schema.sql))

> [!CAUTION]
> Markdown Table