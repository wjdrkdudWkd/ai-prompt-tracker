-- Additional performance indexes

-- Composite index for common queries
CREATE INDEX idx_provider_function_date ON ai_call_records(provider_name, function_name, created_at DESC);

-- Index for cost analysis
CREATE INDEX idx_cost_created ON ai_call_records(estimated_cost DESC, created_at DESC)
WHERE estimated_cost IS NOT NULL;

-- Index for error tracking
CREATE INDEX idx_error_tracking ON ai_call_records(success, created_at DESC)
WHERE success = false;

-- Index for response time analysis
CREATE INDEX idx_response_time ON ai_call_records(response_time_ms DESC)
WHERE response_time_ms IS NOT NULL;

-- Index for cache hit analysis
CREATE INDEX idx_cache_analysis ON ai_call_records(cache_hit, created_at DESC);

-- Partial index for recent records (last 30 days optimization)
-- ❌ (문제) WHERE에 now() 같은 함수가 들어간 부분 인덱스
-- CREATE INDEX idx_ai_call_records_recent ON ai_call_records(created_at)
-- WHERE created_at >= now() - interval '30 days';

-- ✅ (대체) 그냥 created_at 인덱스
CREATE INDEX IF NOT EXISTS idx_ai_call_records_created_at
    ON ai_call_records (created_at);

-- Index for user-based queries
CREATE INDEX idx_user_session ON ai_call_records(user_id, session_id, created_at DESC)
WHERE user_id IS NOT NULL;

-- Stats table optimization indexes
CREATE INDEX idx_stats_cost_trend ON daily_stats(stats_date DESC, total_cost DESC);
CREATE INDEX idx_stats_calls_trend ON daily_stats(stats_date DESC, total_calls DESC);

-- Index for category analysis
CREATE INDEX idx_category_stats ON daily_stats(category, stats_date DESC)
WHERE category IS NOT NULL;
