-- Drop old tables (from V1 and V2)
DROP TABLE IF EXISTS ai_call_records CASCADE;
DROP TABLE IF EXISTS daily_stats CASCADE;

-- Create executions table
CREATE TABLE executions (
    id VARCHAR(36) PRIMARY KEY,
    function_name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    tags TEXT[],
    environment VARCHAR(20) NOT NULL,

    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP,
    duration_ms BIGINT,

    status VARCHAR(20) NOT NULL,
    error_message TEXT,

    -- Aggregated metrics from calls
    calls_count INTEGER DEFAULT 0,
    total_cost DECIMAL(10, 6),
    total_tokens BIGINT
);

-- Create indexes for executions
CREATE INDEX idx_executions_function ON executions(function_name);
CREATE INDEX idx_executions_started_at ON executions(started_at);
CREATE INDEX idx_executions_environment ON executions(environment);
CREATE INDEX idx_executions_status ON executions(status);

-- Create calls table
CREATE TABLE calls (
    id VARCHAR(36) PRIMARY KEY,
    execution_id VARCHAR(36) NOT NULL,

    provider VARCHAR(50) NOT NULL,
    model VARCHAR(100) NOT NULL,

    prompt_tokens INTEGER,
    completion_tokens INTEGER,
    total_tokens INTEGER,

    cost DECIMAL(10, 6),
    latency_ms BIGINT NOT NULL,

    status VARCHAR(20) NOT NULL,
    error_type VARCHAR(100),
    error_message TEXT,

    -- Dev/test only fields
    request_preview TEXT,
    response_preview TEXT,
    raw_json TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key to executions
    CONSTRAINT fk_calls_execution
        FOREIGN KEY (execution_id)
        REFERENCES executions(id)
        ON DELETE CASCADE
);

-- Create indexes for calls
CREATE INDEX idx_calls_execution ON calls(execution_id);
CREATE INDEX idx_calls_provider ON calls(provider);
CREATE INDEX idx_calls_created_at ON calls(created_at);
CREATE INDEX idx_calls_status ON calls(status);

-- Additional performance indexes
CREATE INDEX idx_calls_provider_model ON calls(provider, model);
CREATE INDEX idx_calls_cost ON calls(cost DESC) WHERE cost IS NOT NULL;
CREATE INDEX idx_executions_function_time ON executions(function_name, started_at DESC);

-- Comments
COMMENT ON TABLE executions IS 'Tracks @AIPrompt method executions (1 execution = N calls)';
COMMENT ON TABLE calls IS 'Tracks individual AI API HTTP calls';
COMMENT ON COLUMN executions.calls_count IS 'Number of AI API calls made during this execution';
COMMENT ON COLUMN executions.total_cost IS 'Total cost across all calls (USD)';
COMMENT ON COLUMN calls.cost IS 'Estimated cost for this single call (USD)';
COMMENT ON COLUMN calls.latency_ms IS 'HTTP request latency in milliseconds';
