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