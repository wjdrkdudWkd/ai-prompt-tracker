# Schema Management Guide

AI Prompt Tracker supports two distinct schema management modes for the tracking database. This guide helps you choose the right mode and configure it correctly.

## Overview

The tracker's database schema (executions and calls tables) can be managed in two ways:

1. **Flyway-managed schema mode** (Recommended for production)
2. **Hibernate-managed schema mode** (Suitable for development/testing)

⚠️ **Important:** Enabling both Flyway and Hibernate schema management simultaneously can cause conflicts like "Index already exists" errors.

---

## Mode 1: Flyway-Managed Schema (Recommended)

### When to Use
- Production environments
- Shared/staging environments
- When you need version-controlled schema migrations
- When multiple developers work on the same database

### Configuration

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: none  # or 'validate' - CRITICAL: do not use 'update/create'
  flyway:
    enabled: true

ai-prompts:
  tracking:
    persistence:
      mode: jdbc
    flyway:
      enabled: true
```

### How It Works
- Flyway migration scripts (`V1__create_schema_h2.sql`, etc.) create and manage the schema
- Hibernate validates the schema but does NOT modify it (`ddl-auto: none`)
- Schema changes are version-controlled and reproducible
- Indexes are defined in Flyway SQL files

### Pros
✅ Version-controlled schema changes
✅ Predictable and reproducible migrations
✅ Safe for production
✅ Clear audit trail of schema changes

### Cons
❌ Requires manual migration scripts for schema changes
❌ Slightly more setup overhead

---

## Mode 2: Hibernate-Managed Schema

### When to Use
- Local development
- Rapid prototyping
- Simple test environments
- When you don't need migration history

### Configuration

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: update  # or 'create-drop' for tests
  flyway:
    enabled: false

ai-prompts:
  tracking:
    persistence:
      mode: jdbc
    flyway:
      enabled: false
```

### How It Works
- Hibernate automatically creates/updates tables based on JPA entity annotations
- Flyway is completely disabled
- Schema changes happen automatically when entities change
- ⚠️ **Note:** Indexes are NOT automatically created in this mode (Flyway SQL defines them)

### Pros
✅ Zero-config for development
✅ Schema auto-updates when entities change
✅ Fast iteration

### Cons
❌ No migration history
❌ Indexes not created (performance impact)
❌ Not suitable for production
❌ Can cause data loss with `create-drop`

---

## Demo Mode (Quickstart)

For the fastest zero-config experience, use demo mode:

```yaml
# application.yml
ai-prompts:
  tracking:
    demo:
      enabled: true
```

### What Demo Mode Does
- Automatically creates H2 in-memory database
- Enables Flyway migrations automatically
- Sets appropriate Hibernate settings
- **Requires:** `com.h2database:h2` dependency in your project

```kotlin
// build.gradle.kts
dependencies {
    runtimeOnly("com.h2database:h2")  // Required for demo mode
}
```

⚠️ **Warning:** Demo mode uses in-memory database. All data is lost on restart. NOT for production.

---

## Conflict Detection

AI Prompt Tracker automatically detects schema management conflicts and logs a warning.

### Conflict Example

```yaml
# ❌ CONFLICT: Both Flyway and Hibernate try to manage schema
spring:
  jpa:
    hibernate:
      ddl-auto: update  # ← Problem: Hibernate will try to create indexes
  flyway:
    enabled: true       # ← Problem: Flyway already created indexes

ai-prompts:
  tracking:
    flyway:
      enabled: true
```

**Error you'll see:**
```
org.h2.jdbc.JdbcSQLSyntaxErrorException: Index "IDX_CALLS_EXECUTION" already exists
```

**Warning log:**
```
╔═══════════════════════════════════════════════════════════════════════════════╗
║                  ⚠️  SCHEMA MANAGEMENT CONFLICT DETECTED  ⚠️                   ║
╚═══════════════════════════════════════════════════════════════════════════════╝

Current configuration:
  - Flyway enabled: true
  - spring.jpa.hibernate.ddl-auto: update

⚠️  ISSUE:
  Both Flyway and Hibernate are configured to manage the database schema.
  This can cause conflicts such as:
    - "Index 'XXX' already exists" errors
    - "Table already exists" errors
    - Unpredictable schema state

📋 RECOMMENDED SOLUTIONS:
[Solutions provided in log...]
```

### How to Fix

**Option 1: Switch to Flyway-only**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: none  # ← Change this
```

**Option 2: Switch to Hibernate-only**
```yaml
spring:
  flyway:
    enabled: false
ai-prompts:
  tracking:
    flyway:
      enabled: false
```

---

## Configuration Summary

| Mode | `flyway.enabled` | `ddl-auto` | Use Case |
|------|------------------|------------|----------|
| **Flyway-managed** | `true` | `none` or `validate` | Production, Staging |
| **Hibernate-managed** | `false` | `update` or `create-drop` | Development, Testing |
| **Demo mode** | Auto (`true`) | Auto (`create-drop`) | Quick local demo |

---

## Migration Scripts Location

Flyway migrations are included in the starter JAR:

```
tracker-starter/src/main/resources/db/migration/
├── V1__create_schema_h2.sql          # H2 compatible schema
└── V1__create_schema_postgresql.sql  # PostgreSQL schema (if exists)
```

These scripts create:
- `executions` table with 4 indexes
- `calls` table with 4 indexes
- Foreign key relationship between tables

---

## Troubleshooting

### Problem: "Index already exists" error

**Cause:** Both Flyway and Hibernate are trying to create indexes.

**Solution:** Set `spring.jpa.hibernate.ddl-auto: none` when using Flyway.

### Problem: Starter tables not created

**Cause:** Persistence mode is set to `NONE` (default).

**Solution:** Set `ai-prompts.tracking.persistence.mode: jdbc` or enable demo mode.

### Problem: H2 ClassNotFoundException in demo mode

**Cause:** H2 dependency is `compileOnly` in starter, not included.

**Solution:** Add H2 to your project:
```kotlin
dependencies {
    runtimeOnly("com.h2database:h2")
}
```

### Problem: Missing indexes in Hibernate-only mode

**Cause:** Entity annotations don't declare indexes (they're in Flyway SQL).

**Solution:** Either:
1. Switch to Flyway mode (recommended), OR
2. Manually create indexes if needed for performance

---

## Best Practices

1. **Production:** Always use Flyway-managed mode with `ddl-auto: none`
2. **Development:** Use demo mode or Hibernate-managed mode for convenience
3. **CI/CD:** Use Flyway mode for consistent test environments
4. **Conflict Avoidance:** Never mix `ddl-auto: update/create` with `flyway.enabled: true`
5. **Consumer Settings:** Starter will NOT override your `ddl-auto` - you must set it correctly

---

## FAQ

**Q: Can I use Flyway for my tables and Hibernate for starter tables?**
A: No. Hibernate's `ddl-auto` applies to ALL entities, including starter entities. This causes conflicts.

**Q: What happens if I ignore the conflict warning?**
A: Your application may fail to start with "Index already exists" errors, or schema state may become unpredictable.

**Q: Does the starter automatically fix my ddl-auto setting?**
A: No. The starter only detects and warns. You must update your configuration manually.

**Q: I'm using PostgreSQL. Do I need different configuration?**
A: The configuration is the same. Flyway will automatically use PostgreSQL-specific migration scripts if available.

---

## Related Documentation

- [Configuration Properties](./CONFIGURATION.md)
- [Quick Start Guide](../README.md)
- [JPA Scanning Guide](./JPA_SCANNING_SOLUTION_SUMMARY.md)
