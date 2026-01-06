# AI Prompt Tracker - Spring Boot Starter Refactor Guide

## Overview

This guide documents the refactoring of `ai-prompt-tracker` into a Spring Boot starter that users can add as a dependency to get:
1. Automatic AI call tracking with `@AIPrompt`
2. Embedded dashboard UI at `/aiprompt-tracker`
3. REST API at `/aiprompt-tracker/api/**`
4. Safe defaults (no DB unless demo mode enabled)

---

## Architecture

### Module Structure

```
ai-prompt-tracker/
├── tracker-starter/          # ← The publishable Spring Boot starter
│   ├── build.gradle.kts     # Library (not application)
│   └── src/main/
│       ├── java/com/galoong/aiprompttracker/
│       │   ├── annotation/   # @AIPrompt
│       │   ├── aop/          # Aspect
│       │   ├── context/      # TrackingContext
│       │   ├── interceptor/  # WebClient, RestTemplate, etc.
│       │   ├── classifier/   # Provider/Model extraction
│       │   ├── parser/       # Usage metrics
│       │   ├── collector/    # CallCollector
│       │   ├── storage/      # ExecutionStore, CallStore
│       │   ├── domain/       # Entities, Repositories
│       │   ├── api/          # Controllers, Services, DTOs
│       │   ├── config/       # Properties, Auto-configuration
│       │   └── ui/           # UI Controller
│       └── resources/
│           ├── META-INF/
│           │   ├── spring/
│           │   │   └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│           │   └── resources/aiprompt-tracker/  # Embedded UI
│           │       ├── index.html
│           │       ├── assets/
│           │       └── ...
│           └── db/migration/tracking/  # Flyway migrations
└── backend/                 # ← Demo application (depends on starter)
    ├── build.gradle.kts     # Depends on tracker-starter
    ├── src/main/
    │   ├── java/            # Example @AIPrompt usage
    │   └── resources/
    │       └── application.yml
    └── docker/              # Docker Compose for local testing
```

---

## Step-by-Step Refactor

### Step 1: Update Root Configuration

**`settings.gradle.kts`:**
```kotlin
rootProject.name = "ai-prompt-tracker"

include("tracker-starter")  // ← The starter
include("backend")           // ← Demo app
```

**`build.gradle.kts` (root):**
```kotlin
// Keep existing - just ensure subprojects doesn't apply Spring Boot plugin to starter
allprojects {
    group = "com.galoong"
    version = "1.0.0-SNAPSHOT"
    repositories {
        mavenCentral()
    }
}
```

---

### Step 2: Create `tracker-starter/build.gradle.kts`

```kotlin
plugins {
    `java-library`
    `maven-publish`
    id("org.springframework.boot") version "3.4.1" apply false
    id("io.spring.dependency-management") version "1.1.7"
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

dependencies {
    // Auto-configuration
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Required
    api("org.springframework.boot:spring-boot-starter-aop")
    api("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Optional (for persistence)
    compileOnly("org.springframework.boot:spring-boot-starter-data-jpa")
    compileOnly("org.flywaydb:flyway-core")
    compileOnly("com.h2database:h2")

    // Optional (for OkHttp support)
    compileOnly("com.squareup.okhttp3:okhttp:4.12.0")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}

// Disable bootJar (this is a library)
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

// Enable standard jar
tasks.named<Jar>("jar") {
    enabled = true
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = "com.galoong"
            artifactId = "ai-prompt-tracker-starter"
            version = project.version.toString()
        }
    }
}
```

---

### Step 3: Copy Code to `tracker-starter`

Copy these packages from `backend/src/main/java/com/galoong/aiprompttracker/` to `tracker-starter/src/main/java/com/galoong/aiprompttracker/`:

1. **Tracking Core:**
   - `annotation/AIPrompt.java`
   - `aop/AIPromptAspect.java`
   - `context/TrackingContext.java`
   - `context/ExecutionContext.java`

2. **HTTP Interceptors:**
   - `interceptor/TrackingWebClientFilter.java`
   - `interceptor/TrackingRestTemplateInterceptor.java`
   - `interceptor/TrackingRestClientInterceptor.java`
   - `interceptor/TrackingOkHttpInterceptor.java`

3. **Classification & Parsing:**
   - `classifier/ProviderClassifier.java`
   - `classifier/ModelExtractor.java`
   - `parser/UsageMetricsParser.java`

4. **Collection & Storage:**
   - `collector/CallCollector.java`
   - `collector/DefaultCallCollector.java`
   - `collector/CallRecordInput.java`
   - `storage/ExecutionStore.java` + implementations
   - `storage/CallStore.java` + implementations

5. **Utilities:**
   - `util/BodyCaptureUtil.java`

6. **Domain (JPA):**
   - `domain/entity/ExecutionRecord.java`
   - `domain/entity/CallRecord.java`
   - `domain/repository/ExecutionRepository.java`
   - `domain/repository/ExecutionRepositoryExtended.java`
   - `domain/repository/CallRepository.java`
   - `domain/repository/CallRepositoryExtended.java`

7. **API Layer:**
   - `api/controller/DashboardController.java`
   - `api/controller/FunctionController.java`
   - `api/controller/CallController.java`
   - `api/controller/ExecutionController.java`
   - `api/service/` (all services)
   - `api/dto/` (all DTOs)
   - `api/exception/` (exception handlers)

8. **Configuration:**
   - `config/properties/TrackingCaptureProperties.java`
   - `config/properties/TrackingPersistenceProperties.java`
   - `config/properties/TrackingFlywayProperties.java`
   - `config/properties/TrackingDemoProperties.java`

---

### Step 4: Create Auto-Configuration

**`tracker-starter/src/main/java/com/galoong/aiprompttracker/config/AiPromptTrackerAutoConfiguration.java`:**

```java
package com.galoong.aiprompttracker.config;

import com.galoong.aiprompttracker.aop.AIPromptAspect;
import com.galoong.aiprompttracker.classifier.ModelExtractor;
import com.galoong.aiprompttracker.classifier.ProviderClassifier;
import com.galoong.aiprompttracker.collector.CallCollector;
import com.galoong.aiprompttracker.collector.DefaultCallCollector;
import com.galoong.aiprompttracker.context.TrackingContext;
import com.galoong.aiprompttracker.interceptor.*;
import com.galoong.aiprompttracker.parser.UsageMetricsParser;
import com.galoong.aiprompttracker.storage.CallStore;
import com.galoong.aiprompttracker.storage.ExecutionStore;
import com.galoong.aiprompttracker.config.properties.TrackingCaptureProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Main auto-configuration for AI Prompt Tracker.
 *
 * Registers core tracking components (AOP, interceptors, classifiers).
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties({TrackingCaptureProperties.class})
public class AiPromptTrackerAutoConfiguration {

    public AiPromptTrackerAutoConfiguration() {
        log.info("AI Prompt Tracker: Initializing auto-configuration");
    }

    // ========== Core Tracking ==========

    @Bean
    @ConditionalOnMissingBean
    public ProviderClassifier providerClassifier() {
        return new ProviderClassifier();
    }

    @Bean
    @ConditionalOnMissingBean
    public ModelExtractor modelExtractor() {
        return new ModelExtractor();
    }

    @Bean
    @ConditionalOnMissingBean
    public UsageMetricsParser usageMetricsParser() {
        return new UsageMetricsParser();
    }

    @Bean
    @ConditionalOnMissingBean
    public CallCollector callCollector(CallStore callStore) {
        return new DefaultCallCollector(callStore);
    }

    @Bean
    @ConditionalOnMissingBean
    public AIPromptAspect aiPromptAspect(ExecutionStore executionStore) {
        return new AIPromptAspect(executionStore);
    }

    @Bean
    @ConditionalOnMissingBean
    public TrackingContext trackingContext(ExecutionStore executionStore) {
        return new TrackingContext(executionStore);
    }

    // ========== HTTP Client Interceptors ==========

    @Bean
    @ConditionalOnClass(WebClient.class)
    @ConditionalOnMissingBean
    public TrackingWebClientFilter trackingWebClientFilter(
            ProviderClassifier providerClassifier,
            ModelExtractor modelExtractor,
            CallCollector callCollector,
            UsageMetricsParser usageMetricsParser,
            TrackingCaptureProperties captureProperties) {
        log.info("AI Prompt Tracker: Registering WebClient filter");
        return new TrackingWebClientFilter(
                providerClassifier, modelExtractor, callCollector,
                usageMetricsParser, captureProperties);
    }

    @Bean
    @ConditionalOnClass(WebClient.class)
    @ConditionalOnMissingBean
    public WebClientCustomizer trackingWebClientCustomizer(TrackingWebClientFilter filter) {
        return builder -> builder.filter(filter);
    }

    @Bean
    @ConditionalOnClass(RestTemplate.class)
    @ConditionalOnMissingBean
    public RestTemplateCustomizer trackingRestTemplateCustomizer(
            ProviderClassifier providerClassifier,
            ModelExtractor modelExtractor,
            CallCollector callCollector,
            UsageMetricsParser usageMetricsParser,
            TrackingCaptureProperties captureProperties) {
        log.info("AI Prompt Tracker: Registering RestTemplate interceptor");
        return restTemplate -> {
            restTemplate.getInterceptors().add(new TrackingRestTemplateInterceptor(
                    providerClassifier, modelExtractor, callCollector,
                    usageMetricsParser, captureProperties));
        };
    }

    @Bean
    @ConditionalOnClass(RestClient.class)
    @ConditionalOnMissingBean
    public RestClientCustomizer trackingRestClientCustomizer(
            ProviderClassifier providerClassifier,
            ModelExtractor modelExtractor,
            CallCollector callCollector,
            UsageMetricsParser usageMetricsParser,
            TrackingCaptureProperties captureProperties) {
        log.info("AI Prompt Tracker: Registering RestClient interceptor");
        return builder -> builder.requestInterceptor(new TrackingRestClientInterceptor(
                providerClassifier, modelExtractor, callCollector,
                usageMetricsParser, captureProperties));
    }
}
```

---

### Step 5: Create Persistence Auto-Configuration

**`tracker-starter/src/main/java/com/galoong/aiprompttracker/config/AiPromptTrackerPersistenceAutoConfiguration.java`:**

```java
package com.galoong.aiprompttracker.config;

import com.galoong.aiprompttracker.config.properties.*;
import com.galoong.aiprompttracker.domain.repository.CallRepository;
import com.galoong.aiprompttracker.domain.repository.ExecutionRepository;
import com.galoong.aiprompttracker.storage.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;

/**
 * Auto-configuration for persistence layer.
 *
 * Only active when persistence.mode=jdbc OR demo.enabled=true
 */
@Slf4j
@AutoConfiguration(after = AiPromptTrackerAutoConfiguration.class)
@EnableConfigurationProperties({
    TrackingPersistenceProperties.class,
    TrackingFlywayProperties.class,
    TrackingDemoProperties.class
})
public class AiPromptTrackerPersistenceAutoConfiguration {

    // ========== Storage Beans ==========

    @Bean
    @ConditionalOnProperty(name = "ai-prompts.tracking.persistence.mode", havingValue = "jdbc")
    public ExecutionStore jpaExecutionStore(ExecutionRepository executionRepository) {
        log.info("AI Prompt Tracker: Enabling JPA execution store (persistence.mode=jdbc)");
        return new JpaExecutionStore(executionRepository);
    }

    @Bean
    @ConditionalOnProperty(name = "ai-prompts.tracking.persistence.mode", havingValue = "jdbc")
    public CallStore jpaCallStore(CallRepository callRepository) {
        log.info("AI Prompt Tracker: Enabling JPA call store (persistence.mode=jdbc)");
        return new JpaCallStore(callRepository);
    }

    @Bean
    @ConditionalOnMissingBean(ExecutionStore.class)
    public ExecutionStore noopExecutionStore() {
        log.info("AI Prompt Tracker: Using no-op execution store (persistence disabled)");
        return new NoopExecutionStore();
    }

    @Bean
    @ConditionalOnMissingBean(CallStore.class)
    public CallStore noopCallStore() {
        log.info("AI Prompt Tracker: Using no-op call store (persistence disabled)");
        return new NoopCallStore();
    }

    // ========== Demo Mode DataSource ==========

    @Bean
    @ConditionalOnProperty(name = "ai-prompts.tracking.demo.enabled", havingValue = "true")
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource demoDataSource() {
        log.warn("=".repeat(80));
        log.warn("AI PROMPT TRACKER: DEMO MODE ENABLED");
        log.warn("Using H2 in-memory database. Data will be lost on restart.");
        log.warn("For production, disable demo mode and configure a real datasource.");
        log.warn("=".repeat(80));

        return DataSourceBuilder.create()
                .driverClassName("org.h2.Driver")
                .url("jdbc:h2:mem:ai-prompt-tracker;DB_CLOSE_DELAY=-1;MODE=PostgreSQL")
                .username("sa")
                .password("")
                .build();
    }

    // ========== Flyway Migrations ==========

    @Bean(initMethod = "migrate")
    @ConditionalOnClass(Flyway.class)
    @ConditionalOnBean(DataSource.class)
    public Flyway trackingFlyway(
            DataSource dataSource,
            TrackingFlywayProperties flywayProps,
            TrackingDemoProperties demoProps) {

        boolean enabled = flywayProps.isEnabled() || demoProps.isEnabled();

        if (!enabled) {
            log.debug("AI Prompt Tracker: Flyway migrations disabled");
            return Flyway.configure().dataSource(dataSource).load();
        }

        log.info("AI Prompt Tracker: Running Flyway migrations");

        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/tracking")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .table("flyway_tracking_schema_history")
                .load();
    }
}
```

---

### Step 6: Create UI Controller

**`tracker-starter/src/main/java/com/galoong/aiprompttracker/ui/AiPromptTrackerUiController.java`:**

```java
package com.galoong.aiprompttracker.ui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for serving the embedded AI Prompt Tracker dashboard UI.
 *
 * Serves UI at /aiprompt-tracker and handles SPA routing.
 */
@Controller
@RequestMapping("/aiprompt-tracker")
public class AiPromptTrackerUiController {

    /**
     * Serve the main UI page
     */
    @GetMapping({"", "/", "/index.html"})
    public String index() {
        return "forward:/aiprompt-tracker/index.html";
    }

    /**
     * SPA fallback: any non-API path under /aiprompt-tracker returns index.html
     */
    @GetMapping("/**")
    public String spa() {
        return "forward:/aiprompt-tracker/index.html";
    }
}
```

---

### Step 7: Create Web Configuration for Static Resources

**`tracker-starter/src/main/java/com/galoong/aiprompttracker/config/AiPromptTrackerWebConfiguration.java`:**

```java
package com.galoong.aiprompttracker.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for serving embedded UI and handling SPA routing.
 */
@Slf4j
@Configuration
public class AiPromptTrackerWebConfiguration implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve static UI assets at /aiprompt-tracker/**
        registry.addResourceHandler("/aiprompt-tracker/**")
                .addResourceLocations("classpath:/META-INF/resources/aiprompt-tracker/")
                .resourceChain(true);

        log.info("AI Prompt Tracker: Registered UI resource handler at /aiprompt-tracker/**");
    }
}
```

---

### Step 8: Create AutoConfiguration Imports File

**`tracker-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:**

```
com.galoong.aiprompttracker.config.AiPromptTrackerAutoConfiguration
com.galoong.aiprompttracker.config.AiPromptTrackerPersistenceAutoConfiguration
com.galoong.aiprompttracker.config.AiPromptTrackerWebConfiguration
```

---

### Step 9: Embed Frontend Build

```bash
# Build frontend
cd frontend
npm run build

# Copy build output to tracker-starter resources
mkdir -p ../tracker-starter/src/main/resources/META-INF/resources/aiprompt-tracker
cp -r dist/* ../tracker-starter/src/main/resources/META-INF/resources/aiprompt-tracker/
```

**Update frontend build to use correct base path:**

`frontend/next.config.js` or `frontend/vite.config.ts`:
```javascript
export default {
  base: '/aiprompt-tracker/',
  build: {
    outDir: 'dist'
  }
}
```

---

### Step 10: Update Backend to Use Starter

**`backend/build.gradle.kts`:**

```kotlin
dependencies {
    // Use the starter!
    implementation(project(":tracker-starter"))

    // Database for demo/testing
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")

    // Monitoring (optional)
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Development
    developmentOnly("org.springframework.boot:spring-boot-devtools")
}

// Keep bootJar enabled (this is an application)
tasks.bootJar {
    enabled = true
}
```

**`backend/src/main/resources/application.yml`:**

```yaml
server:
  port: 8080

# Demo mode - H2 + auto-migrate
ai-prompts:
  tracking:
    demo:
      enabled: true  # One line!
    capture-enabled: true
    capture-mode: SAFE
    store-raw-data: true

spring:
  application:
    name: ai-prompt-tracker-demo
```

**`backend/src/main/java/com/galoong/aiprompttracker/DemoApplication.java`:**

```java
package com.galoong.aiprompttracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.java, args);
    }
}
```

**`backend/src/main/java/com/galoong/aiprompttracker/example/ExampleService.java`:**

```java
package com.galoong.aiprompttracker.example;

import com.galoong.aiprompttracker.annotation.AIPrompt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class ExampleService {

    private final RestClient.Builder restClientBuilder;

    @AIPrompt(name = "openai-chat", category = "chat")
    public String chatWithOpenAI(String prompt) {
        RestClient restClient = restClientBuilder.baseUrl("https://api.openai.com").build();

        return restClient.post()
                .uri("/v1/chat/completions")
                .header("Authorization", "Bearer " + System.getenv("OPENAI_API_KEY"))
                .body(Map.of(
                    "model", "gpt-4",
                    "messages", List.of(Map.of("role", "user", "content", prompt))
                ))
                .retrieve()
                .body(String.class);
    }
}
```

---

## Usage for External Apps

### 1. Add Dependency

**`build.gradle.kts`:**
```kotlin
dependencies {
    implementation("com.galoong:ai-prompt-tracker-starter:1.0.0-SNAPSHOT")

    // Optional: For persistence
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")
}
```

### 2. Configure (Optional)

**Default (no config needed):**
```yaml
# Tracking works, but no persistence
# UI accessible at /aiprompt-tracker (shows empty dashboard)
```

**Demo Mode:**
```yaml
ai-prompts:
  tracking:
    demo:
      enabled: true
```

**Production:**
```yaml
ai-prompts:
  tracking:
    persistence:
      mode: jdbc
    flyway:
      enabled: false  # Manage migrations yourself

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/myapp
    username: myuser
    password: ${DB_PASSWORD}
```

### 3. Annotate Methods

```java
@Service
public class MyService {

    @AIPrompt(name = "summarize", category = "text-processing")
    public String summarize(String text) {
        // Call OpenAI/Anthropic/etc - automatically tracked!
        return chatClient.call(text);
    }
}
```

### 4. Access Dashboard

Open browser: `http://localhost:8080/aiprompt-tracker`

---

## File Copying Checklist

To complete the refactor, copy these directories from `backend/src/main/java/com/galoong/aiprompttracker/` to `tracker-starter/src/main/java/com/galoong/aiprompttracker/`:

- [ ] `annotation/`
- [ ] `aop/`
- [ ] `api/` (all controllers, services, DTOs, exceptions)
- [ ] `classifier/`
- [ ] `collector/`
- [ ] `context/`
- [ ] `domain/` (entities, repositories)
- [ ] `interceptor/`
- [ ] `parser/`
- [ ] `storage/`
- [ ] `util/`
- [ ] `config/properties/` (all property classes)

From `backend/src/main/resources/`:
- [ ] `db/migration/` → `tracker-starter/src/main/resources/db/migration/tracking/`

---

## Build & Publish

```bash
# Build starter
./gradlew :tracker-starter:build

# Publish to local Maven repo
./gradlew :tracker-starter:publishToMavenLocal

# Or publish to build/repo
./gradlew :tracker-starter:publish
```

---

## Testing

```bash
# Run demo app
cd backend
./gradlew bootRun

# Open browser
open http://localhost:8080/aiprompt-tracker

# Make some AI calls (trigger @AIPrompt methods)
curl -X POST http://localhost:8080/api/example/chat \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Hello AI"}'

# Refresh dashboard - should see tracked calls
```

---

## Summary

This refactoring transforms the monolithic backend into:

1. **tracker-starter** - Publishable Spring Boot starter (library)
2. **backend** - Demo application that uses the starter

Users can now:
- Add one dependency
- Annotate methods with `@AIPrompt`
- Access dashboard at `/aiprompt-tracker`
- No DB required by default (safe!)
- One-line demo mode for instant gratification

Maven coordinates:
```
com.galoong:ai-prompt-tracker-starter:1.0.0-SNAPSHOT
```
