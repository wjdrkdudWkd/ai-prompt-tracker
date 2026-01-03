-- V5: Add indexes for efficient dashboard queries

-- Executions table indexes
CREATE INDEX IF NOT EXISTS idx_executions_started_at ON executions(started_at);
CREATE INDEX IF NOT EXISTS idx_executions_function_name ON executions(function_name);
CREATE INDEX IF NOT EXISTS idx_executions_environment ON executions(environment);
CREATE INDEX IF NOT EXISTS idx_executions_category ON executions(category);
CREATE INDEX IF NOT EXISTS idx_executions_status ON executions(status);
CREATE INDEX IF NOT EXISTS idx_executions_function_started ON executions(function_name, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_executions_env_started ON executions(environment, started_at DESC);

-- Calls table indexes
CREATE INDEX IF NOT EXISTS idx_calls_created_at ON calls(created_at);
CREATE INDEX IF NOT EXISTS idx_calls_provider ON calls(provider);
CREATE INDEX IF NOT EXISTS idx_calls_model ON calls(model);
CREATE INDEX IF NOT EXISTS idx_calls_status ON calls(status);
CREATE INDEX IF NOT EXISTS idx_calls_execution_created ON calls(execution_id, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_calls_provider_created ON calls(provider, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_calls_model_created ON calls(model, created_at DESC);

-- Composite indexes for common queries
CREATE INDEX IF NOT EXISTS idx_executions_env_status_started ON executions(environment, status, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_calls_provider_status_created ON calls(provider, status, created_at DESC);

-- Comments for documentation
COMMENT ON INDEX idx_executions_started_at IS 'Used for time range filtering in dashboard queries';
COMMENT ON INDEX idx_executions_function_name IS 'Used for function aggregation and lookup';
COMMENT ON INDEX idx_executions_function_started IS 'Composite index for function history with time ordering';
COMMENT ON INDEX idx_calls_execution_created IS 'Used for execution detail call timeline (ordered by creation)';
COMMENT ON INDEX idx_calls_provider_created IS 'Used for provider filtering with time range';
