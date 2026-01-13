# JPA Scanning Verification Guide

This document provides verification steps to confirm that JPA entity and repository scanning works correctly with the AI Prompt Tracker starter.

## 🎯 Expected Behavior

### Without Explicit @EntityScan (Default Case)

When a consumer application has NO explicit `@EntityScan`:

1. ✅ Consumer repositories are discovered (e.g., `UserRepository`, `ProductRepository`)
2. ✅ Starter repositories are discovered (`ExecutionRepository`, `CallRepository`)
3. ✅ Consumer entities are managed by JPA
4. ✅ Starter entities are managed by JPA
5. ✅ No consumer-side configuration required

### With Explicit @EntityScan

When a consumer application uses `@EntityScan`:

1. ⚠️ A warning appears at startup explaining the issue
2. ⚠️ Warning includes exact copy-paste snippet to fix
3. ❌ Starter entities will NOT be discovered (unless consumer adds starter package)
4. ✅ Consumer entities continue to work

## 🧪 Verification Commands

### Run Consumer App with Debug Logging

```bash
./gradlew :backend:bootRun --args='--debug --logging.level.org.springframework.boot.autoconfigure=DEBUG --logging.level.com.galoong=DEBUG'
```

### Enable Diagnostic Mode

Add to `application.yml`:

```yaml
ai-prompts:
  debug:
    scan: true
```

Then run:

```bash
./gradlew :backend:bootRun --args='--debug --logging.level.com.galoong=DEBUG'
```

## 📊 Expected Log Ordering

The logs should show that package registration happens **BEFORE** repository scanning:

### ✅ CORRECT Order (ImportBeanDefinitionRegistrar)

```
[INFO] AI Prompt Tracker: ✅ Registered base package 'com.galoong.aiprompttracker' to AutoConfigurationPackages
[INFO] AI Prompt Tracker: JPA scan bootstrap loaded (AutoConfigurationPackages extension)
[INFO] Bootstrapping Spring Data JPA repositories in DEFAULT mode.
[INFO] @EnableAutoConfiguration was declared on a class in the package 'com.yourcompany.yourapp'. Automatic @Repository and @Entity scanning is enabled.
[INFO] Finished Spring Data repository scanning in 105 ms. Found 4 JPA repository interfaces.
```

**Key indicators:**
- "Registered base package" appears BEFORE "Bootstrapping Spring Data JPA"
- "Finished Spring Data repository scanning" shows 4 repositories (2 consumer + 2 starter)

### ❌ INCORRECT Order (BeanFactoryPostProcessor - old approach)

```
[INFO] Bootstrapping Spring Data JPA repositories in DEFAULT mode.
[INFO] Finished Spring Data repository scanning in 105 ms. Found 2 JPA repository interfaces.
[INFO] AI Prompt Tracker: JPA scan BFPP started - appending package to AutoConfigurationPackages
[INFO] AI Prompt Tracker: ✅ Appended 'com.galoong.aiprompttracker' to AutoConfigurationPackages
```

**Problem:**
- "Finished Spring Data repository scanning" happens BEFORE "JPA scan BFPP started"
- Only 2 repositories found (consumer only, starter missed)
- BFPP runs too late!

## 🔍 Detailed Verification Steps

### Step 1: Verify Registrar Timing

**Command:**
```bash
./gradlew :backend:bootRun --args='--debug' 2>&1 | grep -E "(Registered base package|Bootstrapping Spring|Finished Spring Data)" | head -5
```

**Expected output:**
```
AI Prompt Tracker: ✅ Registered base package 'com.galoong.aiprompttracker' to AutoConfigurationPackages
Bootstrapping Spring Data JPA repositories in DEFAULT mode.
Finished Spring Data repository scanning in 105 ms. Found 4 JPA repository interfaces.
```

**Success criteria:**
- "Registered base package" appears FIRST
- "Finished... Found 4 JPA repository interfaces" (includes starter repos)

### Step 2: Verify Repository Discovery

**Command:**
```bash
./gradlew :backend:bootRun --args='--debug' 2>&1 | grep "Creating shared instance of singleton bean" | grep -i repository
```

**Expected output:**
```
Creating shared instance of singleton bean 'executionRepository'
Creating shared instance of singleton bean 'callRepository'
Creating shared instance of singleton bean 'yourConsumerRepository'
```

**Success criteria:**
- Both `executionRepository` and `callRepository` are present
- Consumer repositories also present

### Step 3: Verify Entity Discovery

**Command:**
```bash
./gradlew :backend:bootRun --args='--debug --ai-prompts.debug.scan=true' 2>&1 | grep -A30 "JPA SCAN DIAGNOSTICS"
```

**Expected output:**
```
╔═══════════════════════════════════════════════════════════════════════════════╗
║                    AI PROMPT TRACKER - JPA SCAN DIAGNOSTICS                   ║
╚═══════════════════════════════════════════════════════════════════════════════╝

[1] AutoConfigurationPackages (used for BOTH repository AND entity scanning):
    ✅ Total packages: 2
    ✅ Consumer packages: 1
       - com.yourcompany.yourapp
    ✅ Starter packages: 1
       - com.galoong.aiprompttracker

[2] EntityScanPackages (OPTIONAL - used for entity scanning if set):
    ✅ EntityScanPackages NOT set (this is GOOD and expected)
    → Hibernate will use AutoConfigurationPackages for entity scanning
    → Both consumer and starter entities will be discovered from AutoConfigurationPackages

[3] JPA Entities discovered by Hibernate:
    ✅ Total entities: 4
    ✅ Consumer entities: 2
       - com.yourcompany.yourapp.domain.User
       - com.yourcompany.yourapp.domain.Product
    ✅ Starter entities: 2
       - com.galoong.aiprompttracker.domain.entity.Execution
       - com.galoong.aiprompttracker.domain.entity.Call
```

**Success criteria:**
- Both consumer and starter packages in AutoConfigurationPackages
- EntityScanPackages NOT set (or if set, includes starter package)
- All entities from both packages discovered

### Step 4: Test API Endpoints

**Command:**
```bash
# Wait for application to start, then:
curl http://localhost:8080/aiprompt-tracker/api/dashboard/summary
```

**Expected:**
- Returns JSON response (not 500 error)
- Indicates starter repositories are working

## ⚠️ Testing Explicit @EntityScan Warning

### Add Explicit @EntityScan to Consumer

Modify your consumer's main application class:

```java
@SpringBootApplication
@EntityScan(basePackages = "com.yourcompany.yourapp.domain")  // Excludes starter!
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

### Run and Check Warning

**Command:**
```bash
./gradlew :backend:bootRun 2>&1 | grep -A40 "CRITICAL WARNING"
```

**Expected output:**
```
╔═══════════════════════════════════════════════════════════════════════════════╗
║                              ⚠️  CRITICAL WARNING ⚠️                           ║
║                     Starter Entities Will NOT Be Discovered!                  ║
╚═══════════════════════════════════════════════════════════════════════════════╝

Your application uses @EntityScan which creates an EntityScanPackages bean.
Hibernate will scan ONLY the packages in EntityScanPackages and IGNORE
AutoConfigurationPackages (which includes the starter entity package).

Current EntityScanPackages:
  • com.yourcompany.yourapp.domain

Missing: com.galoong.aiprompttracker.domain.entity

═══════════════════════════════════════════════════════════════════════════════
SOLUTION: Add starter entity package to your @EntityScan:
═══════════════════════════════════════════════════════════════════════════════

@SpringBootApplication
@EntityScan(basePackages = {
    "com.yourcompany.yourapp.domain",
    "com.galoong.aiprompttracker.domain.entity"  // Add this line!
})
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}

═══════════════════════════════════════════════════════════════════════════════
IMPACT:
  • Starter entities (Execution, Call) will NOT be managed by JPA
  • Starter repositories (ExecutionRepository, CallRepository) will fail
  • Application may fail at runtime if persistence mode requires starter entities
═══════════════════════════════════════════════════════════════════════════════
```

**Success criteria:**
- Clear multi-line warning appears
- Shows exact copy-paste snippet to fix
- Explains the impact

## 📈 Success Criteria Summary

| Criterion | How to Verify | Expected Result |
|-----------|---------------|-----------------|
| Registrar runs early | Check log order | "Registered base package" BEFORE "Finished Spring Data repository scanning" |
| Repository count correct | Check "Finished Spring Data" log | "Found 4 JPA repository interfaces" (2 consumer + 2 starter) |
| Starter repositories created | Check bean creation logs | Both `executionRepository` and `callRepository` appear |
| Starter entities discovered | Check diagnostic output | Both `Execution` and `Call` entities listed |
| Consumer repos still work | Check repository count | Consumer repos appear in discovery |
| Consumer entities still work | Check entity discovery | Consumer entities appear in discovery |
| Warning for explicit @EntityScan | Add @EntityScan without starter package | Clear warning with copy-paste snippet appears |

## 🐛 Troubleshooting

### Problem: Only 2 repositories found (consumer only)

**Symptom:**
```
Finished Spring Data repository scanning in 105 ms. Found 2 JPA repository interfaces.
```

**Diagnosis:**
- Check log order - if BFPP logs appear AFTER "Finished Spring Data", registrar ran too late
- Old BeanFactoryPostProcessor approach

**Solution:**
- Ensure using ImportBeanDefinitionRegistrar (this document)
- Check `@Import(AiPromptTrackerAutoConfigPackageRegistrar.class)` in auto-configuration

### Problem: "Not a managed type: Execution" error

**Symptom:**
```
Caused by: java.lang.IllegalArgumentException: Not a managed type: class com.galoong.aiprompttracker.domain.entity.Execution
```

**Diagnosis:**
- Consumer has explicit @EntityScan that excludes starter package
- Or package registration failed

**Solution:**
- Check for warning at startup about EntityScanPackages
- Add starter entity package to @EntityScan
- Or remove @EntityScan to use default scanning

### Problem: No warning for explicit @EntityScan

**Symptom:**
- Consumer uses @EntityScan without starter package
- No warning appears

**Diagnosis:**
- `AiPromptTrackerJpaConsumerWarningsAutoConfiguration` not loaded
- Check it's in @Import list

**Solution:**
- Verify auto-configuration imports
- Check conditional on JPA classpath

## 📚 Related Documentation

- [JPA_SCANNING_IMPLEMENTATION.md](./JPA_SCANNING_IMPLEMENTATION.md) - Implementation details
- [JPA_SCANNING_TROUBLESHOOTING.md](./JPA_SCANNING_TROUBLESHOOTING.md) - Detailed troubleshooting
- [STARTER_PUBLISHING.md](./STARTER_PUBLISHING.md) - Consumer integration guide
