# Dual-Phase EntityScan Detection Strategy

## 🎯 Problem Statement

When a consumer application uses `@EntityScan`, the starter's entities will NOT be discovered by Hibernate unless the consumer explicitly includes the starter entity package. This breaks the plug-and-play experience.

We need to detect this misconfiguration and warn the consumer with actionable guidance.

## 🚨 Challenge: Timing Matters

The challenge is **WHEN** to detect `@EntityScan` usage:

### Attempt 1: ImportBeanDefinitionRegistrar (Early Phase) ⚠️

**Timing:** Runs during `@Configuration` class processing (very early)

**Detection Method:** Check `BeanDefinitionRegistry` for "entityScanPackages" bean definition

**Problem:** The EntityScanPackages bean definition may not exist yet!

**Why?** The consumer's `@EntityScan` is processed by `EntityScanPackages.Registrar` (also an ImportBeanDefinitionRegistrar). The order of execution between different registrars is **NOT guaranteed** unless explicitly controlled.

**Scenarios:**
- ✅ **Scenario A:** Consumer's @EntityScan processed first → bean definition exists → detection succeeds
- ❌ **Scenario B:** Starter's registrar runs first → bean definition doesn't exist yet → detection fails

**Result:** Detection is **OPPORTUNISTIC** - may succeed or may miss

### Attempt 2: BeanFactoryPostProcessor (Late Phase) ✅

**Timing:** Runs after all bean definitions are loaded (late)

**Detection Method:** Query `BeanFactory` for `EntityScanPackages` bean instance

**Guarantee:** If consumer used `@EntityScan`, EntityScanPackages WILL exist at this point

**Why?** By the time BeanFactoryPostProcessors run:
- ALL configuration classes have been processed
- ALL ImportBeanDefinitionRegistrars have executed
- ALL bean definitions are fully registered

**Result:** Detection is **GUARANTEED** - never misses

## ✅ Solution: Dual-Phase Detection

Use BOTH mechanisms with duplicate prevention:

1. **Phase 1 (Early):** ImportBeanDefinitionRegistrar attempts to detect early
2. **Phase 2 (Late):** BeanFactoryPostProcessor guarantees detection
3. **Duplicate Prevention:** Shared state ensures only ONE warning emitted

## 📊 Spring Boot Startup Order

Understanding the startup order is critical:

```
1. @Configuration class scanning begins
2. ImportBeanDefinitionRegistrars execute (EARLY PHASE)
   ├─ Consumer's @EntityScan registrar may run (creates EntityScanPackages bean def)
   ├─ AiPromptTrackerAutoConfigPackageRegistrar runs (registers starter package)
   └─ AiPromptTrackerJpaEntityScanWarningRegistrar runs (may detect EntityScanPackages)
3. @EnableJpaRepositories processing
4. Spring Data JPA repository scanning
5. All bean definitions loaded
6. BeanFactoryPostProcessors execute (LATE PHASE)
   └─ EntityScanWarningBeanFactoryPostProcessor runs (guaranteed to detect)
7. Bean instantiation begins
8. Hibernate entity scanning
9. Application ready
```

## 🏗️ Implementation Architecture

### Component 1: EntityScanWarningState (Shared State)

**Purpose:** Prevent duplicate warnings across both phases

**Implementation:**
```java
public final class EntityScanWarningState {
    private static final AtomicBoolean WARNING_EMITTED = new AtomicBoolean(false);

    public static boolean tryEmitWarning(List<String> packages, String appClassName) {
        // Atomic check-and-set: only one thread/phase wins
        if (!WARNING_EMITTED.compareAndSet(false, true)) {
            return false; // Already emitted
        }
        logMissingPackageWarning(packages, appClassName);
        return true;
    }
}
```

**Thread Safety:** Uses `AtomicBoolean` for safe concurrent access

**Test Support:** Provides `resetForTesting()` method

### Component 2: AiPromptTrackerJpaEntityScanWarningRegistrar (Early Phase)

**Type:** ImportBeanDefinitionRegistrar

**When:** During @Configuration class processing (early)

**Detection Method:**
```java
// Check if EntityScanPackages bean definition exists
if (!registry.containsBeanDefinition("entityScanPackages")) {
    return; // Not found - late phase will check
}

// Extract packages from bean definition constructor args
List<String> packages = extractEntityScanPackages(registry);

// Check if starter package included
if (!EntityScanWarningState.hasStarterPackage(packages)) {
    EntityScanWarningState.tryEmitWarning(packages, appClassName);
}
```

**Limitation:** May miss EntityScanPackages if not yet registered

**Fallback:** Late phase will catch it

### Component 3: EntityScanWarningBeanFactoryPostProcessor (Late Phase)

**Type:** BeanFactoryPostProcessor

**Order:** `Ordered.LOWEST_PRECEDENCE` (runs last)

**When:** After all bean definitions loaded (late)

**Detection Method:**
```java
// Attempt to retrieve EntityScanPackages bean
EntityScanPackages bean = beanFactory.getBean(EntityScanPackages.class);

if (bean == null) {
    return; // Consumer didn't use @EntityScan - OK
}

// Extract packages
List<String> packages = Arrays.asList(bean.getPackageNames().toArray(new String[0]));

// Check if starter package included
if (!EntityScanWarningState.hasStarterPackage(packages)) {
    EntityScanWarningState.tryEmitWarning(packages, "YourApplication");
}
```

**Guarantee:** Never misses EntityScanPackages

**Duplicate Prevention:** Uses shared state to skip if already warned

## 📝 Auto-Configuration Wiring

```java
@AutoConfiguration
@AutoConfigureBefore({
    JpaRepositoriesAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
@Import({
    AiPromptTrackerAutoConfigPackageRegistrar.class,      // Package registration
    AiPromptTrackerJpaEntityScanWarningRegistrar.class    // Early warning detection
})
public class AiPromptTrackerJpaScanAutoConfiguration {

    @Bean
    public static BeanFactoryPostProcessor entityScanWarningBeanFactoryPostProcessor() {
        return new EntityScanWarningBeanFactoryPostProcessor();
    }
}
```

**Note:** Static @Bean method for BFPP to ensure proper ordering

## 🧪 Verification Scenarios

### Scenario A: Consumer WITHOUT @EntityScan (Default Case)

**Expected:**
- ✅ No warning (default is fine)
- ✅ Starter entities discovered via AutoConfigurationPackages

**Phase 1 (Early):**
- Registry check: `containsBeanDefinition("entityScanPackages")` → false
- Action: Return silently

**Phase 2 (Late):**
- Bean query: `getBean(EntityScanPackages.class)` → throws NoSuchBeanDefinitionException
- Action: Return silently

**Result:** No warning emitted ✅

### Scenario B: Consumer WITH @EntityScan (Missing Starter Package)

**Example:**
```java
@SpringBootApplication
@EntityScan("com.consumer.domain")  // Missing starter package!
public class ConsumerApp {}
```

**Expected:**
- ⚠️ Warning MUST appear
- ⚠️ Warning includes copy-paste solution snippet

**Phase 1 (Early) - May Succeed:**
- Registry check: `containsBeanDefinition("entityScanPackages")` → may be true
- Extract packages: `["com.consumer.domain"]`
- Check: Starter package missing
- Action: `EntityScanWarningState.tryEmitWarning()` → true (first to emit)
- Result: Warning emitted ✅

**Phase 2 (Late) - Skips:**
- Bean query: `getBean(EntityScanPackages.class)` → found
- Extract packages: `["com.consumer.domain"]`
- Check: Starter package missing
- Action: `EntityScanWarningState.tryEmitWarning()` → false (already emitted)
- Result: Skips silently ✅

**Phase 1 (Early) - May Miss:**
- Registry check: `containsBeanDefinition("entityScanPackages")` → false (timing!)
- Action: Return silently

**Phase 2 (Late) - Catches It:**
- Bean query: `getBean(EntityScanPackages.class)` → found
- Extract packages: `["com.consumer.domain"]`
- Check: Starter package missing
- Action: `EntityScanWarningState.tryEmitWarning()` → true (first to emit)
- Result: Warning emitted ✅

**Overall Result:** Warning GUARANTEED to appear (at least one phase detects it) ✅

### Scenario C: Consumer WITH @EntityScan (Including Starter Package)

**Example:**
```java
@SpringBootApplication
@EntityScan({
    "com.consumer.domain",
    "com.galoong.aiprompttracker.domain.entity"  // Correctly included!
})
public class ConsumerApp {}
```

**Expected:**
- ✅ No warning (configuration is correct)
- ✅ All entities discovered

**Phase 1 (Early):**
- Registry check: `containsBeanDefinition("entityScanPackages")` → may be true
- Extract packages: `["com.consumer.domain", "com.galoong.aiprompttracker.domain.entity"]`
- Check: Starter package present
- Action: Log success message, return

**Phase 2 (Late):**
- Bean query: `getBean(EntityScanPackages.class)` → found
- Extract packages: `["com.consumer.domain", "com.galoong.aiprompttracker.domain.entity"]`
- Check: Starter package present
- Action: Log success message, return

**Result:** No warning emitted ✅

## 📋 Warning Output Example

```
╔═══════════════════════════════════════════════════════════════════════════════╗
║                              ⚠️  CONFIGURATION WARNING ⚠️                      ║
║              Starter Entities Will NOT Be Discovered by Hibernate             ║
╚═══════════════════════════════════════════════════════════════════════════════╝

Your application uses @EntityScan which tells Hibernate to scan ONLY specific packages.
This overrides Spring Boot's default scanning (AutoConfigurationPackages).

Current @EntityScan packages:
  • com.consumer.domain

Missing: com.galoong.aiprompttracker.domain.entity

═══════════════════════════════════════════════════════════════════════════════
SOLUTION: Add starter entity package to your @EntityScan:
═══════════════════════════════════════════════════════════════════════════════

@SpringBootApplication
@EntityScan(basePackages = {
    "com.consumer.domain",
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
  • Starter repositories (ExecutionRepository, CallRepository) may fail
  • Application may fail at runtime if persistence mode requires starter entities

To verify this is correctly configured, enable diagnostic mode:
  ai-prompts.debug.scan=true
═══════════════════════════════════════════════════════════════════════════════
```

## ✅ Benefits of Dual-Phase Approach

1. **Reliability:** Guaranteed detection via late phase
2. **Early Feedback:** May detect early when possible
3. **No Duplicates:** Shared state prevents multiple warnings
4. **Non-Breaking:** Never modifies EntityScanPackages
5. **Clear Guidance:** Actionable copy-paste solution
6. **Test-Friendly:** Reset mechanism for test isolation

## 🔍 Debugging

Enable debug logging to see which phase detects the issue:

```yaml
logging:
  level:
    com.galoong.aiprompttracker.autoconfigure: DEBUG
```

**Expected logs:**

**Early phase detects:**
```
DEBUG - AI Prompt Tracker: Checking for explicit @EntityScan configuration...
DEBUG - AI Prompt Tracker: Explicit @EntityScan detected - checking if starter package is included...
DEBUG - AI Prompt Tracker: Warning emitted successfully in early phase
DEBUG - AI Prompt Tracker: Late-phase EntityScan detection started (BeanFactoryPostProcessor)
DEBUG - AI Prompt Tracker: Warning already emitted by early phase, skipping
```

**Late phase detects:**
```
DEBUG - AI Prompt Tracker: Checking for explicit @EntityScan configuration...
DEBUG - AI Prompt Tracker: No explicit @EntityScan detected (GOOD - using AutoConfigurationPackages)
DEBUG - AI Prompt Tracker: Late-phase EntityScan detection started (BeanFactoryPostProcessor)
DEBUG - AI Prompt Tracker: Starter entity package MISSING from @EntityScan - attempting to emit warning
DEBUG - AI Prompt Tracker: Warning emitted successfully in late phase
```

## 📚 Files Overview

| File | Purpose | Type |
|------|---------|------|
| `EntityScanWarningState.java` | Shared state + duplicate prevention | Utility class |
| `AiPromptTrackerJpaEntityScanWarningRegistrar.java` | Early-phase detection | ImportBeanDefinitionRegistrar |
| `EntityScanWarningBeanFactoryPostProcessor.java` | Late-phase detection (reliable) | BeanFactoryPostProcessor |
| `AiPromptTrackerJpaScanAutoConfiguration.java` | Wiring + @Bean registration | @AutoConfiguration |

## 🎯 Conclusion

The dual-phase detection strategy provides:
- **Guaranteed detection** via late-running BFPP
- **Early feedback** when possible via registrar
- **No false positives** (only warns when actually needed)
- **No false negatives** (always detects misconfigurations)
- **Clear actionable guidance** (copy-paste solution)
- **Never breaks consumer** (warning-only, never modifies beans)

This approach solves the timing-dependent detection problem while maintaining simplicity and reliability.
