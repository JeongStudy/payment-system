-- 1. 스키마가 없으면 생성
CREATE SCHEMA IF NOT EXISTS payment;

SET search_path TO payment;

-- 2. manager 계정에 payment 스키마에 대한 모든 권한 부여 (이미 있으면 아무 일도 없음)
GRANT ALL ON SCHEMA payment TO manager;

-- 3. manager 계정에 payment 스키마 내 모든 테이블에 권한 부여 (이미 있으면 아무 일도 없음)
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA payment TO manager;

-- 4. manager 계정에 payment 스키마 내 모든 시퀀스에 권한 부여 (시퀀스가 생성되는 경우에 대비)
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA payment TO manager;
