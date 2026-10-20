-- Grimault / 재현 가능한 개발용 더미 데이터 (DataGrip 실행용)
-- 명세: docs/dev/seed-data-spec.md
-- 전제: initialization-dev.sql을 통해 grimault_dev_db 및 스키마 구성 완료
--
-- DataGrip에서 연결 대상을 grimault_dev_db / grimault_dev로 직접 확인한 후,
-- 이 파일 전체를 하나의 실행 단위로 실행한다.
-- 실행 시 개발 DB의 account, category, transaction, ledger_entry 데이터를 모두 초기화한다.
-- 초기 데이터셋은 매번 같은 ID, 거래 금액, 일시로 재구성된다.
-- 원본 데이터나 개인정보는 사용하지 않는다.
--
-- 데이터 초기화와 적재는 PostgreSQL의 단일 트랜잭션으로 처리한다.

BEGIN;

TRUNCATE TABLE public.ledger_entry, public.transaction, public.category, public.account
    RESTART IDENTITY;

-- ID-stable account fixtures. CREDIT_CARD jsonb mirrors Kotlin CreditCardMetadata.
INSERT INTO public.account (id, name, type, subtype, credit_card_metadata, is_active, created_at) VALUES
    (1, '개발_주거래통장', 'ASSET', 'BANK', NULL, true, '2026-01-01 00:00:00+09'),
    (2, '개발_생활비통장', 'ASSET', 'BANK', NULL, true, '2026-01-01 00:00:00+09'),
    (3, '개발_비상금통장', 'ASSET', 'BANK', NULL, true, '2026-01-01 00:00:00+09'),
    (4, '개발_현금지갑', 'ASSET', 'CASH', NULL, true, '2026-01-01 00:00:00+09'),
    (5, '개발_페이머니', 'ASSET', 'PREPAID', NULL, true, '2026-01-01 00:00:00+09'),
    (6, '개발_신한카드', 'LIABILITY', 'CREDIT_CARD', '{"issuer":"SHINHAN","billingPeriod":{"startMonthOffset":-1,"startDay":20,"endMonthOffset":0,"endDay":19},"settlementDay":15}', true, '2026-01-01 00:00:00+09'),
    (7, '개발_하나카드(해지)', 'LIABILITY', 'CREDIT_CARD', '{"issuer":"HANA","billingPeriod":{"startMonthOffset":-1,"startDay":5,"endMonthOffset":0,"endDay":4},"settlementDay":25}', false, '2026-01-01 00:00:00+09'),
    (8, '개발_구은행(해지)', 'ASSET', 'BANK', NULL, false, '2026-01-01 00:00:00+09'),
    (9, '개발_소액대출', 'LIABILITY', 'LOAN', NULL, true, '2026-01-01 00:00:00+09'),
    (10, '개발_기초자본', 'EQUITY', 'CAPITAL', NULL, false, '2026-01-01 00:00:00+09');

-- Master categories (16 expense + 4 income; 1 inactive historical category).
INSERT INTO public.category (id, type, parent_name, sub_name, is_active) VALUES
    (1, 'EXPENSE', '식비', '외식', true),
    (2, 'EXPENSE', '식비', '식료품', true),
    (3, 'EXPENSE', '식비', '카페', true),
    (4, 'EXPENSE', '주거/통신', '통신비', true),
    (5, 'EXPENSE', '주거/통신', '월세', true),
    (6, 'EXPENSE', '생활', '생필품', true),
    (7, 'EXPENSE', '생활', '의류', true),
    (8, 'EXPENSE', '교통', '대중교통', true),
    (9, 'EXPENSE', '교통', '택시', true),
    (10, 'EXPENSE', '문화', '구독', true),
    (11, 'EXPENSE', '문화', '영화', true),
    (12, 'EXPENSE', '건강', '병원', true),
    (13, 'EXPENSE', '건강', '약국', true),
    (14, 'EXPENSE', '기타', '수수료', true),
    (15, 'EXPENSE', '기타', '기부', true),
    (16, 'EXPENSE', '기타', '기타', false),
    (17, 'INCOME', '급여', '월급', true),
    (18, 'INCOME', '부수입', '프로젝트', true),
    (19, 'INCOME', '금융', '이자', true),
    (20, 'INCOME', '기타', '환급', true);

-- Baseline scenarios (IDs 1..35); created_at uses a fixed timestamp plus ID minutes.
WITH baseline (id, transaction_date, merchant, description, payment_method, tags, is_waste) AS (
    VALUES
    (1, '2026-01-01', '개발_초기설정', '주거래통장 기초잔액', NULL, 'fixture,opening', false),
    (2, '2026-01-01', '개발_초기설정', '생활비통장 기초잔액', NULL, 'fixture,opening', false),
    (3, '2026-01-01', '개발_초기설정', '비상금통장 기초잔액', NULL, 'fixture,opening', false),
    (4, '2026-01-01', '개발_초기설정', '현금지갑 기초잔액', NULL, 'fixture,opening', false),
    (5, '2026-01-01', '개발_초기설정', '페이머니 기초잔액', NULL, 'fixture,opening', false),
    (6, '2026-01-01', '개발_초기설정', '해지은행 과거 잔액', NULL, 'fixture,opening', false),
    (7, '2026-01-01', '개발_초기설정', '소액대출 기초 부채', NULL, 'fixture,opening', false),
    (8, '2026-01-02', '개발_계좌이체', '해지은행 잔액 전액 이체', NULL, 'fixture,transfer', false),
    (9, '2026-01-03', '개발_회사', '급여 입금', NULL, 'fixture,income', false),
    (10, '2026-01-04', '개발_프로젝트', '프로젝트 정산금', NULL, 'fixture,income', false),
    (11, '2026-01-05', '개발_은행', '예금 이자', NULL, 'fixture,income', false),
    (12, '2026-01-06', '개발_마트', '식료품 구매', '체크카드', 'fixture,expense', false),
    (13, '2026-01-07', '개발_식당', '카드 외식', '신한카드', 'fixture,expense', false),
    (14, '2026-01-08', '개발_스트리밍', '카드 구독료', '신한카드', 'fixture,expense', false),
    (15, '2026-01-09', '개발_카드대금', '신한카드 부분 상환', NULL, 'fixture,settlement', false),
    (16, '2026-01-10', '개발_ATM', '현금 인출', NULL, 'fixture,transfer', false),
    (17, '2026-01-11', '개발_페이충전', '생활비통장에서 페이 충전', NULL, 'fixture,transfer', false),
    (18, '2026-01-12', '개발_카페', '페이머니로 커피 구매', '페이머니', 'fixture,expense', false),
    (19, '2026-01-13', '개발_버스', '현금 교통비', '현금', 'fixture,expense', false),
    (20, '2026-01-14', '개발_임대인', '월세 납부', NULL, 'fixture,expense', false),
    (21, '2026-01-15', '개발_대출', '대출 원금 일부 상환', NULL, 'fixture,loan', false),
    (22, '2026-01-16', '개발_대출', '대출 원금 추가 차입', NULL, 'fixture,loan', false),
    (23, '2026-01-17', '개발_의류점', '해지카드 과거 구매', '하나카드', 'fixture,historical', false),
    (24, '2026-01-18', '개발_카드대금', '해지카드 대금 상환', NULL, 'fixture,settlement', false),
    (25, '2026-01-19', '개발_계좌이체', '비상금 저축 이동', NULL, 'fixture,transfer', false),
    (26, '2026-01-20', '개발_환급처', '생활비통장 환급금', NULL, 'fixture,income', false),
    (27, '2026-01-21', '개발_페이충전', '신한카드로 페이 충전', '신한카드', 'fixture,transfer', false),
    (28, '2026-01-22', '개발_상점', '페이머니 생필품 구매', '페이머니', 'fixture,expense', false),
    (29, '2026-01-23', '개발_은행', '송금 수수료', NULL, 'fixture,expense', false),
    (30, '2026-01-24', '개발_기부처', '기부금', NULL, 'fixture,expense', false),
    (31, '2026-01-25', '개발_카페', '커피 구매', '체크카드', 'fixture,expense', true),
    (32, '2026-01-26', '개발_병원', '병원비 신용카드 결제', '신한카드', 'fixture,expense', false),
    (33, '2026-01-27', '개발_환급처', '주거래통장 환급금', NULL, 'fixture,income', false),
    (34, '2026-01-28', '개발_기타매장', '과거 기타 지출', '현금', 'fixture,expense', false),
    (35, '2026-01-29', '개발_영화관', '영화 관람', '체크카드', 'fixture,expense', true)
)
INSERT INTO public.transaction
    (id, transaction_date, merchant, description, payment_method, tags, is_waste, created_at)
SELECT id, transaction_date::date, merchant, description, payment_method, tags, is_waste,
       TIMESTAMPTZ '2026-01-01 00:00:00+09' + id * INTERVAL '1 minute'
FROM baseline ORDER BY id;

-- Baseline ledger postings: each transaction has exactly two signed legs.
INSERT INTO public.ledger_entry (transaction_id, account_id, category_id, amount, entry_type) VALUES
    (1, 1, NULL, 3000000, 'ASSET'),
    (1, 10, NULL, -3000000, 'EQUITY'),
    (2, 2, NULL, 500000, 'ASSET'),
    (2, 10, NULL, -500000, 'EQUITY'),
    (3, 3, NULL, 1000000, 'ASSET'),
    (3, 10, NULL, -1000000, 'EQUITY'),
    (4, 4, NULL, 80000, 'ASSET'),
    (4, 10, NULL, -80000, 'EQUITY'),
    (5, 5, NULL, 50000, 'ASSET'),
    (5, 10, NULL, -50000, 'EQUITY'),
    (6, 8, NULL, 100000, 'ASSET'),
    (6, 10, NULL, -100000, 'EQUITY'),
    (7, 9, NULL, -400000, 'LIABILITY'),
    (7, 10, NULL, 400000, 'EQUITY'),
    (8, 8, NULL, -100000, 'ASSET'),
    (8, 1, NULL, 100000, 'ASSET'),
    (9, 1, NULL, 600000, 'ASSET'),
    (9, NULL, 17, -600000, 'REVENUE'),
    (10, 2, NULL, 125000, 'ASSET'),
    (10, NULL, 18, -125000, 'REVENUE'),
    (11, 3, NULL, 3500, 'ASSET'),
    (11, NULL, 19, -3500, 'REVENUE'),
    (12, 2, NULL, -48000, 'ASSET'),
    (12, NULL, 2, 48000, 'EXPENSE'),
    (13, 6, NULL, -35000, 'LIABILITY'),
    (13, NULL, 1, 35000, 'EXPENSE'),
    (14, 6, NULL, -14900, 'LIABILITY'),
    (14, NULL, 10, 14900, 'EXPENSE'),
    (15, 1, NULL, -30000, 'ASSET'),
    (15, 6, NULL, 30000, 'LIABILITY'),
    (16, 1, NULL, -70000, 'ASSET'),
    (16, 4, NULL, 70000, 'ASSET'),
    (17, 2, NULL, -60000, 'ASSET'),
    (17, 5, NULL, 60000, 'ASSET'),
    (18, 5, NULL, -12000, 'ASSET'),
    (18, NULL, 3, 12000, 'EXPENSE'),
    (19, 4, NULL, -8000, 'ASSET'),
    (19, NULL, 8, 8000, 'EXPENSE'),
    (20, 1, NULL, -450000, 'ASSET'),
    (20, NULL, 5, 450000, 'EXPENSE'),
    (21, 1, NULL, -50000, 'ASSET'),
    (21, 9, NULL, 50000, 'LIABILITY'),
    (22, 1, NULL, 100000, 'ASSET'),
    (22, 9, NULL, -100000, 'LIABILITY'),
    (23, 7, NULL, -24000, 'LIABILITY'),
    (23, NULL, 7, 24000, 'EXPENSE'),
    (24, 1, NULL, -24000, 'ASSET'),
    (24, 7, NULL, 24000, 'LIABILITY'),
    (25, 1, NULL, -200000, 'ASSET'),
    (25, 3, NULL, 200000, 'ASSET'),
    (26, 2, NULL, 7500, 'ASSET'),
    (26, NULL, 20, -7500, 'REVENUE'),
    (27, 6, NULL, -25000, 'LIABILITY'),
    (27, 5, NULL, 25000, 'ASSET'),
    (28, 5, NULL, -18000, 'ASSET'),
    (28, NULL, 6, 18000, 'EXPENSE'),
    (29, 1, NULL, -1500, 'ASSET'),
    (29, NULL, 14, 1500, 'EXPENSE'),
    (30, 1, NULL, -10000, 'ASSET'),
    (30, NULL, 15, 10000, 'EXPENSE'),
    (31, 1, NULL, -5500, 'ASSET'),
    (31, NULL, 3, 5500, 'EXPENSE'),
    (32, 6, NULL, -47000, 'LIABILITY'),
    (32, NULL, 12, 47000, 'EXPENSE'),
    (33, 1, NULL, 10000, 'ASSET'),
    (33, NULL, 20, -10000, 'REVENUE'),
    (34, 4, NULL, -3000, 'ASSET'),
    (34, NULL, 16, 3000, 'EXPENSE'),
    (35, 1, NULL, -21000, 'ASSET'),
    (35, NULL, 11, 21000, 'EXPENSE');

-- 12 weeks x 7 fixed weekly scenarios = 84 additional transactions (IDs 36..119).
-- Events repeat with stable amounts; no random() and no clock-dependent fields.
WITH weekly (week_no, slot, day_offset, merchant, description, payment_method) AS (
    SELECT w, v.slot, v.day_offset, v.merchant, v.description, v.payment_method
    FROM generate_series(0, 11) AS w
    CROSS JOIN LATERAL (VALUES
        (0, 0, '개발_분식집', '주간 외식', '체크카드'),
        (1, 1, '개발_마켓', '주간 장보기', '체크카드'),
        (2, 2, '개발_버스', '주간 대중교통', '현금'),
        (3, 3, '개발_카페', '주간 카드 커피', '신한카드'),
        (4, 4, '개발_스트리밍', '주간 구독료', '신한카드'),
        (5, 5, '개발_프로젝트', '주간 부수입', NULL),
        (6, 6, '개발_계좌이체', '주간 생활비 이체', NULL)
    ) AS v(slot, day_offset, merchant, description, payment_method)
)
INSERT INTO public.transaction
    (id, transaction_date, merchant, description, payment_method, tags, is_waste, created_at)
SELECT 36 + 7 * week_no + slot,
       DATE '2026-01-05' + 7 * week_no + day_offset,
       merchant,
       description || ' (' || lpad((week_no + 1)::text, 2, '0') || '주차)',
       payment_method,
       'fixture,weekly',
       slot = 3 AND week_no % 3 = 0,
       TIMESTAMPTZ '2026-01-01 00:00:00+09'
         + (36 + 7 * week_no + slot) * INTERVAL '1 minute'
FROM weekly
ORDER BY week_no, slot;

-- Weekly posting template: week number affects transaction ID only.
WITH weekly_legs (week_no, slot, leg_order, account_id, category_id, amount, entry_type) AS (
    SELECT w, v.slot, v.leg_order, v.account_id, v.category_id, v.amount, v.entry_type
    FROM generate_series(0, 11) AS w
    CROSS JOIN LATERAL (VALUES
    (0, 1, 2, NULL, -12500, 'ASSET'),
    (0, 2, NULL, 1, 12500, 'EXPENSE'),
    (1, 1, 2, NULL, -32000, 'ASSET'),
    (1, 2, NULL, 2, 32000, 'EXPENSE'),
    (2, 1, 4, NULL, -2750, 'ASSET'),
    (2, 2, NULL, 8, 2750, 'EXPENSE'),
    (3, 1, 6, NULL, -4900, 'LIABILITY'),
    (3, 2, NULL, 3, 4900, 'EXPENSE'),
    (4, 1, 6, NULL, -7900, 'LIABILITY'),
    (4, 2, NULL, 10, 7900, 'EXPENSE'),
    (5, 1, 2, NULL, 42000, 'ASSET'),
    (5, 2, NULL, 18, -42000, 'REVENUE'),
    (6, 1, 1, NULL, -25000, 'ASSET'),
    (6, 2, 2, NULL, 25000, 'ASSET')
    ) AS v(slot, leg_order, account_id, category_id, amount, entry_type)
)
INSERT INTO public.ledger_entry (transaction_id, account_id, category_id, amount, entry_type)
SELECT 36 + 7 * week_no + slot, account_id, category_id, amount, entry_type
FROM weekly_legs ORDER BY week_no, slot, leg_order;

-- 명시적 마스터 PK 이후 API INSERT를 위한 시퀀스 재설정.
-- ALTER SEQUENCE RESTART는 트랜잭션에서 롤백 가능하다.
ALTER SEQUENCE public.account_id_seq RESTART WITH 11;
ALTER SEQUENCE public.category_id_seq RESTART WITH 21;
ALTER SEQUENCE public.transaction_id_seq RESTART WITH 120;
-- ledger_entry ID는 적재 중 SERIAL에서 자동 발급되므로 별도 조정하지 않는다.

COMMIT;
