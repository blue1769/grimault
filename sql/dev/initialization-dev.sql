-- Grimault / 개발 DB 최초 환경 구성 (DataGrip 수동 실행용)
-- 목적: 기존 PostgreSQL 인스턴스에 개발용 DB와 로그인 역할을 최초 1회 구성한다.
-- 각 구간은 해당 연결의 Query Console에서 사람이 하나씩 선택·실행하고 결과를 확인한다.
-- 이 파일 전체를 일괄 실행하지 않는다. 기존 grimault_db는 변경하지 않는다.
-- 비밀번호는 이 파일에 기재하거나 커밋하지 않는다.
--
-- [1] 관리자 연결 (database: postgres 등 관리용 DB)
--     아직 grimault_dev 역할이 없음을 확인하고 다음 문장만 실행한다.
CREATE USER grimault_dev
    WITH NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION;

-- [2] 같은 관리자 연결
--     역할이 정상 생성된 것을 확인한 후, 다음 문장만 실행한다.
--     CREATE DATABASE는 트랜잭션 블록 밖에서 실행한다 (DataGrip Auto-commit 사용).
CREATE DATABASE grimault_dev_db OWNER grimault_dev;

-- [3] 같은 관리자 연결
--     개발 DB의 PUBLIC 기본 접속 권한만 제한한다. 다른 DB의 권한은 건드리지 않는다.
REVOKE CONNECT, TEMPORARY ON DATABASE grimault_dev_db FROM PUBLIC;

-- [4] 관리자 연결을 grimault_dev_db로 전환한 뒤 실행
--     기존 public 스키마 생성 권한 정책에 영향을 받지 않도록 명시한다.
GRANT USAGE, CREATE ON SCHEMA public TO grimault_dev;

-- [5] 관리자 연결에서 grimault_dev 역할의 로그인 비밀번호를 개별 설정한다.
--     DataGrip의 Role/User 편집 기능을 이용하거나 별도 비저장 콘솔에서 ALTER ROLE을 실행한다.
--     실제 비밀번호를 이 SQL 파일이나 Git에 남기지 않는다.
ALTER USER grimault_dev WITH PASSWORD '<CHANGE_PASSWORD>';

-- [6] 새 연결: database=grimault_dev_db, user=grimault_dev
--     정상 접속을 확인한 다음, 리포지토리의 sql/public-schema.sql을 연다.
--     DataGrip 수동 트랜잭션 모드에서 해당 DDL을 실행한다.
--     전부 성공하면 COMMIT, 실패하면 ROLLBACK하고 원인을 확인한다.
--     DDL을 이 파일에 복제하지 않는다. 스키마 변경은 sql/public-schema.sql에서만 관리한다.
--
-- [7] 개발 사용자 연결에서 다음 조회로 테이블 구성을 확인한다.
--     (조회는 최초 구성 결과 확인용이며 초기화 명령이 아니다.)
SELECT current_database(), current_user, current_schema(), tablename
FROM pg_catalog.pg_tables
WHERE schemaname = 'public'
  AND tablename IN ('account', 'category', 'category_item', 'transaction', 'ledger_entry')
ORDER BY tablename;
--
-- 이후 반복 테스트에는 initialization-dev.sql을 다시 실행하지 않는다.
-- sql/dev/seed-dev.sql로 데이터를 재구성하고 sql/dev/verify-dev.sql로 정합성을 확인한다.
