-- =============================================================================
-- 1. 금융 계정 마스터 (account)
-- =============================================================================
CREATE TABLE public.account (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL,
    sub_type VARCHAR(20) NOT NULL,
    settlement_day SMALLINT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- 계정 유형별 sub_type 무결성 및 신용카드 결제일 제약
    CONSTRAINT ck_account_type CHECK (
        (type = 'LIABILITY' AND sub_type = 'CREDIT_CARD' AND settlement_day BETWEEN 1 AND 31) OR
        (type = 'ASSET' AND sub_type IN ('CASH', 'BANK', 'PREPAID') AND settlement_day IS NULL)
    )
);

COMMENT ON TABLE public.account IS '금융 계정 마스터 (통장, 신용카드, 현금 지갑, 선불머니 등 잔액 주체)';
COMMENT ON COLUMN public.account.id IS '계정 고유 식별자 (PK)';
COMMENT ON COLUMN public.account.name IS '계정 고유 명칭 (예: 신한은행, 현대카드 제로, 네이버머니 등)';
COMMENT ON COLUMN public.account.type IS '회계 대분류 (ASSET: 자산, LIABILITY: 부채)';
COMMENT ON COLUMN public.account.sub_type IS '실사용 세부 성격 (BANK: 은행통장, CASH: 현금, PREPAID: 선불충전금, CREDIT_CARD: 신용카드)';
COMMENT ON COLUMN public.account.settlement_day IS '신용카드 결제일 (1~31, 신용카드 전용 메타데이터)';
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