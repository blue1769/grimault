SELECT markdown_row
FROM (
         -- 1. 헤더 행
         SELECT 1 AS ord, '| **Column Name** | **Data Type** | **Nullable** | **Description** |' AS markdown_row
         UNION ALL
         -- 2. 구분선 행
         SELECT 2 AS ord, '| --- | --- | --- | --- |' AS markdown_row
         UNION ALL
         -- 3. 데이터 행 (원래 테이블 컬럼 순서인 ordinal_position 보장)
         SELECT
             c.ordinal_position + 2 AS ord,
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
           AND c.table_name = 'ledger_entry'
     ) t
ORDER BY ord;