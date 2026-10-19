-- Grimault / 개발 DB 데이터 무결성 진단 (DataGrip 실행용, 읽기 전용)
-- 시딩 직후와 API 개발 도중에 동일하게 사용한다.
-- 최초 시드의 행 수, 기대 잔액, 레코드별 스냅샷은 대조하지 않는다.
-- 계정 분류/카드 메타데이터/분개 비즈니스 유형은 API가 검증한다.
-- FK가 보장하는 참조 대상의 실존 여부는 중복 검사하지 않는다.
--
-- [1] 참고용 테이블 건수 및 원장 합계 (고정값과 비교하지 않음)
SELECT
    (SELECT count(*) FROM public.account) AS account_count,
    (SELECT count(*) FROM public.category) AS category_count,
    (SELECT count(*) FROM public.transaction) AS transaction_count,
    (SELECT count(*) FROM public.ledger_entry) AS ledger_entry_count,
    (SELECT COALESCE(sum(amount), 0) FROM public.ledger_entry) AS ledger_sum;

-- [2] DB 자체의 구조적·회계적 무결성 진단
-- PASS가 1행 나오면 검출된 위반이 없다.
-- FAIL이 나오면 해당 transaction_id / ledger_entry_id와 설명을 확인한다.
-- 거래의 분개는 최소 2개여야 한다. 3개 이상의 분개는 허용한다.
-- '참조 누락'은 nullable account_id/category_id의 값이 모두 빠진 경우다.
-- 유효한 참조의 실존 여부는 기존 FK 제약이 보장한다.
WITH transaction_summary AS (
    SELECT
        t.id AS transaction_id,
        count(e.id) AS posting_count,
        COALESCE(sum(e.amount), 0::numeric) AS amount_sum
    FROM public.transaction AS t
    LEFT JOIN public.ledger_entry AS e ON e.transaction_id = t.id
    GROUP BY t.id
),
violations AS (
    SELECT
        'MISSING_POSTINGS'::text AS check_code,
        ts.transaction_id::bigint AS transaction_id,
        NULL::bigint AS ledger_entry_id,
        ('분개 수: ' || ts.posting_count || '개 (최소 2개 필요)')::text AS details
    FROM transaction_summary AS ts
    WHERE ts.posting_count < 2

    UNION ALL

    SELECT
        'NON_ZERO_SUM',
        ts.transaction_id::bigint,
        NULL::bigint,
        ('분개 합계: ' || ts.amount_sum::text || ' (0이어야 함)')::text
    FROM transaction_summary AS ts
    WHERE ts.amount_sum <> 0

    UNION ALL

    SELECT
        'MISSING_ENTRY_REFERENCE',
        e.transaction_id::bigint,
        e.id::bigint,
        'account_id와 category_id가 모두 NULL'::text
    FROM public.ledger_entry AS e
    WHERE e.account_id IS NULL AND e.category_id IS NULL

    UNION ALL

    SELECT
        'MULTIPLE_ENTRY_REFERENCES',
        e.transaction_id::bigint,
        e.id::bigint,
        'account_id와 category_id가 동시에 존재'::text
    FROM public.ledger_entry AS e
    WHERE e.account_id IS NOT NULL AND e.category_id IS NOT NULL
)
SELECT
    'FAIL'::text AS status,
    v.check_code,
    v.transaction_id,
    v.ledger_entry_id,
    v.details
FROM violations AS v

UNION ALL

SELECT
    'PASS'::text,
    'INTEGRITY_OK'::text,
    NULL::bigint,
    NULL::bigint,
    '거래별 Zero-Sum, 분개 수, 참조 칼럼 구성이 정상'::text
WHERE NOT EXISTS (SELECT 1 FROM violations)

ORDER BY status, check_code, transaction_id, ledger_entry_id;
