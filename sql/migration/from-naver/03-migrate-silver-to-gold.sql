-- =============================================================================
-- Grimault / 네이버 가계부 데이터 마이그레이션: Silver → Gold
-- =============================================================================
-- 실행 전제:
--   1. 01-migrate-bronze-to-silver.sql 실행 및 Curated 데이터 정제 완료
--   2. 02-enrich-silver.sql 실행 완료 (transaction_type은 Silver에서 확정)
--   3. stage.income_curated에 환불용 target_merchant, target_category 컬럼 준비
--   4. Gold 대상 테이블 초기 적재 준비 완료 (재실행·UPSERT 용도 아님)
--
-- 마이그레이션 실행 순서:
--   1. Account / Category / CategoryItem 마스터 시드 생성
--   2. Transaction / LedgerEntry 거래 및 분개 생성
--   3. Account opening_date 최종 확정
--
-- 전체 INSERT 및 UPDATE는 하나의 트랜잭션에서 실행한다.
-- 기존 Gold 데이터가 적재된 상태에서는 대상 초기화 없이 실행하지 않는다.
-- =============================================================================

BEGIN;

-- =============================================================================
-- 1. 마스터 시드 생성: Account
-- =============================================================================
WITH credit_card_metadata_seed (
    name, issuer, settlement_day,
    start_month_offset, start_day, end_month_offset, end_day
) AS (
    VALUES
        ('휘닉스',        'BC',      20, -1,  6,  0,  5),
        ('생활의지혜',    'SHINHAN', 20, -1,  7,  0,  6),
        ('1Q Daily+',    'HANA',     5, -2, 23, -1, 22),
        ('Deep Dream',   'SHINHAN', 20, -1,  7,  0,  6)
), unioned_account_sources AS (
    SELECT coalesce(bank_account, card_name) AS name
    FROM stage.outgo_curated

    UNION ALL

    SELECT deposit_account AS name
    FROM stage.income_curated

    UNION ALL

    SELECT DISTINCT trim(split_part(category, '>', 2)) AS name
    FROM stage.outgo_curated
    WHERE category ILIKE '이체/대체>%'
), deduped_accounts AS (
    SELECT DISTINCT name
    FROM unioned_account_sources
), eligible_accounts AS (
    SELECT da.name,
           CASE
               WHEN da.name IN (
                   '전세대출(가양)', '전세대출(성남)',
                   '휘닉스', '생활의지혜', '1Q Daily+', 'Deep Dream'
               ) THEN 'LIABILITY'
               ELSE 'ASSET'
           END AS type,
           CASE
               WHEN da.name = '지갑' THEN 'CASH'
               WHEN da.name IN (
                   '우리은행', '국민은행', '하나주택청약', '카카오뱅크',
                   '신한은행', '기업은행', '언제든적금', '전세금'
               ) THEN 'BANK'
               WHEN da.name IN (
                   '식권', '복지카드', '쿠페이머니', '파스쿠치카드',
                   '긴급재난지원금', '보그헤어(선납)', '스타벅스(App)'
               ) THEN 'PREPAID'
               WHEN da.name IN ('전세대출(가양)', '전세대출(성남)') THEN 'LOAN'
               WHEN da.name IN ('휘닉스', '생활의지혜', '1Q Daily+', 'Deep Dream') THEN 'CREDIT_CARD'
           END AS subtype,
           CASE
               WHEN s.name IS NOT NULL THEN jsonb_build_object(
                   'issuer', s.issuer,
                   'settlementDay', s.settlement_day,
                   'billingPeriod', jsonb_build_object(
                       'startMonthOffset', s.start_month_offset,
                       'startDay', s.start_day,
                       'endMonthOffset', s.end_month_offset,
                       'endDay', s.end_day
                   )
               )
           END AS credit_card_metadata,
           da.name IN (
               '신한은행', '지갑', '쿠페이머니', '스타벅스(App)',
               'Deep Dream', '1Q Daily+'
           ) AS is_active
    FROM deduped_accounts da
    LEFT JOIN credit_card_metadata_seed s ON s.name = da.name
    WHERE da.name IS NOT NULL
      AND da.name NOT IN ('-', '어머니')
)
INSERT INTO public.account (
    name, type, subtype, opening_date, credit_card_metadata, is_active
)
SELECT name,
       type,
       subtype,
       DATE '1970-01-01' AS opening_date, -- 분개 생성 완료 전까지 사용하는 임시값
       credit_card_metadata,
       is_active
FROM eligible_accounts;

-- 기초 잔액 분개의 상대 계정으로 CAPITAL 계정이 필요하다.
-- 이미 EQUITY/CAPITAL 계정이 존재하면 기존 계정을 사용한다.
INSERT INTO public.account (name, type, subtype, opening_date, is_active)
SELECT '기초자본', 'EQUITY', 'CAPITAL', DATE '1970-01-01', false
WHERE NOT EXISTS (
    SELECT 1 FROM public.account WHERE type = 'EQUITY' AND subtype = 'CAPITAL'
);

-- =============================================================================
-- 1. 마스터 시드 생성: Category / CategoryItem
-- =============================================================================
CREATE TEMP TABLE tmp_category_seed ON COMMIT DROP AS
WITH unioned_category_sources AS (
    SELECT DISTINCT
           'EXPENSE' AS type,
           trim(split_part(category, '>', 1)) AS parent_name,
           trim(split_part(category, '>', 2)) AS sub_name
    FROM stage.outgo_curated

    UNION ALL

    SELECT DISTINCT
           'INCOME' AS type,
           trim(split_part(category, '>', 1)) AS parent_name,
           trim(split_part(category, '>', 2)) AS sub_name
    FROM stage.income_curated
)
SELECT DISTINCT type, parent_name, sub_name
FROM unioned_category_sources
WHERE NOT (type = 'EXPENSE' AND parent_name IN ('이체/대체', '카드대금', '미분류'))
  AND NOT (type = 'INCOME'  AND parent_name IN ('전월이월', '미분류'));

INSERT INTO public.category (type, name, is_active, sort_order)
SELECT type,
       parent_name,
       true,
       row_number() OVER (ORDER BY type, parent_name)
FROM (
    SELECT DISTINCT type, parent_name
    FROM tmp_category_seed
) AS parents;

INSERT INTO public.category_item (
    category_id, name, is_active, sort_order, is_favorite
)
SELECT c.id,
       s.sub_name,
       true,
       row_number() OVER (PARTITION BY c.id ORDER BY s.sub_name),
       false
FROM tmp_category_seed s
JOIN public.category c
  ON c.type = s.type AND c.name = s.parent_name;

-- =============================================================================
-- 2. 거래 및 분개 생성: Transaction / LedgerEntry
-- =============================================================================
-- 원천 raw_id 추적 컬럼은 마이그레이션 전용이며, 차분 마이그레이션 검증까지 유지한다.
ALTER TABLE public.transaction ADD COLUMN IF NOT EXISTS outgo_raw_id int;
ALTER TABLE public.transaction ADD COLUMN IF NOT EXISTS income_raw_id int;

-- =============================================================================
-- 2-1. 지출 원천: 일반 지출(SPENDING) 및 음수 지출 환불(REFUND)
-- =============================================================================
CREATE TEMP TABLE tmp_eligible_expenses ON COMMIT DROP AS
SELECT o.raw_id,
       o.entry_date,
       o.transaction_type,
       coalesce(nullif(o.merchant, ''), o.description) AS merchant,
       o.description,
       coalesce(nullif(o.cash_amount, 0), nullif(o.card_amount, 0)) AS amount,
       a.id AS account_id,
       a.type AS account_type,
       ci.id AS category_item_id,
       o.tags,
       o.is_waste,
       CASE WHEN o.bank_account IS NOT NULL AND o.card_name IS NOT NULL
            THEN o.card_name
       END AS payment_method
FROM stage.outgo_curated o
LEFT JOIN public.account a
  ON a.name = coalesce(o.bank_account, o.card_name)
LEFT JOIN public.category c
  ON c.type = 'EXPENSE'
 AND c.name = split_part(o.category, '>', 1)
LEFT JOIN public.category_item ci
  ON ci.category_id = c.id
 AND ci.name = split_part(o.category, '>', 2)
WHERE o.transaction_type IN ('SPENDING', 'REFUND');

INSERT INTO public.transaction (
    transaction_date, type, merchant, description, payment_method,
    tags, is_waste, outgo_raw_id
)
SELECT entry_date,
       transaction_type,
       merchant,
       description,
       payment_method,
       tags,
       is_waste,
       raw_id
FROM tmp_eligible_expenses;

WITH bases AS (
    SELECT t.id AS transaction_id,
           e.account_id,
           e.account_type,
           e.category_item_id,
           e.amount
    FROM tmp_eligible_expenses e
    JOIN public.transaction t ON t.outgo_raw_id = e.raw_id
)
INSERT INTO public.ledger_entry (
    transaction_id, account_id, category_item_id, amount, entry_type
)
SELECT transaction_id, account_id, NULL::int, -amount, account_type
FROM bases
UNION ALL
SELECT transaction_id, NULL::int, category_item_id, amount, 'EXPENSE'
FROM bases;

-- =============================================================================
-- 2-2. 수입 원천: 일반 수입(INCOME_RECEIPT)
-- =============================================================================
CREATE TEMP TABLE tmp_eligible_incomes ON COMMIT DROP AS
SELECT i.raw_id,
       i.entry_date,
       i.transaction_type,
       coalesce(nullif(trim(i.description), ''), '수입') AS description,
       i.amount,
       a.id AS account_id,
       ci.id AS category_item_id,
       i.tags
FROM stage.income_curated i
LEFT JOIN public.account a ON a.name = i.deposit_account
LEFT JOIN public.category c
  ON c.type = 'INCOME'
 AND c.name = split_part(i.category, '>', 1)
LEFT JOIN public.category_item ci
  ON ci.category_id = c.id
 AND ci.name = split_part(i.category, '>', 2)
WHERE i.transaction_type = 'INCOME_RECEIPT';

INSERT INTO public.transaction (
    transaction_date, type, merchant, description, payment_method,
    tags, is_waste, income_raw_id
)
SELECT entry_date,
       transaction_type,
       description,
       description,
       NULL,
       tags,
       false,
       raw_id
FROM tmp_eligible_incomes;

WITH bases AS (
    SELECT t.id AS transaction_id,
           i.account_id,
           i.category_item_id,
           i.amount
    FROM tmp_eligible_incomes i
    JOIN public.transaction t ON t.income_raw_id = i.raw_id
)
INSERT INTO public.ledger_entry (
    transaction_id, account_id, category_item_id, amount, entry_type
)
SELECT transaction_id, account_id, NULL::int, amount, 'ASSET'
FROM bases
UNION ALL
SELECT transaction_id, NULL::int, category_item_id, -amount, 'REVENUE'
FROM bases;

-- =============================================================================
-- 2-3. 수입 원천: 지출 취소 및 환불(REFUND)
-- =============================================================================
CREATE TEMP TABLE tmp_eligible_refunds ON COMMIT DROP AS
SELECT i.raw_id,
       i.entry_date,
       i.transaction_type,
       i.target_merchant AS merchant,
       i.description,
       i.amount,
       a.id AS account_id,
       ci.id AS category_item_id,
       i.tags
FROM stage.income_curated i
LEFT JOIN public.account a ON a.name = i.deposit_account
LEFT JOIN public.category c
  ON c.type = 'EXPENSE'
 AND c.name = split_part(i.target_category, '>', 1)
LEFT JOIN public.category_item ci
  ON ci.category_id = c.id
 AND ci.name = split_part(i.target_category, '>', 2)
WHERE i.transaction_type = 'REFUND';

INSERT INTO public.transaction (
    transaction_date, type, merchant, description, payment_method,
    tags, is_waste, income_raw_id
)
SELECT entry_date,
       transaction_type,
       merchant,
       description,
       NULL,
       tags,
       false,
       raw_id
FROM tmp_eligible_refunds;

WITH bases AS (
    SELECT t.id AS transaction_id,
           r.account_id,
           r.category_item_id,
           r.amount
    FROM tmp_eligible_refunds r
    JOIN public.transaction t ON t.income_raw_id = r.raw_id
)
INSERT INTO public.ledger_entry (
    transaction_id, account_id, category_item_id, amount, entry_type
)
SELECT transaction_id, account_id, NULL::int, amount, 'ASSET'
FROM bases
UNION ALL
SELECT transaction_id, NULL::int, category_item_id, -amount, 'EXPENSE'
FROM bases;

-- =============================================================================
-- 2-4. 지출 원천: 계정 간 이체(TRANSFER, 선불 충전 및 카드대금 포함)
-- =============================================================================
CREATE TEMP TABLE tmp_eligible_transfers ON COMMIT DROP AS
SELECT o.raw_id,
       o.entry_date,
       o.transaction_type,
       split_part(o.category, '>', 2) AS merchant,
       o.description,
       coalesce(nullif(o.cash_amount, 0), nullif(o.card_amount, 0)) AS amount,
       sa.id AS source_account_id,
       sa.type AS source_account_type,
       ta.id AS target_account_id,
       ta.type AS target_account_type,
       o.tags,
       o.is_waste
FROM stage.outgo_curated o
LEFT JOIN public.account sa
  ON sa.name = coalesce(o.bank_account, o.card_name)
LEFT JOIN public.account ta
  ON ta.name = split_part(o.category, '>', 2)
WHERE o.transaction_type = 'TRANSFER';

INSERT INTO public.transaction (
    transaction_date, type, merchant, description, payment_method,
    tags, is_waste, outgo_raw_id
)
SELECT entry_date,
       transaction_type,
       merchant,
       description,
       NULL,
       tags,
       is_waste,
       raw_id
FROM tmp_eligible_transfers;

WITH bases AS (
    SELECT t.id AS transaction_id,
           x.source_account_id,
           x.source_account_type,
           x.target_account_id,
           x.target_account_type,
           x.amount
    FROM tmp_eligible_transfers x
    JOIN public.transaction t ON t.outgo_raw_id = x.raw_id
)
INSERT INTO public.ledger_entry (
    transaction_id, account_id, category_item_id, amount, entry_type
)
SELECT transaction_id, source_account_id, NULL::int, -amount, source_account_type
FROM bases
UNION ALL
SELECT transaction_id, target_account_id, NULL::int, amount, target_account_type
FROM bases;

-- =============================================================================
-- 2-5. 수입 원천: 기초 잔액(OPENING_BALANCE)
-- =============================================================================
CREATE TEMP TABLE tmp_eligible_equities ON COMMIT DROP AS
SELECT i.raw_id,
       i.entry_date,
       i.transaction_type,
       i.amount,
       i.deposit_account AS account_name,
       a.id AS account_id,
       i.tags
FROM stage.income_curated i
LEFT JOIN public.account a ON a.name = i.deposit_account
WHERE i.transaction_type = 'OPENING_BALANCE';

INSERT INTO public.transaction (
    transaction_date, type, merchant, description, payment_method,
    tags, is_waste, income_raw_id
)
SELECT entry_date,
       transaction_type,
       '장부 시작 기초 잔액',
       account_name || ' 시작 금액',
       NULL,
       tags,
       false,
       raw_id
FROM tmp_eligible_equities;

WITH bases AS (
    SELECT t.id AS transaction_id,
           e.account_id,
           e.amount
    FROM tmp_eligible_equities e
    JOIN public.transaction t ON t.income_raw_id = e.raw_id
)
INSERT INTO public.ledger_entry (
    transaction_id, account_id, category_item_id, amount, entry_type
)
SELECT transaction_id, account_id, NULL::int, amount, 'ASSET'
FROM bases
UNION ALL
SELECT transaction_id,
       (SELECT id FROM public.account WHERE type = 'EQUITY' AND subtype = 'CAPITAL'),
       NULL::int,
       -amount,
       'EQUITY'
FROM bases;

-- =============================================================================
-- 3. 완성된 원장을 기준으로 account.opening_date 최종 확정
-- =============================================================================
-- 금융 계정별 최초 분개 거래일을 적용하고, 거래가 없는 계정은 CURRENT_DATE를 적용한다.
-- 내부 CAPITAL 계정은 상징적 시작일 1970-01-01을 유지한다.
UPDATE public.account a
SET opening_date = CASE
    WHEN a.type = 'EQUITY' AND a.subtype = 'CAPITAL'
        THEN DATE '1970-01-01'
    ELSE coalesce(
        (
            SELECT min(t.transaction_date)
            FROM public.ledger_entry le
            JOIN public.transaction t ON t.id = le.transaction_id
            WHERE le.account_id = a.id
        ),
        CURRENT_DATE
    )
END;

COMMIT;
