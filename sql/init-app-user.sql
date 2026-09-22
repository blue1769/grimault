-- =============================================================================
-- Application Dedicated User Provisioning (PoLP)
-- Target Database: grimault_db
-- Executed by: Database Owner / Superuser (grimault_admin)
-- =============================================================================

-- 1. 애플리케이션 전용 런타임 계정 생성
CREATE USER grimault_app WITH PASSWORD '<CHANGE_PASSWORD>';

-- 2. 데이터베이스 접속 권한
GRANT CONNECT ON DATABASE grimault_db TO grimault_app;

-- 3. public 스키마 탐색(USAGE) 허용 (raw, stage는 접근 원천 차단)
GRANT USAGE ON SCHEMA public TO grimault_app;

-- 4. public 테이블 DML 권한만 부여 (DDL 권한 없음 -> 테이블 수정/삭제 물리적 차단)
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO grimault_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO grimault_app;

-- 5. 시퀀스 채번 권한 부여 (PK Auto-increment 대응)
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO grimault_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO grimault_app;