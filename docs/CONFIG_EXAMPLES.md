# AI Prompt Tracker - Configuration Examples

Quick reference for different deployment scenarios.

---

## 1. Default (No Persistence)

**Use case:** You only want logs, no database

```yaml
# No configuration needed!
# Tracking works, APIs return 503
```

**What happens:**
- ✅ AOP and interceptors track executions/calls
- ✅ Logs show tracking data
- ❌ Data NOT persisted to database
- ❌ REST APIs return 503 "Persistence Disabled"

**When to use:**
- You only need logs
- You have your own metrics system
- You don't want database overhead

---

## 2. Demo Mode (H2 In-Memory)

**Use case:** Local testing, quick evaluation

```yaml
ai-prompts:
  tracking:
    demo:
      enabled: true
```

**What happens:**
- ✅ H2 in-memory database auto-configured
- ✅ Flyway migrations run automatically
- ✅ Tables created
- ✅ Data persisted (until restart)
- ✅ REST APIs return data

**When to use:**
- First time trying the library
- Local development
- CI/CD test environments
- Demos and presentations

**Note:** Data is lost when app restarts (in-memory database)

---

## 3. Production (PostgreSQL, Manual Migrations)

**Use case:** Production with full control

```yaml
ai-prompts:
  tracking:
    persistence:
      mode: jdbc  # Enable persistence
    flyway:
      enabled: false  # YOU manage migrations (recommended)
    store-raw-data: false  # Don't store request/response bodies (privacy)

spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: none  # NEVER auto-create in production!
    properties:
      hibernate:
        jdbc:
          batch_size: 20
        order_inserts: true
```

**What happens:**
- ✅ Data persisted to PostgreSQL
- ✅ REST APIs work
- ✅ YOU control when migrations run
- ❌ Flyway does NOT run automatically

**Migration strategy:**

Copy migration files to your project:
```bash
cp backend/src/main/resources/db/migration/tracking/*.sql \
   your-app/src/main/resources/db/migration/
```

Then run your normal Flyway migration process.

---

## 4. Production (PostgreSQL, Auto-Migrate)

**Use case:** Production with library-managed migrations

```yaml
ai-prompts:
  tracking:
    persistence:
      mode: jdbc
    flyway:
      enabled: true  # Library runs migrations
    store-raw-data: false

spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: none
```

**What happens:**
- ✅ Data persisted to PostgreSQL
- ✅ Flyway migrations run on startup
- ✅ Uses separate history table: `flyway_tracking_schema_history`

**⚠️ Considerations:**
- Migrations run on application startup
- Review migration files before enabling
- Safe if you trust the library's migrations

---

## 5. Development Environment

**Use case:** Local dev with persistent data

```yaml
spring:
  profiles: dev

ai-prompts:
  tracking:
    persistence:
      mode: jdbc
    flyway:
      enabled: true  # Auto-migrate in dev
    store-raw-data: true  # Capture full request/response for debugging
    max-request-bytes: 32768
    max-response-bytes: 65536

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/dev_db
    username: dev
    password: dev
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: none
```

---

## 6. Staging Environment

**Use case:** Pre-production testing

```yaml
spring:
  profiles: staging

ai-prompts:
  tracking:
    persistence:
      mode: jdbc
    flyway:
      enabled: false  # Manage migrations via deployment pipeline
    store-raw-data: false
    max-request-bytes: 1024
    max-response-bytes: 2048

spring:
  datasource:
    url: jdbc:postgresql://${STAGING_DB_HOST}:5432/${STAGING_DB_NAME}
    username: ${STAGING_DB_USER}
    password: ${STAGING_DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: none
```

---

## 7. MySQL Production

**Use case:** Production with MySQL

```yaml
ai-prompts:
  tracking:
    persistence:
      mode: jdbc
    flyway:
      enabled: false

spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:3306/${DB_NAME}?useSSL=true&serverTimezone=UTC
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    database-platform: org.hibernate.dialect.MySQL8Dialect
    hibernate:
      ddl-auto: none
```

**Note:** MySQL migrations may need syntax adjustments (PostgreSQL arrays → JSON)

---

## 8. Multi-Environment with Profiles

**application.yml (base)**
```yaml
ai-prompts:
  tracking:
    persistence:
      mode: ${PERSISTENCE_MODE:none}  # Default: no persistence
```

**application-dev.yml**
```yaml
ai-prompts:
  tracking:
    demo:
      enabled: true  # H2 in dev
```

**application-staging.yml**
```yaml
ai-prompts:
  tracking:
    persistence:
      mode: jdbc
    flyway:
      enabled: true
```

**application-prod.yml**
```yaml
ai-prompts:
  tracking:
    persistence:
      mode: jdbc
    flyway:
      enabled: false  # Manual migrations in prod
    store-raw-data: false
```

---

## 9. Environment Variables

**Docker Compose / Kubernetes**

```yaml
environment:
  - AI_PROMPTS_TRACKING_PERSISTENCE_MODE=jdbc
  - AI_PROMPTS_TRACKING_FLYWAY_ENABLED=false
  - AI_PROMPTS_TRACKING_STORE_RAW_DATA=false
  - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/tracker
  - SPRING_DATASOURCE_USERNAME=tracker
  - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
```

---

## 10. Minimal Production (Security Hardened)

**Use case:** Production with maximum security

```yaml
ai-prompts:
  tracking:
    persistence:
      mode: jdbc
    flyway:
      enabled: false
    store-raw-data: false  # NEVER store request/response bodies
    max-request-bytes: 0
    max-response-bytes: 0

spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}?ssl=true&sslmode=require
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
  jpa:
    hibernate:
      ddl-auto: none
    properties:
      hibernate:
        jdbc:
          batch_size: 20
```

---

## Configuration Properties Reference

### Core Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `ai-prompts.tracking.persistence.mode` | enum | `NONE` | Persistence mode: `NONE`, `JDBC` |
| `ai-prompts.tracking.flyway.enabled` | boolean | `false` | Enable Flyway migrations |
| `ai-prompts.tracking.demo.enabled` | boolean | `false` | Enable demo mode (H2 + auto-migrate) |
| `ai-prompts.tracking.store-raw-data` | boolean | `false` | Store request/response bodies |
| `ai-prompts.tracking.max-request-bytes` | int | `1024` | Max request body size to store |
| `ai-prompts.tracking.max-response-bytes` | int | `2048` | Max response body size to store |

### Behavior Matrix

| Mode | Demo | Flyway | Database | Migrations | APIs |
|------|------|--------|----------|------------|------|
| `NONE` | false | false | Not required | N/A | 503 |
| `NONE` | true | - | H2 (auto) | Auto | 200 |
| `JDBC` | false | false | User provides | Manual | 200 |
| `JDBC` | false | true | User provides | Auto | 200 |

---

## Quick Decision Tree

**Start here:** Do you need to store tracking data in a database?

- **No** → Use default (no config)
- **Yes, just for testing** → Use demo mode (`demo.enabled=true`)
- **Yes, for production** → Continue...

**Do you want the library to run migrations?**

- **No, I'll manage them** → `persistence.mode=jdbc`, `flyway.enabled=false`
- **Yes, auto-migrate** → `persistence.mode=jdbc`, `flyway.enabled=true`

**Done!**

---

## Common Mistakes to Avoid

### ❌ DON'T: Auto-create schema in production
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop  # DANGEROUS!
```

### ✅ DO: Use explicit migrations
```yaml
ai-prompts:
  tracking:
    persistence:
      mode: jdbc
    flyway:
      enabled: true  # or false if you manage migrations
spring:
  jpa:
    hibernate:
      ddl-auto: none  # SAFE
```

---

### ❌ DON'T: Hardcode credentials
```yaml
spring:
  datasource:
    password: mysecretpassword  # INSECURE!
```

### ✅ DO: Use environment variables
```yaml
spring:
  datasource:
    password: ${DB_PASSWORD}  # SECURE
```

---

### ❌ DON'T: Store sensitive data in production
```yaml
ai-prompts:
  tracking:
    store-raw-data: true  # May contain API keys, PII!
```

### ✅ DO: Disable raw data storage
```yaml
ai-prompts:
  tracking:
    store-raw-data: false  # SAFE
```

---

## Need Help?

- **Quick Start:** See [QUICKSTART.md](QUICKSTART.md) for demo setup
- **Production:** See [PRODUCTION_GUIDE.md](PRODUCTION_GUIDE.md) for detailed deployment guide
- **Architecture:** See [ARCHITECTURE_ANALYSIS.md](ARCHITECTURE_ANALYSIS.md) for how it works
- **Implementation:** See [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md) for recent changes
