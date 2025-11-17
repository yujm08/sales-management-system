-- 매출 관리 시스템 초기 스키마 생성

-- 회사 테이블 생성
CREATE TABLE companies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    is_mynet BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 사용자 테이블 생성
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    role VARCHAR(50) NOT NULL CHECK (role IN ('ADMIN', 'USER')),
    is_canon BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 제품 테이블 생성
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    product_code VARCHAR(10) NOT NULL UNIQUE,
    product_name VARCHAR(500) NOT NULL, -- 제품명 글자 제한 없음
    category VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 제품 가격 이력 테이블 생성 (과거 가격 보존용)
CREATE TABLE product_price_history (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    cost_price DECIMAL(10,2) NOT NULL,
    supply_price DECIMAL(10,2) NOT NULL,
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL,
    effective_to TIMESTAMP WITH TIME ZONE NULL, -- NULL이면 현재 적용 중
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL
);

-- 일별 실적 테이블 생성
CREATE TABLE daily_sales (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    sales_date DATE NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0,
    last_modified_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    modified_by VARCHAR(255),
    UNIQUE(company_id, product_id, sales_date) -- 회사별, 제품별, 날짜별 유니크
);

-- 목표 설정 테이블 생성
CREATE TABLE targets (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NULL REFERENCES companies(id), -- NULL이면 전체 목표
    product_id BIGINT NOT NULL REFERENCES products(id),
    target_year INTEGER NOT NULL,
    target_month INTEGER NOT NULL CHECK (target_month BETWEEN 1 AND 12),
    target_quantity INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    UNIQUE(company_id, product_id, target_year, target_month)
);

-- 인덱스 생성 (성능 최적화)
CREATE INDEX idx_daily_sales_date ON daily_sales(sales_date);
CREATE INDEX idx_daily_sales_company ON daily_sales(company_id);
CREATE INDEX idx_daily_sales_product ON daily_sales(product_id);
CREATE INDEX idx_price_history_effective ON product_price_history(product_id, effective_from, effective_to);
CREATE INDEX idx_targets_period ON targets(target_year, target_month);

-- 마이넷 회사 생성
INSERT INTO companies (name, is_mynet) VALUES ('마이넷', TRUE);

-- 기본 하위 회사들 생성 (실제 회사명 반영)
INSERT INTO companies (name, is_mynet) VALUES ('영현아이앤씨', FALSE);
INSERT INTO companies (name, is_mynet) VALUES ('마이씨앤에스', FALSE);
INSERT INTO companies (name, is_mynet) VALUES ('우리STM', FALSE);
INSERT INTO companies (name, is_mynet) VALUES ('엠에스앤샵', FALSE);
INSERT INTO companies (name, is_mynet) VALUES ('원이스토리 (쿠팡)', FALSE);
INSERT INTO companies (name, is_mynet) VALUES ('대현씨앤씨', FALSE);
INSERT INTO companies (name, is_mynet) VALUES ('마이넷 (GX판매)', FALSE);

-- 캐논 계정용 더미 회사 생성
INSERT INTO companies (name, is_mynet) VALUES ('캐논', FALSE);