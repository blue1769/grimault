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