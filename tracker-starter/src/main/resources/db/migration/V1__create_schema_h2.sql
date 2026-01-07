-- Create executions table (H2 compatible)
CREATE TABLE executions (
    id VARCHAR(36) PRIMARY KEY,
    function_name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    tags VARCHAR(1000),  -- Comma-separated tags for H2
    environment VARCHAR(20) NOT NULL,
    
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP,
    duration_ms BIGINT,
    
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    
    calls_count INTEGER DEFAULT 0,
    total_cost DOUBLE PRECISION,
    total_tokens BIGINT
);

-- Create indexes for executions
CREATE INDEX idx_executions_function ON executions(function_name);
CREATE INDEX idx_executions_started_at ON executions(started_at);
CREATE INDEX idx_executions_environment ON executions(environment);

-- Create calls table (H2 compatible)
CREATE TABLE calls (
    id VARCHAR(36) PRIMARY KEY,
    execution_id VARCHAR(36) NOT NULL,
    
    provider VARCHAR(50) NOT NULL,
    model VARCHAR(100) NOT NULL,
    
    prompt_tokens INTEGER,
    completion_tokens INTEGER,
    total_tokens INTEGER,
    
    cost DOUBLE PRECISION,
    latency_ms BIGINT NOT NULL,
    
    status VARCHAR(20) NOT NULL,
    error_type VARCHAR(100),
    error_message TEXT,
    
    request_preview TEXT,
    response_preview TEXT,
    raw_json TEXT,
    was_truncated BOOLEAN DEFAULT FALSE,
    
    created_at TIMESTAMP NOT NULL,
    
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
