
-- =============================================================================
-- Silver Enrichment: Transaction Type Classification
-- =============================================================================
-- 목적:
--   Silver Curated 데이터의 거래 유형을 Gold 적재 전에 확정한다.
--
-- 규칙:
--   1. 기존 Gold 마이그레이션의 거래 유형 판별 조건을 유지한다.
--   2. Gold에 적재하지 않는 원천 기록은 transaction_type = NULL로 둔다.
--   3. 거래 및 분개 생성은 Silver → Gold 단계에서 수행한다.
-- =============================================================================

BEGIN;

-- =============================================================================
-- 1. Outgo: SPENDING / TRANSFER / REFUND
-- =============================================================================
UPDATE stage.outgo_curated
SET transaction_type = CASE
    -- 중복 입력으로 Gold 적재에서 제외한 거래
    WHEN raw_id = 18962
        THEN NULL

    -- 카드 승인취소 및 부분취소 (음수 지출)
    WHEN cash_amount < 0 OR card_amount < 0
        THEN 'REFUND'

    -- 계정 간 이체 및 카드대금 상환
    WHEN split_part(category, '>', 1) IN ('이체/대체', '카드대금')
        THEN 'TRANSFER'

    -- 일반 소비 지출
    WHEN split_part(category, '>', 1) NOT IN ('이체/대체', '카드대금')
        THEN 'SPENDING'

    ELSE NULL
END;

-- =============================================================================
-- 2. Income: INCOME_RECEIPT / REFUND / OPENING_BALANCE
-- =============================================================================
UPDATE stage.income_curated
SET transaction_type = CASE
    -- 선불 자산 충전의 중복 수입 기록 (Outgo에서 TRANSFER 처리)
    WHEN raw_id IN (
        767, 816, 832, 840, 852, 853, 865, 873, 884,
        889, 891, 903, 913, 924, 928, 942, 958
    )
        THEN NULL

    -- 기존 장부의 기초 잔액
    WHEN raw_id IN (3, 4, 5, 480)
        THEN 'OPENING_BALANCE'

    -- 소비 취소 및 환불
    WHEN category = '부수입>환불'
         AND raw_id != 622
        THEN 'REFUND'

    -- 일반 수입
    WHEN split_part(category, '>', 1) != '전월이월'
         AND split_part(category, '>', 2) != '환불'
        THEN 'INCOME_RECEIPT'

    ELSE NULL
END;

COMMIT;
