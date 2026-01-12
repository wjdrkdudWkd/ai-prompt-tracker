# Schema Management Verification Guide

This document provides step-by-step verification scenarios for the schema management modes.

## Prerequisites

1. Build the starter:
   ```bash
   ./gradlew :tracker-starter:build
   ```

2. Have a test consumer project or use the backend module

## Verification Scenarios

### Scenario 1: Flyway ON + ddl-auto=update (Should warn but work)

**Goal:** Verify that index duplication is eliminated and a warning is logged.

#### Configuration

Create/Update `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: update  # ← Potential conflict
    show-sql: true
  flyway:
    enabled: true

ai-prompts:
  tracking:
    persistence:
      mode: jdbc
    flyway:
      enabled: true
```

#### Expected Behavior

1. **Application starts successfully** (no "Index already exists" error)
2. **Warning log appears once:**
   ```
   ╔═══════════════════════════════════════════════════════════════════════════════╗
   ║                  ⚠️  SCHEMA MANAGEMENT CONFLICT DETECTED  ⚠️                   ║
   ╚═══════════════════════════════════════════════════════════════════════════════╝

   Current configuration:
     - Flyway enabled: true
     - spring.jpa.hibernate.ddl-auto: update

   ⚠️  ISSUE:
     Both Flyway and Hibernate are configured to manage the database schema.

   📋 RECOMMENDED SOLUTIONS:
   [Option 1 and 2 displayed]
   ```

3. **Database schema created correctly:**
   ```sql
   -- Tables exist
   SELECT * FROM executions;
   SELECT * FROM calls;

   -- Indexes exist (created by Flyway only, no duplicates)
   SELECT * FROM INFORMATION_SCHEMA.INDEXES WHERE TABLE_NAME IN ('executions', 'calls');
   ```

#### Verification Commands

```bash
# Start the application
./gradlew :backend:bootRun --args='--spring.profiles.active=test'

# Check logs for warning (should appear once)
grep -A 20 "SCHEMA MANAGEMENT CONFLICT" backend/logs/app.log

# Verify no "Index already exists" error
grep "Index.*already exists" backend/logs/app.log
# Should return: (no output)
```

---

### Scenario 2: Flyway ON + ddl-auto=none (No warning, clean)

**Goal:** Verify the recommended Flyway-managed configuration works perfectly.

#### Configuration

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: none  # ← Correct for Flyway mode
    show-sql: true
  flyway:
    enabled: true

ai-prompts:
  tracking:
    persistence:
      mode: jdbc
    flyway:
      enabled: true
```

#### Expected Behavior

1. **Application starts successfully**
2. **No conflict warning** (ddl-auto is safe)
3. **Schema management mode log:**
   ```
   ╔═══════════════════════════════════════════════════════════════════════════════╗
   ║           AI PROMPT TRACKER - Schema Management Mode: Flyway-managed          ║
   ╚═══════════════════════════════════════════════════════════════════════════════╝
     Current configuration:
       - spring.jpa.hibernate.ddl-auto: none
       - Schema managed by: Flyway
   ```

4. **Flyway migrations executed:**
   ```
   INFO  FlywayExecutor : Successfully applied 1 migration to schema "PUBLIC"
   ```

5. **Database schema created by Flyway:**
   - All tables and indexes present
   - Hibernate validates schema but doesn't modify it

#### Verification Commands

```bash
# Start the application
./gradlew :backend:bootRun --args='--spring.profiles.active=test'

# Verify Flyway-managed mode log
grep "Schema Management Mode: Flyway-managed" backend/logs/app.log

# Verify NO conflict warning
grep "SCHEMA MANAGEMENT CONFLICT" backend/logs/app.log
# Should return: (no output)

# Verify Flyway executed
grep "Successfully applied.*migration" backend/logs/app.log
```

---

### Scenario 3: Flyway OFF + ddl-auto=update (Hibernate-managed mode)

**Goal:** Verify Hibernate-only schema management works without Flyway.

#### Configuration

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: update  # ← Hibernate manages schema
    show-sql: true
  flyway:
    enabled: false  # ← Flyway disabled

ai-prompts:
  tracking:
    persistence:
      mode: jdbc
    flyway:
      enabled: false
```

#### Expected Behavior

1. **Application starts successfully**
2. **No conflict warning** (Flyway is disabled)
3. **Schema management mode log:**
   ```
   ╔═══════════════════════════════════════════════════════════════════════════════╗
   ║         AI PROMPT TRACKER - Schema Management Mode: Hibernate-managed         ║
   ╚═══════════════════════════════════════════════════════════════════════════════╝
     Current configuration:
       - spring.jpa.hibernate.ddl-auto: update
       - Schema managed by: Hibernate
   ```

4. **Hibernate creates tables:**
   ```
   Hibernate: create table executions (...)
   Hibernate: create table calls (...)
   ```

5. **⚠️ Note: Indexes NOT created** (they're only defined in Flyway SQL)
   - Tables exist and functional
   - No indexes for performance optimization
   - Acceptable for development, not recommended for production

#### Verification Commands

```bash
# Start the application
./gradlew :backend:bootRun --args='--spring.profiles.active=test'

# Verify Hibernate-managed mode log
grep "Schema Management Mode: Hibernate-managed" backend/logs/app.log

# Verify NO conflict warning
grep "SCHEMA MANAGEMENT CONFLICT" backend/logs/app.log
# Should return: (no output)

# Verify Hibernate created tables
grep "Hibernate:.*create table" backend/logs/app.log

# Verify Flyway did NOT run
grep "Flyway.*migration" backend/logs/app.log
# Should return: (no output)

# Check H2 console (if enabled) for missing indexes
# Connect to: http://localhost:8080/h2-console
# Run: SELECT * FROM INFORMATION_SCHEMA.INDEXES WHERE TABLE_NAME = 'calls';
# Result: No indexes present (expected in Hibernate-only mode)
```

---

## Demo Mode Verification

**Goal:** Verify quickstart demo mode works out of the box.

### Configuration

```yaml
ai-prompts:
  tracking:
    demo:
      enabled: true
```

### Requirements

```kotlin
// build.gradle.kts
dependencies {
    runtimeOnly("com.h2database:h2")  // Required!
}
```

### Expected Behavior

1. **Application starts successfully**
2. **H2 in-memory database auto-configured**
3. **Flyway runs automatically**
4. **H2 console available at:** `http://localhost:8080/h2-console`
   - JDBC URL: `jdbc:h2:mem:ai-prompt-tracker`
   - Username: `sa`
   - Password: (empty)

### Verification Commands

```bash
# Start the application
./gradlew :backend:bootRun

# Verify demo mode activated
grep "Demo mode enabled" backend/logs/app.log

# Test H2 console access
curl -I http://localhost:8080/h2-console
# Should return: HTTP/1.1 200 OK

# Query demo database
# Use H2 console or:
curl http://localhost:8080/aiprompt-tracker/api/executions
# Should return: [] (empty but accessible)
```

---

## Troubleshooting Verification Issues

### Issue: "Index already exists" error still occurs

**Cause:** Entity files still have `@Index` annotations.

**Solution:** Verify the changes were applied:
```bash
grep -A 5 "@Table" tracker-starter/src/main/java/com/galoong/aiprompttracker/domain/entity/CallRecord.java

# Should show:
# @Table(name = "calls")
# NOT:
# @Table(name = "calls", indexes = {...})
```

### Issue: Warning not appearing in Scenario 1

**Cause:** Configuration not loaded correctly or schema management auto-config disabled.

**Solution:** Verify configuration:
```bash
# Check if JDBC mode is enabled
grep "persistence.mode" backend/src/main/resources/application.yml

# Check if auto-configuration imported
grep "SchemaManagementAutoConfiguration" tracker-starter/src/main/java/com/galoong/aiprompttracker/autoconfigure/AiPromptTrackerAutoConfiguration.java
```

### Issue: Application fails to start with ClassNotFoundException: org.h2.Driver

**Cause:** H2 dependency missing (demo mode).

**Solution:** Add H2 dependency:
```kotlin
dependencies {
    runtimeOnly("com.h2database:h2")
}
```

---

## Summary Checklist

After running all scenarios, verify:

- [ ] Scenario 1: Warning logged, app starts, no index duplication error
- [ ] Scenario 2: No warning, Flyway-managed mode log appears
- [ ] Scenario 3: No warning, Hibernate-managed mode log appears, tables created
- [ ] Demo mode: Works with H2 dependency
- [ ] Documentation: SCHEMA_MANAGEMENT.md accessible and clear
- [ ] Code: No `@Index` annotations in entity `@Table` definitions
- [ ] Build: `./gradlew :tracker-starter:compileJava` succeeds

---

## Performance Notes

### Scenario 1 vs Scenario 2 (Flyway mode)

Both scenarios use Flyway, so **indexes ARE created**. Performance is identical.

### Scenario 3 (Hibernate-only mode)

**No indexes created** → Queries may be slower for large datasets.

Example query performance difference:
```sql
-- Query: Find calls by execution_id
SELECT * FROM calls WHERE execution_id = '123';

-- With index (Flyway mode): ~1ms (index seek)
-- Without index (Hibernate mode): ~100ms for 10k rows (full table scan)
```

**Recommendation:** Use Hibernate-only mode only for development/testing with small datasets.

---

## Related Documentation

- [Schema Management Guide](./SCHEMA_MANAGEMENT.md)
- [Configuration Properties](./CONFIGURATION.md)
