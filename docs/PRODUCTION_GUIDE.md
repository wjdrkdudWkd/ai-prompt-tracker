# AI Prompt Tracker - Production Deployment Guide

This guide explains how to safely deploy AI Prompt Tracker to production with proper database configuration and migration management.

---

## Overview

AI Prompt Tracker is designed to be **opt-in** and **production-safe**:

- **Default**: No database required (tracking runs in-memory only)
- **Demo**: H2 in-memory for local testing
- **Production**: JDBC mode with user-provided database

---

## Production Configuration

### Mode 1: Persistence Disabled (Default)

**Use case**: You only want in-memory tracking without persistence

```yaml
ai-prompts:
  tracking:
    persistence:
      mode: none  # default, can be omitted
```

**Behavior**:
- ✅ Tracking still works (AOP, interceptors, metrics)
- ✅ Execution context and call aggregation work
- ✅ Logs show tracking information
- ❌ Data is NOT persisted to database
- ❌ REST APIs return 503 "Persistence Disabled"

**When to use**:
- You only need logs
- You have your own metrics system
- You don't need historical data

---

### Mode 2: JDBC with Manual Migrations (Recommended)

**Use case**: Production deployment with full control over migrations

```yaml
ai-prompts:
  tracking:
    persistence:
      mode: jdbc  # Enable database persistence
    flyway:
      enabled: false  # default - YOU manage migrations

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/myapp
    username: myuser
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: none  # NEVER use create-drop in production!
```

**Behavior**:
- ✅ Data persisted to your database
- ✅ REST APIs work
- ✅ YOU control when migrations run
- ✅ No surprises in production

**Migration Strategy** (choose one):

#### Option A: Flyway Managed by Your App

If your app already uses Flyway:

1. Copy migration files to your project:
```bash
# Copy from library's resources
cp backend/src/main/resources/db/migration/tracking/*.sql \
   your-app/src/main/resources/db/migration/
```

2. Run your normal Flyway migration process

#### Option B: Library-Managed Flyway

Let the library run migrations:

```yaml
ai-prompts:
  tracking:
    persistence:
      mode: jdbc
    flyway:
      enabled: true  # Library runs migrations
```

**⚠️ Considerations**:
- Migrations run on application startup
- Uses separate Flyway history table: `flyway_tracking_schema_history`
- Safe if you trust the library's migrations
- Review migrations before enabling

#### Option C: Manual SQL Execution

Run migrations yourself via DB tools:

1. Extract SQL from `db/migration/tracking/`
2. Review and execute via your DB deployment pipeline
3. Keep `flyway.enabled=false`

---

### Mode 3: JDBC with Auto Migrations

**Use case**: You want everything automated (lower environments)

```yaml
ai-prompts:
  tracking:
    persistence:
      mode: jdbc
    flyway:
      enabled: true  # Auto-run migrations

spring:
  datasource:
    url: jdbc:postgresql://test-db:5432/test
    username: test
    password: test
```

**Behavior**:
- ✅ Database tables created automatically
- ✅ Migrations run on startup
- ⚠️ Less control in production

**When to use**:
- Development environments
- CI/CD test environments
- Staging (with caution)

---

## Database Setup

### PostgreSQL (Recommended)

**1. Create Database**:
```sql
CREATE DATABASE ai_prompt_tracker;
CREATE USER tracker_user WITH PASSWORD 'secure_password';
GRANT ALL PRIVILEGES ON DATABASE ai_prompt_tracker TO tracker_user;
```

**2. Configure Datasource**:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ai_prompt_tracker
    username: tracker_user
    password: ${TRACKER_DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: none
```

**3. Add Dependency** (if not already present):
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
```

### MySQL

**1. Create Database**:
```sql
CREATE DATABASE ai_prompt_tracker CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'tracker_user'@'%' IDENTIFIED BY 'secure_password';
GRANT ALL PRIVILEGES ON ai_prompt_tracker.* TO 'tracker_user'@'%';
FLUSH PRIVILEGES;
```

**2. Configure Datasource**:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_prompt_tracker?useSSL=false&serverTimezone=UTC
    username: tracker_user
    password: ${TRACKER_DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    database-platform: org.hibernate.dialect.MySQL8Dialect
    hibernate:
      ddl-auto: none
```

**3. Migration Adjustments**:

MySQL has different syntax for some PostgreSQL features. You may need to adjust:
- `TEXT[]` (PostgreSQL arrays) → `JSON` or `VARCHAR(1000)` in MySQL
- `DOUBLE PRECISION` → `DOUBLE`

---

## Migration Files Reference

### Schema Structure

```sql
-- V1__create_executions_table.sql
CREATE TABLE executions (
  id VARCHAR(36) PRIMARY KEY,
  function_name VARCHAR(100) NOT NULL,
  category VARCHAR(50),
  tags TEXT[],  -- PostgreSQL array
  environment VARCHAR(20) NOT NULL,
  started_at TIMESTAMP NOT NULL,
  finished_at TIMESTAMP,
  duration_ms BIGINT,
  status VARCHAR(20) NOT NULL,
  error_message TEXT,
  calls_count INT DEFAULT 0,
  total_cost DOUBLE PRECISION,
  total_tokens BIGINT
);

-- Indexes for query performance
CREATE INDEX idx_executions_function ON executions(function_name);
CREATE INDEX idx_executions_started_at ON executions(started_at);
CREATE INDEX idx_executions_environment ON executions(environment);
CREATE INDEX idx_executions_status ON executions(status);
```

```sql
-- V2__create_calls_table.sql
CREATE TABLE calls (
  id VARCHAR(36) PRIMARY KEY,
  execution_id VARCHAR(36) NOT NULL,
  provider VARCHAR(50) NOT NULL,
  model VARCHAR(100) NOT NULL,
  prompt_tokens INT,
  completion_tokens INT,
  total_tokens INT,
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

  FOREIGN KEY (execution_id) REFERENCES executions(id)
);

-- Indexes
CREATE INDEX idx_calls_execution ON calls(execution_id);
CREATE INDEX idx_calls_provider ON calls(provider);
CREATE INDEX idx_calls_created_at ON calls(created_at);
CREATE INDEX idx_calls_status ON calls(status);
```

---

## Environment-Specific Configuration

### Development

```yaml
spring:
  profiles: dev

ai-prompts:
  tracking:
    persistence:
      mode: jdbc
    flyway:
      enabled: true  # Auto-migrate in dev
    store-raw-data: true  # Capture request/response for debugging
    max-request-bytes: 32768
    max-response-bytes: 65536

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/dev_db
    username: dev
    password: dev
```

### Test/CI

```yaml
spring:
  profiles: test

ai-prompts:
  tracking:
    demo:
      enabled: true  # H2 in-memory for tests
```

### Production

```yaml
spring:
  profiles: prod

ai-prompts:
  tracking:
    persistence:
      mode: jdbc
    flyway:
      enabled: false  # Migrations via deployment pipeline
    store-raw-data: false  # Privacy/performance
    max-request-bytes: 1024
    max-response-bytes: 2048

spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: none  # CRITICAL: never auto-create in prod
```

---

## Security Considerations

### 1. Database Credentials

**Never hardcode credentials**:

```yaml
# ❌ BAD
spring:
  datasource:
    password: mysecretpassword

# ✅ GOOD
spring:
  datasource:
    password: ${DB_PASSWORD}  # From environment or secrets manager
```

### 2. Request/Response Data

**Disable in production** (may contain sensitive data):

```yaml
ai-prompts:
  tracking:
    store-raw-data: false  # Don't store request/response bodies
```

Or limit size:

```yaml
ai-prompts:
  tracking:
    store-raw-data: true
    max-request-bytes: 512   # Very small preview only
    max-response-bytes: 1024
```

### 3. Database Access

- Use read-only user for dashboard queries
- Restrict write access to application only
- Use SSL for database connections

---

## Performance Tuning

### 1. Database Connection Pool

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### 2. JPA Batch Inserts

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
```

### 3. Index Tuning

Monitor slow queries and add indexes:

```sql
-- If filtering by date + environment is common
CREATE INDEX idx_executions_started_env
  ON executions(started_at, environment);

-- If searching by function + status
CREATE INDEX idx_executions_func_status
  ON executions(function_name, status);
```

---

## Monitoring

### 1. Database Table Sizes

```sql
-- Check table sizes
SELECT
  schemaname,
  tablename,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE tablename IN ('executions', 'calls')
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

### 2. Row Counts

```sql
SELECT
  (SELECT COUNT(*) FROM executions) AS executions_count,
  (SELECT COUNT(*) FROM calls) AS calls_count;
```

### 3. Data Retention

Consider purging old data:

```sql
-- Delete executions older than 90 days (cascades to calls)
DELETE FROM executions
WHERE started_at < NOW() - INTERVAL '90 days';
```

---

## Troubleshooting

### Issue: "Persistence Disabled" Error

**Symptom**: APIs return 503 with `PERSISTENCE_DISABLED`

**Solution**: Enable JDBC mode

```yaml
ai-prompts:
  tracking:
    persistence:
      mode: jdbc  # Add this
```

### Issue: Tables Not Created

**Symptom**: `org.postgresql.util.PSQLException: ERROR: relation "executions" does not exist`

**Solution**: Enable Flyway OR create tables manually

```yaml
ai-prompts:
  tracking:
    flyway:
      enabled: true  # Enable migrations
```

### Issue: Flyway Conflict

**Symptom**: `FlywayException: Found more than one schema history table`

**Solution**: Library uses separate table `flyway_tracking_schema_history`

If conflict persists, disable library Flyway and manage manually:

```yaml
ai-prompts:
  tracking:
    flyway:
      enabled: false
```

### Issue: Slow Dashboard Queries

**Solution**: Add indexes, tune queries, or limit date ranges

```yaml
# Limit default query range in frontend
NEXT_PUBLIC_DEFAULT_DAYS=7
```

---

## Migration Path from Existing Setup

If you're already using AI Prompt Tracker with auto-DDL:

### Before (implicit persistence)

```yaml
# Old config - database always required
spring:
  datasource:
    url: jdbc:postgresql://...
  jpa:
    hibernate:
      ddl-auto: create-drop  # ⚠️ Risky!
```

### After (explicit persistence)

```yaml
# New config - explicit and safe
ai-prompts:
  tracking:
    persistence:
      mode: jdbc  # Explicit opt-in
    flyway:
      enabled: true  # Managed migrations

spring:
  datasource:
    url: jdbc:postgresql://...
  jpa:
    hibernate:
      ddl-auto: none  # ✅ Safe
```

---

## Summary

### Recommended Production Setup

1. **Use JDBC mode** with your existing database
2. **Disable auto Flyway** (manage migrations yourself)
3. **Disable raw data** or use very small limits
4. **Monitor table growth** and plan retention policy
5. **Use environment variables** for credentials
6. **Review migrations** before applying

### Configuration Checklist

- [ ] `persistence.mode=jdbc`
- [ ] `flyway.enabled=false` (manage manually)
- [ ] `store-raw-data=false` or small limits
- [ ] Datasource configured with environment variables
- [ ] `hibernate.ddl-auto=none`
- [ ] Migrations reviewed and applied
- [ ] Indexes created for performance
- [ ] Data retention policy defined

---

**Need Help?**

- [Quick Start Guide](QUICKSTART.md) - Demo mode setup
- [Architecture Analysis](ARCHITECTURE_ANALYSIS.md) - How it works internally
- [Integration Guide](INTEGRATIONS_GUIDE.md) - Multi-client support
