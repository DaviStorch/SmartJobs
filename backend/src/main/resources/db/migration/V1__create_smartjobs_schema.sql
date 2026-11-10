CREATE TABLE app_user (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    theme VARCHAR(10) NOT NULL DEFAULT 'light',
    language VARCHAR(5) NOT NULL DEFAULT 'pt',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE resume (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_resume_user_active ON resume(user_id, active);

CREATE TABLE resume_section (
    id BIGSERIAL PRIMARY KEY,
    resume_id BIGINT NOT NULL REFERENCES resume(id) ON DELETE CASCADE,
    section_type VARCHAR(30) NOT NULL,
    content TEXT NOT NULL DEFAULT '',
    CONSTRAINT uq_resume_section UNIQUE (resume_id, section_type)
);

CREATE TABLE job_opportunity (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    title VARCHAR(180) NOT NULL,
    company VARCHAR(160) NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    location VARCHAR(160),
    work_mode VARCHAR(30),
    salary VARCHAR(120),
    contract_type VARCHAR(80),
    experience_level VARCHAR(80),
    requirements TEXT,
    benefits TEXT,
    published_at DATE,
    source VARCHAR(100),
    source_url VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    match_score NUMERIC(3, 2),
    recommendation VARCHAR(20),
    notes TEXT,
    applied_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_job_user_status ON job_opportunity(user_id, status);
CREATE INDEX idx_job_user_created ON job_opportunity(user_id, created_at DESC);
CREATE INDEX idx_job_user_score ON job_opportunity(user_id, match_score DESC);
CREATE INDEX idx_job_user_company_title ON job_opportunity(user_id, company, title);
