# 데이터 추출 및 스키마 설계

# 주요 솔루션

## DBMS 선정

<aside>

✅ **“PostgreSQL”**

</aside>

### PostgreSQL의 강점

1. 원장(Ledger) 모델링 및 계산 쿼리의 성숙도
    - 누적 잔액(Running Balance) 연산 최적화 및 메모리 처리 방식에서의 성숙도와 유연성
    - 계층형 카테고리 트리 구조 탐색 시 재귀 쿼리나 ltree 확장 모듈 직접적 활용성
2. 강력한 준정형 데이터 타입 `JSONB` 의 존재
    - RAW 데이터 적재 후 재가공 시점에 GIN Index를 이용
    - JSON 속성에 대한 쿼리 및 데이터 가공에서의 자유도 확보
3. 데이터 정합성 보장에서의 엄격성
    - CHECK 제약 조건, Partial Index, 조건부 고유 제약 등 설계에서 MySQL 대비 강력

### 프로젝트 부수적 목적 증대

- 기존에 다뤄온 RDBMS의 핵심 원리를 크게 벗어나지 않는 러닝 커브
- 추가적인 RDBMS의 실질적인 경험치 누적의 계기
- NAS 컨테이너를 통한 손쉬운 운영 환경 가용성 확보

## 작업 파일 관리 전략

<aside>

✅ **“프로젝트 결과물 명명을 한 뒤에 별도 분리”**

</aside>

```jsx
household-ledger/
├── docs/                 # 노션 대신 작성할 작업 문서, DDL, 스키마 정의
├── data/                 # 네이버 가계부 원본 CSV/엑셀 (.gitignore 필수)
├── scripts/              # RAW 데이터 PostgreSQL 1차 적재/정제 스크립트
├── backend/              # Kotlin / Spring Boot API 프로젝트
│   ├── src/
│   └── build.gradle.kts
├── frontend/             # (3주차에 추가될) 경량 Web UI
├── docker-compose.yml    # PostgreSQL 1기 + backend 구동 명세
└── README.md             # 프로젝트 소개 및 실행 가이드
```

- 신규 Gradle Project 생성하여 분리: 가계부 자체가 완결성 높은 단독 프로젝트 성격
- 배포 시 dockerfile 빌드 컨텍스트가 불필요한 의존성 충돌 없는 경량 관리면에서 유리

# 태스크 목록

- ~~프로젝트 구성 및 저장소 생성~~

    ```bash
    # 프로젝트 다운로드
    curl "https://start.spring.io/starter.zip" \
      -d language=kotlin \
      -d type=gradle-project \
      -d javaVersion=17 \
      -d bootVersion=4.1.1 \
      -d dependencies=web,data-jpa,postgresql,validation \
      -d groupId=com.blustar \
      -d artifactId=grimault \
      -d name=grimault \
      -o grimault.zip
    ```

    ```bash
    # 압축 해제
    unzip grimault.zip -d ./grimault
    rm grimault.zip
    cd ./grimault
    ```

    ```bash
    # 필수 작업 디렉토리 생성
    mkdir data scripts docs
    ```

    ```bash
    # 기본 Git 설정
    git init
    git branch -M main
    
    # 개발환경 공통 gitignore 다운로드
    curl -s "https://www.toptal.com/developers/gitignore/api/java,kotlin,gradle,macos,visualstudiocode" > .gitignore
    
    # 금융 데이터 및 환경설정 파일 유출 방지
    cat << 'EOF' >> .gitignore
    
    ### Local Data & Secrets ###
    /data/
    *.csv
    *.xls
    *.xlsx
    .env
    EOF
    ```

    ```bash
    # 첫 커밋 및 원격 저장소 푸시
    git add .
    git commit -m "chore: initial commit for grimault project setup"
    
    git remote add origin https://github.com/blue1769/grimault.git
    git push -u origin main
    ```

- [x]  PostgreSQL 컨테이너 구동 (Synology NAS)
- ~~네이버 가계부 데이터 다운로드 및 데이터베이스 가져오기(Datagrip > Import)~~

    ```sql
    CREATE SCHEMA IF NOT EXISTS raw;
    
    -- 지출 원본 테이블
    CREATE TABLE raw.outgo (
        id SERIAL PRIMARY KEY,
        entry_date TEXT,
        merchant TEXT,
        description TEXT,
        cash_amount TEXT,
        card_amount TEXT,
        bank_account TEXT,
        card_name TEXT,
        category TEXT,
        tags TEXT,
        is_waste TEXT
    );
    
    -- 수입 원본 테이블
    CREATE TABLE raw.income (
        id SERIAL PRIMARY KEY,
        entry_date TEXT,
        description TEXT,
        amount TEXT,
        deposit_account TEXT,
        category TEXT,
        tags TEXT
    );
    ```

- [x]  RAW 데이터 데이터베이스 입력
- ~~RAW 데이터 기반 네이버 가계부 기능별 사용성 및 패턴 분석~~
    <aside>

  📄 [Raw-data Reverse Engineering](raw-data-reverse-engineering.md)

    </aside>
- [ ]  네이버 가계부 주요 기능 분석 및 명세, 선별
- [ ]  신규 가계부 대상 스키마 정규화 및 설계

   ```sql
   -- =============================================================================
   -- 1. 금융 계정 마스터 (account)
   -- =============================================================================
   CREATE TABLE public.account (
       id SERIAL PRIMARY KEY,
       name VARCHAR(50) NOT NULL UNIQUE,
       type VARCHAR(20) NOT NULL,
       is_active BOOLEAN NOT NULL DEFAULT true,
       created_at TIMESTAMPTZ NOT NULL DEFAULT now()
   );
   
   COMMENT ON TABLE public.account IS '금융 계정 마스터 (통장, 신용카드, 현금 지갑, 페이머니 등 잔액 관리 주체)';
   COMMENT ON COLUMN public.account.id IS '계정 고유 식별자 (PK)';
   COMMENT ON COLUMN public.account.name IS '계정 고유 명칭 (예: 신한은행, Deep Dream, 쿠페이머니 등)';
   COMMENT ON COLUMN public.account.type IS '계정 자산 분류 (ASSET: 현금/통장/선불금, LIABILITY: 신용카드 부채)';
   COMMENT ON COLUMN public.account.is_active IS '신규 거래 작성 시 계정 활성화 여부 (true: 현재 유효 계정, false: 과거 해지 계정)';
   COMMENT ON COLUMN public.account.created_at IS '계정 등록 시스템 일시';
   
   -- =============================================================================
   -- 2. 분류 카테고리 마스터 (category)
   -- =============================================================================
   CREATE TABLE public.category (
       id SERIAL PRIMARY KEY,
       type VARCHAR(20) NOT NULL,
       parent_name VARCHAR(50) NOT NULL,
       sub_name VARCHAR(50) NOT NULL,
       is_active BOOLEAN NOT NULL DEFAULT true,
       CONSTRAINT uq_category UNIQUE (type, parent_name, sub_name)
   );
   
   COMMENT ON TABLE public.category IS '분류 카테고리 마스터 (대분류 > 소분류 2단계 고정 체계)';
   COMMENT ON COLUMN public.category.id IS '카테고리 고유 식별자 (PK)';
   COMMENT ON COLUMN public.category.type IS '손익 분류 구분 (EXPENSE: 지출, INCOME: 수입)';
   COMMENT ON COLUMN public.category.parent_name IS '1단계 대분류 명칭 (예: 식비, 주거/통신, 생활용품 등)';
   COMMENT ON COLUMN public.category.sub_name IS '2단계 소분류 명칭 (예: 외식, 통신비, 기타 등)';
   COMMENT ON COLUMN public.category.is_active IS '신규 거래 작성 UI 노출 여부 (true: 기본 노출, false: 기타 등 선택 지양 항목)';
   
   -- =============================================================================
   -- 3. 거래 헤더 (transaction)
   -- =============================================================================
   CREATE TABLE public.transaction (
       id BIGSERIAL PRIMARY KEY,
       transaction_date DATE NOT NULL,
       merchant VARCHAR(100) NOT NULL,
       description VARCHAR(255) NOT NULL,
       payment_method VARCHAR(50),
       tags TEXT,
       is_waste BOOLEAN NOT NULL DEFAULT false,
       created_at TIMESTAMPTZ NOT NULL DEFAULT now()
   );
   
   COMMENT ON TABLE public.transaction IS '거래 원천 헤더 (비즈니스 소비/수입/이체 이벤트 통합 관리)';
   COMMENT ON COLUMN public.transaction.id IS '거래 고유 식별자 (PK)';
   COMMENT ON COLUMN public.transaction.transaction_date IS '실제 소비/수입이 발생한 비즈니스 일자 (YYYY-MM-DD)';
   COMMENT ON COLUMN public.transaction.merchant IS '결제 가맹점 및 거래처 상호명 (예: 스타벅스, SKT, 쿠팡 등)';
   COMMENT ON COLUMN public.transaction.description IS '상세 구매 품목 및 사용 내역 적요';
   COMMENT ON COLUMN public.transaction.payment_method IS '결제 수단 식별 메타데이터 (체크카드명, 현금 등 세부 수단)';
   COMMENT ON COLUMN public.transaction.tags IS '모임/인명/맥락 기록용 검색 태그 (쉼표 구분 자유 텍스트)';
   COMMENT ON COLUMN public.transaction.is_waste IS '낭비성 지출 여부 플래그 (true: 낭비, false: 일반)';
   COMMENT ON COLUMN public.transaction.created_at IS '데이터베이스 최초 등록 시스템 감사 일시';
   
   -- =============================================================================
   -- 4. 분개 원장 라인 (ledger_entry)
   -- =============================================================================
   CREATE TABLE public.ledger_entry (
       id BIGSERIAL PRIMARY KEY,
       transaction_id BIGINT NOT NULL REFERENCES public.transaction(id) ON DELETE CASCADE,
       account_id INT REFERENCES public.account(id),
       category_id INT REFERENCES public.category(id),
       amount NUMERIC(15, 2) NOT NULL,
       entry_type VARCHAR(20) NOT NULL
   );
   
   COMMENT ON TABLE public.ledger_entry IS '복식부기 분개 원장 라인 (자산 이동 및 손익 인식의 세부 라인, 0-Sum 불변식 대상)';
   COMMENT ON COLUMN public.ledger_entry.id IS '분개 라인 고유 식별자 (PK)';
   COMMENT ON COLUMN public.ledger_entry.transaction_id IS '연계 거래 헤더 식별자 (FK, ON DELETE CASCADE)';
   COMMENT ON COLUMN public.ledger_entry.account_id IS '변동 대상 금융 계정 식별자 (자산/부채 변동 시 필수, FK)';
   COMMENT ON COLUMN public.ledger_entry.category_id IS '손익 인식 대상 카테고리 식별자 (수입/지출 손익 인식 시 매핑, FK)';
   COMMENT ON COLUMN public.ledger_entry.amount IS '거래 금액 (부호형: 증가는 양수(+), 감소는 음수(-))';
   COMMENT ON COLUMN public.ledger_entry.entry_type IS '분개 속성 구분 (ASSET: 자산, LIABILITY: 부채, EXPENSE: 비용, REVENUE: 수익)';
   ```

   <aside>
   💡 참고: DB에서 직접 Markdown 테이블 명세를 작성하는 쿼리

   ```sql
   SELECT 
       '| ' || c.column_name || 
       ' | ' || c.data_type || 
       ' | ' || (CASE WHEN c.is_nullable = 'NO' THEN 'NOT NULL' ELSE 'NULL' END) || 
       ' | ' || COALESCE(pgd.description, '-') || ' |' AS markdown_row
   FROM information_schema.columns c
   JOIN pg_catalog.pg_statio_all_tables st 
     ON c.table_schema = st.schemaname AND c.table_name = st.relname
   LEFT JOIN pg_catalog.pg_description pgd 
     ON pgd.objoid = st.relid AND pgd.objsubid = c.ordinal_position
   WHERE c.table_schema = 'public' 
     AND c.table_name = 'transaction'
   ORDER BY c.ordinal_position;
   ```

   </aside>

- [ ]  RAW 데이터 클렌징 및 마이그레이션
    - [ ]  마이그레이션 스크립트 작성
    - [ ]  스크립트 구동 (프로젝트 배포 직전)