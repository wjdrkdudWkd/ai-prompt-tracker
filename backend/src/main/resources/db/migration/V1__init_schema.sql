-- AI Call Records Table
CREATE TABLE ai_call_records (
    id BIGSERIAL PRIMARY KEY,
    provider_name VARCHAR(50) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    function_name VARCHAR(200) NOT NULL,
    function_description VARCHAR(500),
    category VARCHAR(100),
    prompt TEXT,
    response_content TEXT,
    prompt_tokens INTEGER,
    completion_tokens INTEGER,
    total_tokens INTEGER,
    estimated_cost DECIMAL(10, 6),
    response_time_ms BIGINT,
    finish_reason VARCHAR(50),
    success BOOLEAN DEFAULT true,
    error_message TEXT,
    cache_hit BOOLEAN DEFAULT false,
    parameters TEXT,
    tags VARCHAR(500),
    user_id VARCHAR(100),
    session_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for ai_call_records
CREATE INDEX idx_provider_model ON ai_call_records(provider_name, model_name);
CREATE INDEX idx_function_name ON ai_call_records(function_name);
CREATE INDEX idx_created_at ON ai_call_records(created_at);
CREATE INDEX idx_category ON ai_call_records(category);

-- Daily Stats Table
CREATE TABLE daily_stats (
    id BIGSERIAL PRIMARY KEY,
    stats_date DATE NOT NULL,
    provider_name VARCHAR(50) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    function_name VARCHAR(200) NOT NULL,
    category VARCHAR(100),
    total_calls INTEGER DEFAULT 0,
    success_calls INTEGER DEFAULT 0,
    failed_calls INTEGER DEFAULT 0,
    total_prompt_tokens BIGINT DEFAULT 0,
    total_completion_tokens BIGINT DEFAULT 0,
    total_tokens BIGINT DEFAULT 0,
    total_cost DECIMAL(10, 6) DEFAULT 0,
    avg_response_time_ms BIGINT,
    min_response_time_ms BIGINT,
    max_response_time_ms BIGINT,
    cache_hits INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for daily_stats
CREATE INDEX idx_stats_date ON daily_stats(stats_date);
CREATE INDEX idx_provider_date ON daily_stats(provider_name, stats_date);
CREATE INDEX idx_function_date ON daily_stats(function_name, stats_date);

-- Unique constraint for daily_stats
CREATE UNIQUE INDEX uk_provider_model_function_date
ON daily_stats(provider_name, model_name, function_name, stats_date);

-- Comments
COMMENT ON TABLE ai_call_records IS 'AI API 호출 기록';
COMMENT ON TABLE daily_stats IS '일별 통계 집계';
COMMENT ON COLUMN ai_call_records.estimated_cost IS '예상 비용 (USD, per 1000 tokens)';
COMMENT ON COLUMN ai_call_records.response_time_ms IS '응답 시간 (밀리초)';
COMMENT ON COLUMN daily_stats.total_cost IS '총 비용 (USD)';
