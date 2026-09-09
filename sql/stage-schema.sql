-- =============================================================================
-- Stage Schema DDL (Silver Layer)
-- =============================================================================
CREATE SCHEMA IF NOT EXISTS stage;

-- =============================================================================
-- 1. 지출 스테이징 테이블 (stage.outgo)
-- =============================================================================
CREATE TABLE stage.outgo (
    raw_id INT PRIMARY KEY,
    entry_date DATE NOT NULL,
    merchant VARCHAR(100),
    description VARCHAR(255),
    cash_amount BIGINT NOT NULL DEFAULT 0,
    card_amount BIGINT NOT NULL DEFAULT 0,
    bank_account VARCHAR(50),
    card_name VARCHAR(50),
    category VARCHAR(100),
    tags TEXT,
    is_waste BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE stage.outgo IS '지출 1차 정제 스테이징 테이블 (Silver)';
COMMENT ON COLUMN stage.outgo.raw_id IS '원천 raw.outgo 식별자 (PK 매핑)';
COMMENT ON COLUMN stage.outgo.entry_date IS '정제된 소비 일자 (DATE)';
COMMENT ON COLUMN stage.outgo.merchant IS '결제 가맹점 및 상호명';
COMMENT ON COLUMN stage.outgo.description IS '상세 소비 내역 적요';
COMMENT ON COLUMN stage.outgo.cash_amount IS '정제된 현금 결제액 (BIGINT)';
COMMENT ON COLUMN stage.outgo.card_amount IS '정제된 카드 결제액 (BIGINT)';
COMMENT ON COLUMN stage.outgo.bank_account IS '출금 계좌 명칭';
COMMENT ON COLUMN stage.outgo.card_name IS '결제 카드 명칭';
COMMENT ON COLUMN stage.outgo.category IS '원천 카테고리 경로 (대분류>소분류)';
COMMENT ON COLUMN stage.outgo.tags IS '태그 텍스트';
COMMENT ON COLUMN stage.outgo.is_waste IS '낭비성 지출 플래그';
COMMENT ON COLUMN stage.outgo.created_at IS '스테이징 적재 시스템 감사 일시';

-- =============================================================================
-- 2. 수입 스테이징 테이블 (stage.income)
-- =============================================================================
CREATE TABLE stage.income (
    raw_id INT PRIMARY KEY,
    entry_date DATE NOT NULL,
    description VARCHAR(255),
    amount BIGINT NOT NULL DEFAULT 0,
    deposit_account VARCHAR(50),
    category VARCHAR(100),
    tags TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE stage.income IS '수입 1차 정제 스테이징 테이블 (Silver)';
COMMENT ON COLUMN stage.income.raw_id IS '원천 raw.income 식별자 (PK 매핑)';
COMMENT ON COLUMN stage.income.entry_date IS '정제된 입금 일자 (DATE)';
COMMENT ON COLUMN stage.income.description IS '상세 입금 내역 적요';
COMMENT ON COLUMN stage.income.amount IS '정제된 입금 금액 (BIGINT)';
COMMENT ON COLUMN stage.income.deposit_account IS '입금 대상 계좌 명칭';
COMMENT ON COLUMN stage.income.category IS '원천 수입 카테고리 경로';
COMMENT ON COLUMN stage.income.tags IS '태그 텍스트';
COMMENT ON COLUMN stage.income.created_at IS '스테이징 적재 시스템 감사 일시';