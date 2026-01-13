# JPA Scanning Troubleshooting Guide

This guide helps diagnose and fix JPA entity and repository scanning issues when using the AI Prompt Tracker Spring Boot Starter.

## 📋 Table of Contents

- [Quick Diagnostic](#quick-diagnostic)
- [Common Issues](#common-issues)
- [Detailed Diagnostic Commands](#detailed-diagnostic-commands)
- [Expected Log Output](#expected-log-output)
- [How It Works](#how-it-works)
- [FAQ](#faq)

---

## 🔍 Quick Diagnostic

### Step 1: Enable Diagnostic Mode

Add to your `application.yml`:

```yaml
ai-prompts:
  debug:
    scan: true
```

### Step 2: Run with Debug Logging

```bash
./gradlew clean bootRun --args='--debug --logging.level.com.galoong=DEBUG'
```

### Step 3: Check Diagnostic Output

Look for this section in the logs:

```
╔═══════════════════════════════════════════════════════════════════════════════╗
║                    AI PROMPT TRACKER - JPA SCAN DIAGNOSTICS                   ║
╚═══════════════════════════════════════════════════════════════════════════════╝

[1] AutoConfigurationPackages (used for repository scanning):
    ✅ Total packages: 2
    ✅ [CONSUMER] com.example.yourapp
    ✅ [STARTER]  com.galoong.aiprompttracker

[2] EntityScanPackages (used for entity scanning):
    ✅ Total packages: 2
    ✅ [CONSUMER] com.example.yourapp
    ✅ [STARTER]  com.galoong.aiprompttracker

[3] JPA Entities discovered by Hibernate:
    ✅ Total entities: 4

    ✅ Consumer entities: 2
       - com.example.yourapp.domain.User
       - com.example.yourapp.domain.Product

    ✅ Starter entities: 2
       - com.galoong.aiprompttracker.domain.entity.Execution
       - com.galoong.aiprompttracker.domain.entity.Call
```

✅ **If you see this output with both consumer and starter entities, everything is working correctly!**

---

## ❌ Common Issues

### Issue 1: Consumer Entities Not Discovered

**Symptoms:**
```
⚠️  NO consumer entities found!
```

Or error:
```
Not a managed type: com.example.yourapp.domain.User
```

**Diagnosis:**

1. Check AutoConfigurationPackages:
   ```bash
   ./gradlew bootRun --args='--debug' 2>&1 | grep "AutoConfigurationPackages"
   ```

2. Verify your package structure:
   - Your `@SpringBootApplication` class should be in a parent package of your entities
   - Example:
     - ✅ Main class: `com.example.yourapp.Application`
     - ✅ Entity: `com.example.yourapp.domain.User` (discovered)
     - ❌ Entity: `com.other.package.User` (NOT discovered)

**Solutions:**

**Option A:** Move entities under your application's base package

**Option B:** Add explicit `@EntityScan` to your main application class:
```java
@SpringBootApplication
@EntityScan(basePackages = {
    "com.example.yourapp",      // Your entities
    "com.galoong.aiprompttracker" // Starter entities (if needed)
})
public class YourApplication {
    // ...
}
```

---

### Issue 2: Starter Entities Not Discovered

**Symptoms:**
```
⚠️  NO starter entities found!
```

Or error:
```
Parameter 0 of method jpaExecutionStore in ApiAutoConfiguration required a bean of type 'ExecutionRepository' that could not be found.
```

**Diagnosis:**

1. Check package registration logs:
   ```bash
   ./gradlew bootRun --args='--debug' 2>&1 | grep "AI Prompt Tracker: JPA scan BFPP"
   ```

   Expected output:
   ```
   AI Prompt Tracker: JPA scan BFPP started - appending packages to registries
   AI Prompt Tracker: ✅ Appended 'com.galoong.aiprompttracker' to AutoConfigurationPackages (for repository scanning)
   AI Prompt Tracker: ✅ Appended 'com.galoong.aiprompttracker' to EntityScanPackages (for entity scanning)
   AI Prompt Tracker: JPA scan BFPP completed successfully
   ```

2. If you see warnings instead:
   ```
   ⚠️  Starter package 'com.galoong.aiprompttracker' NOT found!
   ```

**Solutions:**

**Option A:** Verify starter version
- Ensure you're using a recent version with the dual-registry BFPP
- Check `build.gradle.kts`:
  ```kotlin
  implementation("com.galoong:ai-prompt-tracker-starter:X.Y.Z")
  ```

**Option B:** Check auto-configuration loading
```bash
./gradlew bootRun --args='--debug' 2>&1 | grep "AiPromptTrackerJpaScanAutoConfiguration"
```

You should see:
```
AiPromptTrackerJpaScanAutoConfiguration matched:
   - @ConditionalOnWebApplication matched (found GenericWebApplicationContext)
```

**Option C:** Manual configuration (fallback)

If automatic registration fails, add to your main application class:
```java
@SpringBootApplication
@EntityScan(basePackages = {
    "com.example.yourapp",
    "com.galoong.aiprompttracker.domain.entity"
})
@EnableJpaRepositories(basePackages = {
    "com.example.yourapp",
    "com.galoong.aiprompttracker.domain.repository"
})
public class YourApplication {
    // ...
}
```

---

### Issue 3: Both Packages Registered but Entities Still Missing

**Symptoms:**
- AutoConfigurationPackages shows both packages ✅
- EntityScanPackages shows both packages ✅
- But entities are still not discovered ❌

**Diagnosis:**

Check entity annotations:
```bash
./gradlew bootRun --args='--debug' 2>&1 | grep "HHH000204"
```

Expected:
```
HHH000204: Processing PersistenceUnitInfo [name: default]
    root-url [...]
```

Then check for entity class processing:
```bash
./gradlew bootRun --args='--debug' 2>&1 | grep "HHH000183"
```

**Solutions:**

1. **Verify @Entity annotations:**
   - Ensure all entity classes have `@Entity` annotation
   - Use `jakarta.persistence.Entity` (not `javax.persistence.Entity` in Spring Boot 3)

2. **Check entity class modifiers:**
   - Entity classes must be non-final
   - Entity classes must have a no-arg constructor (can be protected)

3. **Verify package structure:**
   - Entity classes must be in compiled classpath
   - Check `build/classes/java/main` contains your entities

---

## 🔬 Detailed Diagnostic Commands

### Full Debug Run

```bash
./gradlew clean bootRun \
  --args='--debug \
         --logging.level.org.springframework.boot.autoconfigure=DEBUG \
         --logging.level.org.springframework.boot.autoconfigure.domain=TRACE \
         --logging.level.org.hibernate=INFO \
         --logging.level.com.galoong=DEBUG \
         --ai-prompts.debug.scan=true'
```

### Check AutoConfigurationPackages

```bash
./gradlew bootRun --args='--debug' 2>&1 | grep -A10 "AutoConfigurationPackages"
```

### Check EntityScanPackages

```bash
./gradlew bootRun --args='--debug' 2>&1 | grep -A10 "EntityScanPackages"
```

### Check Discovered Entities

```bash
./gradlew bootRun --args='--debug' 2>&1 | grep -A20 "JPA Entities discovered"
```

### Check Repository Bean Creation

```bash
./gradlew bootRun --args='--debug' 2>&1 | grep "Creating shared instance of singleton bean" | grep -i repository
```

Expected output:
```
Creating shared instance of singleton bean 'executionRepository'
Creating shared instance of singleton bean 'callRepository'
Creating shared instance of singleton bean 'yourCustomRepository'
```

### Check Hibernate Entity Scanning

```bash
./gradlew bootRun --args='--debug' 2>&1 | grep "HHH.*entity"
```

---

## 📊 Expected Log Output

### Successful Startup Sequence

1. **BFPP Package Registration:**
   ```
   AI Prompt Tracker: JPA scan BFPP started - appending packages to registries
   AI Prompt Tracker: Existing AutoConfigurationPackages: [com.example.yourapp]
   AI Prompt Tracker: ✅ Appended 'com.galoong.aiprompttracker' to AutoConfigurationPackages (for repository scanning)
   AI Prompt Tracker: Updated AutoConfigurationPackages: [com.example.yourapp, com.galoong.aiprompttracker]
   AI Prompt Tracker: ℹ️  EntityScanPackages not explicitly registered (this is normal)
   AI Prompt Tracker: ✅ Appended 'com.galoong.aiprompttracker' to EntityScanPackages (for entity scanning)
   AI Prompt Tracker: JPA scan BFPP completed successfully
   ```

2. **JPA Auto-Configuration:**
   ```
   AI Prompt Tracker: JPA auto-configuration loaded. Entities and repositories will be auto-discovered via AutoConfigurationPackages extension.
   ```

3. **Repository Registration:**
   ```
   Creating shared instance of singleton bean 'executionRepository'
   Creating shared instance of singleton bean 'callRepository'
   ```

4. **Entity Discovery:**
   ```
   HHH000204: Processing PersistenceUnitInfo [name: default]
   ```

5. **Service Registration:**
   ```
   Persistence mode: JDBC - enabling JPA execution store
   Persistence mode: JDBC - enabling JPA call store
   Registering DashboardService
   Registering ExecutionService
   ```

6. **Diagnostic Report (if enabled):**
   ```
   ╔═══════════════════════════════════════════════════════════════════════════════╗
   ║                    AI PROMPT TRACKER - JPA SCAN DIAGNOSTICS                   ║
   ╚═══════════════════════════════════════════════════════════════════════════════╝
   ```

---

## 🏗️ How It Works

### Dual-Registry Strategy

The starter uses a **dual-registry approach** to ensure both entities and repositories are discovered:

1. **AutoConfigurationPackages** (for repository scanning)
   - Used by `JpaRepositoriesAutoConfiguration`
   - Scans for `@Repository` interfaces extending `JpaRepository`

2. **EntityScanPackages** (for entity scanning)
   - Used by `HibernateJpaAutoConfiguration`
   - Scans for `@Entity` classes

### Package Registration Flow

```
Application Startup
     │
     ├─> Spring Boot registers consumer's base package in AutoConfigurationPackages
     │
     ├─> AiPromptTrackerJpaScanAutoConfiguration loads
     │
     ├─> Static @Bean BFPP executes EARLY
     │   │
     │   ├─> Check: AutoConfigurationPackages.has(beanFactory)?
     │   │   ├─> YES: Get existing packages
     │   │   │   └─> Append 'com.galoong.aiprompttracker' if not present
     │   │   └─> NO: Skip (graceful degradation)
     │   │
     │   └─> Try: EntityScanPackages.get(beanFactory)
     │       ├─> EXISTS: Append 'com.galoong.aiprompttracker' if not present
     │       └─> NOT EXISTS: Create with starter package only
     │
     ├─> JpaRepositoriesAutoConfiguration scans AutoConfigurationPackages
     │   └─> Discovers: Consumer repositories + Starter repositories
     │
     ├─> HibernateJpaAutoConfiguration scans EntityScanPackages (or AutoConfigurationPackages as fallback)
     │   └─> Discovers: Consumer entities + Starter entities
     │
     └─> Application Ready ✅
```

### Why Static @Bean?

The BFPP is registered as a **static @Bean** method:

```java
@Bean
public static BeanFactoryPostProcessor aiPromptTrackerPackageRegistrar() {
    return new PackageAppendingBeanFactoryPostProcessor();
}
```

**Benefits:**
- Executes **before** the containing `@Configuration` class is instantiated
- Runs **very early** in Spring lifecycle
- Guaranteed to execute **before** `EntityManagerFactory` creation
- **No dependency** on component scanning

---

## ❓ FAQ

### Q: Do I need @EnableJpaRepositories?

**A: No!** The starter automatically registers packages. Your main class should just have:

```java
@SpringBootApplication
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

### Q: Do I need @EntityScan?

**A: Usually no.** The starter handles package registration automatically.

**Exception:** If your entities are in a package **outside** your `@SpringBootApplication` base package, you need to add `@EntityScan`.

### Q: What if EntityScanPackages is empty?

**A: This is normal!** Hibernate falls back to `AutoConfigurationPackages` for entity scanning when `EntityScanPackages` is not explicitly set. As long as `AutoConfigurationPackages` includes both packages, entities will be discovered.

### Q: Can I use the starter without JPA?

**A: Yes!** Set persistence mode to `memory` or `noop`:

```yaml
ai-prompts:
  tracking:
    persistence:
      mode: memory
```

JPA auto-configuration will not load, and no entity/repository scanning occurs.

### Q: The starter works in my demo app but fails in my real app. Why?

**Common causes:**

1. **Different package structure:** Your real app's entities might be in a different package hierarchy
2. **Custom JPA configuration:** Your app might have custom `@EnableJpaRepositories` or `@EntityScan` that overrides Boot's defaults
3. **Profile-specific configuration:** Different `application-{profile}.yml` settings
4. **Multi-module project:** Entities in a separate module might not be scanned

**Solution:** Enable diagnostic mode (`ai-prompts.debug.scan=true`) and compare the package lists between demo and real app.

### Q: How do I verify repositories are actually discovered?

Run:
```bash
./gradlew bootRun --args='--debug' 2>&1 | grep "Creating shared instance of singleton bean" | grep -i repository
```

You should see:
```
Creating shared instance of singleton bean 'executionRepository'
Creating shared instance of singleton bean 'callRepository'
Creating shared instance of singleton bean 'yourCustomRepository'
```

### Q: What if I see "selected by rule" warnings?

This is related to Gradle module metadata variant resolution, not JPA scanning. See `STARTER_PUBLISHING.md` for details on how to suppress these warnings via publishing configuration.

---

## 🆘 Still Having Issues?

If you've followed this guide and are still experiencing issues:

1. **Collect diagnostic output:**
   ```bash
   ./gradlew clean bootRun --args='--debug --ai-prompts.debug.scan=true' > diagnostic.log 2>&1
   ```

2. **Check for these log lines:**
   - "AI Prompt Tracker: JPA scan BFPP started"
   - "AutoConfigurationPackages" with package list
   - "EntityScanPackages" with package list
   - "JPA Entities discovered" with entity list

3. **Create an issue at:**
   https://github.com/wjdrkdudWkd/ai-prompt-tracker/issues

   Include:
   - Your Spring Boot version
   - Your `build.gradle.kts` dependencies
   - Main application class structure
   - Entity package structure
   - Relevant sections from `diagnostic.log`

---

## 📚 Related Documentation

- [Spring Boot Auto-Configuration](https://docs.spring.io/spring-boot/reference/using/auto-configuration.html)
- [Spring Data JPA Repositories](https://docs.spring.io/spring-data/jpa/reference/repositories.html)
- [Hibernate Entity Scanning](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html)
- [Starter Publishing Guide](./STARTER_PUBLISHING.md)
