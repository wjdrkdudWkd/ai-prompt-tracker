# AI Prompt Tracker - Architecture Analysis

## Executive Summary

AI Prompt Tracker is a Spring Boot observability library that automatically tracks AI API calls made within annotated methods. It provides a **non-invasive, annotation-based approach** to monitor AI usage, costs, and performance across multiple HTTP clients (WebClient, RestTemplate, RestClient, OkHttp).

**Core Concept**: 1 Execution (annotated method invocation) → N Calls (HTTP requests to AI providers)

---

## 1. High-Level Architecture

### Layer Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                      User Application                            │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  @AIPrompt("my-function")                                   │ │
│  │  public String myMethod() {                                 │ │
│  │      return aiClient.call(prompt); ← HTTP calls tracked    │ │
│  │  }                                                          │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                        AOP LAYER                                 │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  AIPromptAspect                                             │ │
│  │  - Intercepts @AIPrompt methods                            │ │
│  │  - Creates ExecutionContext (UUID, metadata)               │ │
│  │  - Sets ThreadLocal context                                │ │
│  │  - Handles success/error/cleanup                           │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    TRACKING LAYER                                │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  TrackingContext (ThreadLocal)                              │ │
│  │  - Holds ExecutionContext per thread                       │ │
│  │  - Aggregates calls into execution                         │ │
│  │  - Persists execution on method exit                       │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                   INTERCEPTOR LAYER                              │
│  ┌──────────────┬──────────────┬──────────────┬──────────────┐ │
│  │ WebClient    │ RestTemplate │ RestClient   │ OkHttp       │ │
│  │ Filter       │ Interceptor  │ Customizer   │ Interceptor  │ │
│  │              │              │              │              │ │
│  │ AUTO-TRACKED │ AUTO-TRACKED │ AUTO-TRACKED │ MANUAL SETUP │ │
│  └──────────────┴──────────────┴──────────────┴──────────────┘ │
│                                                                  │
│  Each interceptor:                                               │
│  1. Checks if TrackingContext.isTracking()                      │
│  2. Classifies provider (OpenAI, Anthropic, etc.)               │
│  3. Captures request/response bodies (with size limits)         │
│  4. Parses usage metrics (tokens, cost)                         │
│  5. Records call via CallCollector                              │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                   CLASSIFIER LAYER                               │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  ProviderClassifier                                         │ │
│  │  - Maps host/path → provider name                          │ │
│  │  - Extensible via CustomProviderMatcher beans              │ │
│  │                                                             │ │
│  │  ModelExtractor                                             │ │
│  │  - Extracts model from request body or path                │ │
│  │                                                             │ │
│  │  UsageMetricsParser                                         │ │
│  │  - Parses JSON responses for token counts                  │ │
│  │  - Provider-specific parsing (OpenAI, Anthropic, etc.)     │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                   COLLECTOR LAYER                                │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  DefaultCallCollector                                       │ │
│  │  - Adds call to ThreadLocal ExecutionContext               │ │
│  │  - Persists call to database immediately                   │ │
│  │  - Aggregates tokens/cost in execution                     │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                   PERSISTENCE LAYER                              │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  PostgreSQL Database                                        │ │
│  │  ┌──────────────────┐    ┌────────────────────────────┐   │ │
│  │  │ executions       │    │ calls                      │   │ │
│  │  │ ----------------│    │ -------------------------│   │ │
│  │  │ id (PK)          │    │ id (PK)                    │   │ │
│  │  │ function_name    │    │ execution_id (FK)          │   │ │
│  │  │ category         │    │ provider                   │   │ │
│  │  │ tags             │    │ model                      │   │ │
│  │  │ environment      │    │ prompt_tokens              │   │ │
│  │  │ started_at       │    │ completion_tokens          │   │ │
│  │  │ finished_at      │    │ total_tokens               │   │ │
│  │  │ duration_ms      │    │ cost                       │   │ │
│  │  │ status           │    │ latency_ms                 │   │ │
│  │  │ error_message    │    │ status                     │   │ │
│  │  │ calls_count      │    │ error_type                 │   │ │
│  │  │ total_cost       │    │ error_message              │   │ │
│  │  │ total_tokens     │    │ request_preview            │   │ │
│  │  └──────────────────┘    │ response_preview           │   │ │
│  │        1 ──────────────> N  raw_json                    │   │ │
│  │                           │ was_truncated              │   │ │
│  │                           │ created_at                 │   │ │
│  │                           └────────────────────────────┘   │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                      API LAYER                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  REST Controllers                                           │ │
│  │  - DashboardController: /api/dashboard/summary             │ │
│  │  - FunctionController: /api/functions                      │ │
│  │  - ExecutionController: /api/executions/{id}               │ │
│  │  - CallController: /api/calls                              │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                       UI LAYER                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  Next.js Dashboard (frontend/)                              │ │
│  │  - Dashboard page (KPI cards)                              │ │
│  │  - Functions list & detail                                 │ │
│  │  - Execution drawer (call timeline)                        │ │
│  │  - Calls explorer                                          │ │
│  │  - Providers comparison                                    │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### Layer Responsibilities

#### 1. AOP Layer (`tracking.aspect`)
- **Responsibility**: Entry and exit point for tracking
- **Key Class**: `AIPromptAspect`
- **Actions**:
  - Intercepts methods annotated with `@AIPrompt`
  - Generates unique execution ID (UUID)
  - Creates `ExecutionContext` with metadata (function name, category, tags, environment)
  - Sets ThreadLocal context via `TrackingContext.startExecution()`
  - Wraps method execution in try/catch/finally
  - Marks execution as success or error
  - **Always** clears ThreadLocal in finally block (prevents memory leaks)

#### 2. Tracking Layer (`tracking.context`)
- **Responsibility**: Manages execution lifecycle and ThreadLocal state
- **Key Classes**: `TrackingContext`, `ExecutionContext`
- **Actions**:
  - Maintains `ThreadLocal<ExecutionContext>` for thread-safe tracking
  - Provides `isTracking()` check for interceptors
  - Accumulates calls into execution context
  - Computes aggregates (total cost, total tokens, calls count)
  - Persists execution to database on method exit
  - Handles null-safety when DB is not configured

#### 3. Interceptor Layer (`tracking.interceptor`)
- **Responsibility**: Capture HTTP calls to AI providers
- **Key Classes**:
  - `TrackingWebClientFilter` (reactive)
  - `TrackingRestTemplateInterceptor` (blocking)
  - `TrackingOkHttpInterceptor` (blocking)
- **Actions**:
  - **Only** activates if `TrackingContext.isTracking()` is true
  - Classifies provider from host/path
  - Captures request/response bodies with **bounded memory limits**
  - Extracts model name from request or path
  - Parses usage metrics (tokens) from response
  - Records call via `CallCollector`
  - Reconstructs response for downstream consumption (critical for reactive flows)

#### 4. Classifier Layer (`tracking.classifier`)
- **Responsibility**: Identify AI providers and extract metadata
- **Key Classes**: `ProviderClassifier`, `ModelExtractor`, `UsageMetricsParser`
- **Actions**:
  - **ProviderClassifier**: Maps host → provider (e.g., `api.openai.com` → `OpenAI`)
  - **ModelExtractor**: Extracts model from JSON body or URL path
  - **UsageMetricsParser**: Provider-specific JSON parsing for token counts
  - Supports custom providers via `CustomProviderMatcher` beans

#### 5. Collector Layer (`tracking.collector`)
- **Responsibility**: Bridge between interceptors and persistence
- **Key Class**: `DefaultCallCollector`
- **Actions**:
  - Receives `CallRecordInput` from interceptors
  - Adds call to ThreadLocal `ExecutionContext` (for aggregation)
  - Persists call to database **immediately**
  - Links call to current execution via `executionId`

#### 6. Persistence Layer (`domain.entity`, `domain.repository`)
- **Responsibility**: JPA entities and repositories
- **Key Entities**: `ExecutionRecord`, `CallRecord`
- **Relationship**: 1 Execution → N Calls (enforced by `execution_id` foreign key)
- **Storage**: PostgreSQL with indexed columns for query performance

#### 7. API Layer (`api.controller`, `api.service`)
- **Responsibility**: REST endpoints for dashboard/analytics
- **Key Controllers**: Dashboard, Function, Execution, Call
- **Actions**:
  - Aggregate metrics across executions/calls
  - Provide paginated list endpoints
  - Support filtering by date, environment, provider, etc.

#### 8. UI Layer (`frontend/`)
- **Responsibility**: Next.js dashboard for visualization
- **Key Pages**: Dashboard, Functions, Executions (drawer), Calls, Providers
- **Integration**: Consumes REST API via React Query

---

## 2. Runtime Flow (Step-by-Step)

### Scenario: User calls `@AIPrompt` method that makes 3 AI API calls

```
Timeline of Events:

T=0ms   User calls annotated method
        ↓
T=1ms   AIPromptAspect.trackExecution() triggered (AOP)
        ↓
        - Generate executionId: "exec-abc123"
        - Create ExecutionContext (function="my-func", category="chat", env="prod")
        - TrackingContext.startExecution(context)  ← Sets ThreadLocal
        ↓
T=2ms   Execute user's method via pjp.proceed()
        ↓
        ┌─────────────────────────────────────────────────────┐
        │  User Method Body Executes                          │
        │                                                      │
        │  T=10ms  First AI API call via WebClient            │
        │          ↓                                           │
        │          TrackingWebClientFilter.filter() triggered │
        │          ↓                                           │
        │          - Check: TrackingContext.isTracking()?     │
        │            → YES (context exists)                   │
        │          - Classify: host="api.openai.com"          │
        │            → provider="OpenAI"                      │
        │          - Capture request body (bounded)           │
        │          - Execute HTTP call                        │
        │          ↓                                           │
        │  T=850ms Response received                          │
        │          ↓                                           │
        │          - Capture response body (bounded)          │
        │          - Parse usage: inputTokens=100,            │
        │            outputTokens=50, totalTokens=150         │
        │          - latency = 850-10 = 840ms                 │
        │          - CallCollector.recordCall()               │
        │            ↓                                         │
        │            - Add to ThreadLocal context             │
        │            - Persist to DB immediately:             │
        │              INSERT INTO calls (                    │
        │                execution_id='exec-abc123',          │
        │                provider='OpenAI',                   │
        │                model='gpt-4-turbo',                 │
        │                prompt_tokens=100,                   │
        │                completion_tokens=50,                │
        │                ...                                  │
        │              )                                       │
        │          - Rebuild response for user consumption    │
        │                                                      │
        │  T=900ms  Second AI API call (similar flow)         │
        │  T=1800ms Third AI API call (similar flow)          │
        │                                                      │
        │  T=2000ms User method returns result                │
        └─────────────────────────────────────────────────────┘
        ↓
T=2001ms Back in AIPromptAspect.trackExecution()
         ↓
         try block completes successfully
         ↓
         TrackingContext.endExecutionSuccess()
         ↓
         - context.markSuccess()
         - context.computeTotals()
           → totalCost = sum of all call costs
           → totalTokens = sum of all call tokens
           → callsCount = 3
         - Persist execution to DB:
           INSERT INTO executions (
             id='exec-abc123',
             function_name='my-func',
             category='chat',
             environment='prod',
             started_at=...,
             finished_at=...,
             duration_ms=2000,
             status='success',
             calls_count=3,
             total_cost=...,
             total_tokens=450
           )
         ↓
T=2002ms finally block executes
         ↓
         TrackingContext.clear()  ← CRITICAL: Removes ThreadLocal
         ↓
T=2003ms Return result to user
```

### Key Observations

1. **ThreadLocal Lifecycle**:
   - Set in `@Around` advice **before** method execution
   - Used by interceptors **during** method execution
   - Cleared in `finally` block **after** method execution
   - This prevents memory leaks in thread pool scenarios

2. **Persistence Strategy**:
   - **Calls**: Persisted immediately when captured (N inserts)
   - **Execution**: Persisted once at method exit (1 insert)
   - Trade-off: More DB calls, but no risk of losing call data if execution fails

3. **Reactive Safety**:
   - WebClient responses are **one-shot Flux<DataBuffer>**
   - Must capture bytes → rebuild Flux for downstream consumption
   - Uses `BodyCaptureUtil.captureDataBuffers()` with hard memory limits

4. **Error Handling**:
   - If user method throws exception:
     - `TrackingContext.endExecutionError(ex)` called
     - Execution marked as `status='error'`
     - Exception **re-thrown** to user (transparency)
   - If HTTP call fails:
     - Interceptor captures error via `.onErrorResume()`
     - Call marked as `status='error'`
     - Error **propagated** to user

---

## 3. Execution vs Call Model

### What is an Execution?

**Definition**: A single invocation of a method annotated with `@AIPrompt`.

**Lifecycle**: Start → Execute → End (success or error) → Persist

**Database Record** (`executions` table):
```sql
CREATE TABLE executions (
  id VARCHAR(36) PRIMARY KEY,           -- UUID generated by aspect
  function_name VARCHAR(100) NOT NULL,  -- From @AIPrompt.name()
  category VARCHAR(50),                 -- From @AIPrompt.category()
  tags TEXT[],                          -- From @AIPrompt.tags()
  environment VARCHAR(20) NOT NULL,     -- From spring.profiles.active
  started_at TIMESTAMP NOT NULL,        -- Instant.now() when aspect starts
  finished_at TIMESTAMP,                -- Instant.now() when method returns
  duration_ms BIGINT,                   -- finished_at - started_at
  status VARCHAR(20) NOT NULL,          -- 'success' or 'error'
  error_message TEXT,                   -- Exception message if error
  calls_count INT DEFAULT 0,            -- Count of AI calls made
  total_cost DOUBLE PRECISION,          -- Sum of call costs
  total_tokens BIGINT                   -- Sum of call tokens
);
```

**Purpose**:
- Represents **semantic unit** (user's business operation)
- Aggregates metrics from N calls
- Enables function-level analytics

**Example**:
```java
@AIPrompt(name = "generate-report", category = "reporting", tags = {"finance", "monthly"})
public Report generateReport(String month) {
    // This method execution = 1 Execution record
    String summary = aiClient.summarize(data);      // Call 1
    String insights = aiClient.analyze(data);       // Call 2
    String recommendations = aiClient.suggest(data); // Call 3
    return new Report(summary, insights, recommendations);
}
```

### What is a Call?

**Definition**: A single HTTP request to an AI provider (OpenAI, Anthropic, Google, etc.).

**Lifecycle**: Interceptor captures request → HTTP call → Response captured → Persist

**Database Record** (`calls` table):
```sql
CREATE TABLE calls (
  id VARCHAR(36) PRIMARY KEY,              -- UUID generated by DB
  execution_id VARCHAR(36) NOT NULL,       -- Foreign key to executions
  provider VARCHAR(50) NOT NULL,           -- 'OpenAI', 'Anthropic', etc.
  model VARCHAR(100) NOT NULL,             -- 'gpt-4-turbo', 'claude-3-opus'
  prompt_tokens INT,                       -- From usage metrics
  completion_tokens INT,                   -- From usage metrics
  total_tokens INT,                        -- From usage metrics or computed
  cost DOUBLE PRECISION,                   -- TODO: Calculate from pricing
  latency_ms BIGINT NOT NULL,              -- HTTP call duration
  status VARCHAR(20) NOT NULL,             -- 'success' or 'error'
  error_type VARCHAR(100),                 -- Exception class name
  error_message TEXT,                      -- Exception message
  request_preview TEXT,                    -- Truncated request JSON
  response_preview TEXT,                   -- Truncated response JSON
  raw_json TEXT,                           -- Full response (if not truncated)
  was_truncated BOOLEAN DEFAULT false,     -- Memory limit exceeded?
  created_at TIMESTAMP NOT NULL            -- When call was made
);
```

**Purpose**:
- Represents **technical unit** (HTTP call)
- Enables provider/model analytics
- Stores detailed request/response for debugging

**Example**: The 3 calls from the execution above

### How is 1 → N Relationship Enforced?

#### In Code (ThreadLocal)

1. **Execution Context Creation** (AIPromptAspect):
   ```java
   String executionId = UUID.randomUUID().toString();
   ExecutionContext context = ExecutionContext.builder()
       .executionId(executionId)
       .functionName(functionName)
       .build();
   TrackingContext.startExecution(context);  // Set ThreadLocal
   ```

2. **Call Recording** (Interceptors):
   ```java
   // Inside interceptor (e.g., TrackingWebClientFilter)
   if (!TrackingContext.isTracking()) {
       return next.exchange(request);  // Skip if no execution context
   }

   ExecutionContext context = TrackingContext.getCurrentExecution();
   String executionId = context.getExecutionId();  // Get parent execution ID

   CallRecordInput input = CallRecordInput.builder()
       .provider("OpenAI")
       .model("gpt-4-turbo")
       .build();

   callCollector.recordCall(input);
   ```

3. **Call Persistence** (DefaultCallCollector):
   ```java
   public void recordCall(CallRecordInput input) {
       ExecutionContext context = TrackingContext.getCurrentExecution();

       // Add to context (for aggregation)
       CallRecordData callData = ...;
       TrackingContext.addCall(callData);

       // Persist to DB with executionId foreign key
       CallRecord record = CallRecord.builder()
           .executionId(context.getExecutionId())  // Links to execution!
           .provider(input.getProvider())
           .build();

       callRepository.save(record);
   }
   ```

#### In Database (Foreign Key)

```sql
-- Foreign key constraint ensures referential integrity
ALTER TABLE calls
  ADD CONSTRAINT fk_calls_execution
  FOREIGN KEY (execution_id)
  REFERENCES executions(id);

-- Query all calls for an execution
SELECT * FROM calls WHERE execution_id = 'exec-abc123';

-- Query execution with call count
SELECT e.*, COUNT(c.id) as call_count
FROM executions e
LEFT JOIN calls c ON c.execution_id = e.id
WHERE e.id = 'exec-abc123'
GROUP BY e.id;
```

### Why ThreadLocal?

**Problem**: How to link HTTP calls (captured by interceptors) to the parent execution (started by AOP aspect)?

**Solution**: ThreadLocal provides thread-scoped storage

**Benefits**:
1. **Thread-safe**: Each thread has its own execution context
2. **No parameter passing**: Interceptors don't need `executionId` parameter
3. **Transparent**: User code doesn't need to know about tracking
4. **Works across layers**: AOP → Service → Repository → HTTP client

**Risks Mitigated**:
1. **Memory leaks**: `finally` block **always** clears ThreadLocal
2. **Thread pool pollution**: Context cleared before thread returns to pool
3. **Async boundaries**: Reactive WebClient preserves context via reactor context (not ThreadLocal in async chains)

**Limitation**:
- Does **not** propagate across async boundaries (e.g., `@Async` methods, new threads)
- Reactive flows use **same thread** for request/response, so ThreadLocal works
- For true async, would need Spring Cloud Sleuth-style context propagation

---

## 4. Tracking Entry Points

### Where Tracking STARTS

**Entry Point**: `AIPromptAspect.trackExecution()`

**Trigger**: Any method annotated with `@AIPrompt`

**Example**:
```java
@Service
public class MyService {

    @AIPrompt(name = "analyze-sentiment")  // ← Entry point
    public String analyzeSentiment(String text) {
        return aiClient.call(text);
    }
}
```

**What Happens**:
1. Spring AOP intercepts method call via `@Around` advice
2. Aspect creates `ExecutionContext` with UUID
3. Aspect calls `TrackingContext.startExecution(context)`
4. ThreadLocal is set with execution context
5. User method executes
6. All HTTP clients check `TrackingContext.isTracking()` → true
7. Interceptors activate

### Where Tracking STOPS

**Exit Point 1**: `TrackingContext.endExecutionSuccess()` (normal flow)
- Called when user method returns successfully
- Marks execution status as `'success'`
- Persists execution record

**Exit Point 2**: `TrackingContext.endExecutionError()` (error flow)
- Called when user method throws exception
- Marks execution status as `'error'`
- Persists execution record with error message
- Exception is **re-thrown** (transparent to user)

**Exit Point 3**: `TrackingContext.clear()` (always)
- Called in `finally` block
- Removes ThreadLocal context
- **Critical** for preventing memory leaks

**Code Flow**:
```java
@Around("@annotation(aiPrompt)")
public Object trackExecution(ProceedingJoinPoint pjp, AIPrompt aiPrompt) throws Throwable {
    // START
    TrackingContext.startExecution(context);

    try {
        Object result = pjp.proceed();

        // STOP (success)
        TrackingContext.endExecutionSuccess();

        return result;

    } catch (Throwable ex) {
        // STOP (error)
        TrackingContext.endExecutionError(ex);

        throw ex;  // Transparent to user

    } finally {
        // CLEANUP (always)
        TrackingContext.clear();  // Remove ThreadLocal
    }
}
```

### What Happens if Exception is Thrown?

**Scenario 1: User method throws exception**
```java
@AIPrompt("my-func")
public String myMethod() {
    String result = aiClient.call(prompt);  // Call 1 tracked
    throw new RuntimeException("Business logic error");
}
```

**Behavior**:
- Call 1 is tracked and persisted (status='success')
- Execution is tracked and persisted (status='error', error_message='Business logic error')
- Exception is re-thrown to caller
- ThreadLocal is cleared in `finally`

**Scenario 2: HTTP call fails**
```java
@AIPrompt("my-func")
public String myMethod() {
    return aiClient.call(prompt);  // Network error
}
```

**Behavior**:
- Call is tracked and persisted (status='error', error_type='WebClientResponseException')
- Exception propagates to user method
- Aspect catches exception
- Execution is tracked and persisted (status='error')
- Exception is re-thrown to caller
- ThreadLocal is cleared in `finally`

**Scenario 3: Database unavailable**
```java
// TrackingContext.persistExecution() catches exceptions
private static void persistExecution(ExecutionContext context) {
    try {
        if (executionRepository != null) {
            executionRepository.save(record);
        } else {
            log.warn("ExecutionRepository not available, cannot persist execution");
        }
    } catch (Exception e) {
        log.error("Failed to persist execution: {}", context.getExecutionId(), e);
        // Does NOT re-throw - tracking failure should not break user app
    }
}
```

**Behavior**:
- Tracking failure is **logged**, not thrown
- User method executes normally
- Application continues running (graceful degradation)

### How Async/Reactive Flows are Handled (WebClient)

**Challenge**: WebClient returns `Mono<T>` or `Flux<T>` (lazy evaluation)

**Solution**: Interceptor works at **subscription time**, not definition time

**Flow**:
```java
@AIPrompt("my-func")
public Mono<String> myMethod() {
    // Definition phase (no HTTP call yet)
    Mono<String> result = webClient.get().retrieve().bodyToMono(String.class);

    return result;  // Returns immediately
}

// Later, when caller subscribes:
myMethod().subscribe(response -> {
    // HTTP call happens HERE
    // TrackingWebClientFilter activates HERE
    // But ThreadLocal is already cleared!
});
```

**Problem**: ThreadLocal cleared before subscription

**Current Behavior**:
- If subscription happens in **same thread**, ThreadLocal works
- If subscription happens in **different thread** (e.g., `.publishOn()`), ThreadLocal is lost
- Tracking **does not work** across async thread boundaries

**Workaround** (not implemented):
- Use Reactor Context instead of ThreadLocal
- Propagate tracking context via `subscriberContext()`
- Requires refactoring TrackingContext

**Practical Impact**:
- Works for **most** Spring AI use cases (synchronous ChatClient)
- Works for **synchronous** WebClient usage (`.block()`)
- **Does not work** for true async/reactive chains with thread switching

---

## 5. Supported HTTP Clients

### Auto-Tracked Clients

These clients are **automatically** tracked with **zero configuration**:

#### 1. Spring WebClient (Reactive)

**How**: `TrackingAutoConfiguration` registers `WebClientCustomizer`

**Code**:
```java
@Bean
@ConditionalOnClass(WebClient.class)
public WebClientCustomizer trackingWebClientCustomizer(TrackingWebClientFilter filter) {
    return webClientBuilder -> webClientBuilder.filter(filter);
}
```

**Impact**: All `WebClient.Builder` instances get tracking filter added

**User Code** (zero config):
```java
@Autowired
private WebClient.Builder webClientBuilder;

@AIPrompt("my-func")
public String myMethod() {
    WebClient client = webClientBuilder.build();
    return client.get().uri("https://api.openai.com/v1/chat/completions")
        .retrieve()
        .bodyToMono(String.class)
        .block();  // Tracked automatically
}
```

#### 2. Spring RestTemplate (Blocking)

**How**: `TrackingAutoConfiguration` registers `RestTemplateCustomizer`

**Code**:
```java
@Bean
@ConditionalOnClass(RestTemplate.class)
public RestTemplateCustomizer trackingRestTemplateCustomizer(TrackingRestTemplateInterceptor interceptor) {
    return restTemplate -> restTemplate.getInterceptors().add(interceptor);
}
```

**Impact**: All `RestTemplate` instances get interceptor added

**User Code** (zero config):
```java
@Autowired
private RestTemplate restTemplate;

@AIPrompt("my-func")
public String myMethod() {
    return restTemplate.getForObject("https://api.anthropic.com/v1/messages", String.class);
    // Tracked automatically
}
```

#### 3. Spring RestClient (Spring Boot 3)

**How**: `TrackingAutoConfiguration` registers `RestClientCustomizer`

**Code**:
```java
@Bean
@ConditionalOnClass(RestClient.class)
public RestClientCustomizer trackingRestClientCustomizer(TrackingRestTemplateInterceptor interceptor) {
    return restClientBuilder -> restClientBuilder.requestInterceptor(interceptor);
}
```

**Impact**: All `RestClient.Builder` instances get interceptor added

**User Code** (zero config):
```java
@Autowired
private RestClient.Builder restClientBuilder;

@AIPrompt("my-func")
public String myMethod() {
    RestClient client = restClientBuilder.build();
    return client.get().uri("https://api.openai.com/v1/chat/completions")
        .retrieve()
        .body(String.class);  // Tracked automatically
}
```

### Manual Setup Required

#### 4. OkHttp

**Why Manual**: OkHttp clients are created directly, not via Spring

**How**: User must manually add interceptor

**User Code**:
```java
@Configuration
public class MyConfig {

    @Autowired
    private TrackingOkHttpInterceptor trackingInterceptor;  // Bean provided by library

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
            .addInterceptor(trackingInterceptor)  // Manual registration
            .build();
    }
}
```

**Then**:
```java
@AIPrompt("my-func")
public String myMethod() {
    Request request = new Request.Builder()
        .url("https://api.openai.com/v1/chat/completions")
        .build();

    Response response = okHttpClient.newCall(request).execute();  // Tracked
    return response.body().string();
}
```

### How Spring AI Fits In

**Spring AI Architecture**:
```
ChatClient (user-facing)
    ↓
OpenAiChatModel / AnthropicChatModel / etc.
    ↓
WebClient / RestClient (underneath)
    ↓
AI Provider API
```

**Tracking Integration**:
- Spring AI uses **WebClient** or **RestClient** internally
- Both are **auto-tracked** via customizers
- **No additional configuration needed**

**Example**:
```java
@Service
public class MyChatService {

    @Autowired
    private ChatClient chatClient;  // Spring AI

    @AIPrompt(name = "chat-with-ai")
    public String chat(String message) {
        return chatClient.prompt()
            .user(message)
            .call()
            .content();  // Tracked automatically via underlying WebClient
    }
}
```

**What Gets Tracked**:
- Provider: Detected from URL (e.g., `api.openai.com` → `OpenAI`)
- Model: Extracted from request JSON or path
- Tokens: Parsed from response JSON
- Latency: HTTP call duration
- Cost: TODO (requires pricing table)

---

## 6. Storage & Modes

### What Data is Stored?

#### Execution Record (always stored)

**Core Metadata** (always present):
- `id`: UUID
- `function_name`: From `@AIPrompt.name()`
- `category`: From `@AIPrompt.category()`
- `tags`: From `@AIPrompt.tags()`
- `environment`: From `spring.profiles.active`
- `started_at`, `finished_at`, `duration_ms`: Timestamps
- `status`: `'success'` or `'error'`
- `calls_count`: Number of AI calls

**Aggregated Metrics** (computed from calls):
- `total_cost`: Sum of all call costs
- `total_tokens`: Sum of all call tokens

**Error Details** (only if status='error'):
- `error_message`: Exception message

#### Call Record (always stored)

**Core Metadata** (always present):
- `id`: UUID (auto-generated)
- `execution_id`: Foreign key to parent execution
- `provider`: AI provider name
- `model`: Model name
- `latency_ms`: HTTP call duration
- `status`: `'success'` or `'error'`
- `created_at`: Timestamp

**Usage Metrics** (optional - depends on parsing):
- `prompt_tokens`: Parsed from response JSON
- `completion_tokens`: Parsed from response JSON
- `total_tokens`: Parsed or computed

**Cost** (optional - not implemented):
- `cost`: TODO - requires pricing table

**Error Details** (only if status='error'):
- `error_type`: Exception class name
- `error_message`: Exception message

**Debug Data** (optional - controlled by config):
- `request_preview`: Truncated request JSON
- `response_preview`: Truncated response JSON
- `raw_json`: Full response JSON (only if not truncated)
- `was_truncated`: Boolean flag

### Which Fields are Optional?

**Configuration** (`application.yml`):
```yaml
ai-prompts:
  tracking:
    store-raw-data: true           # Enable request/response capture
    max-request-bytes: 16384       # 16KB truncation limit
    max-response-bytes: 32768      # 32KB truncation limit
    max-in-memory-bytes: 2097152   # 2MB hard cap (prevents OOM)
    capture-content-types:
      - application/json           # Only capture JSON
```

**If `store-raw-data: false`**:
- `request_preview`, `response_preview`, `raw_json` are **null**
- Only metadata is stored (provider, model, tokens, latency, status)

**If Response > `max-response-bytes`**:
- `response_preview`: Truncated to 32KB + "...(truncated)"
- `raw_json`: **null** (too large)
- `was_truncated`: **true**

**If Response > `max-in-memory-bytes`**:
- `response_preview`: "[Response too large to capture]"
- `raw_json`: **null**
- `was_truncated`: **true**
- **Critical**: Hard cap prevents OutOfMemoryError

### How Dev/Test/Prod Modes Differ

**No Built-In Mode Logic** (intentionally simple)

**Current Behavior**:
- Same data captured in all environments
- `environment` field set from `spring.profiles.active`
- UI can filter by environment

**Recommended Configuration**:

**Dev**:
```yaml
spring:
  profiles:
    active: dev

ai-prompts:
  tracking:
    store-raw-data: true       # Capture request/response for debugging
    max-request-bytes: 32768   # Larger preview
    max-response-bytes: 65536
```

**Prod**:
```yaml
spring:
  profiles:
    active: prod

ai-prompts:
  tracking:
    store-raw-data: false      # Skip request/response (privacy/performance)
    # OR
    store-raw-data: true
    max-request-bytes: 1024    # Smaller preview
    max-response-bytes: 2048
```

**Test**:
```yaml
spring:
  profiles:
    active: test

ai-prompts:
  tracking:
    store-raw-data: true       # Capture for test verification
```

### What Happens if No DB is Configured?

**Code** (TrackingContext.java):
```java
private static void persistExecution(ExecutionContext context) {
    try {
        if (executionRepository != null) {
            executionRepository.save(record);
            log.info("Persisted execution {}", context.getExecutionId());
        } else {
            log.warn("ExecutionRepository not available, cannot persist execution");
        }
    } catch (Exception e) {
        log.error("Failed to persist execution: {}", context.getExecutionId(), e);
    }
}
```

**Behavior**:
1. **Tracking Still Works**: Execution context created, calls tracked
2. **No Persistence**: Data only exists in ThreadLocal during execution
3. **Log Warnings**: "ExecutionRepository not available"
4. **No Exception**: Application continues normally (graceful degradation)

**Use Case**: Testing tracking logic without database

**Limitation**: No historical data, no REST API endpoints work

---

## 7. REST API Surface

### All Endpoints

| Endpoint | Method | Purpose | Data Model |
|----------|--------|---------|------------|
| `/api/dashboard/summary` | GET | Aggregated metrics across all executions | DashboardSummaryResponse |
| `/api/functions` | GET | List all functions with aggregated metrics | Page<FunctionAggregateResponse> |
| `/api/functions/{name}` | GET | Function detail with provider/model breakdown | FunctionDetailResponse |
| `/api/functions/{name}/executions` | GET | List executions for a function | Page<ExecutionSummaryResponse> |
| `/api/executions/{id}` | GET | Execution detail with call timeline | ExecutionDetailResponse |
| `/api/calls` | GET | List all calls with filters | Page<CallResponse> |

### Endpoint Details

#### 1. Dashboard Summary

**Endpoint**: `GET /api/dashboard/summary?from=&to=&env=`

**Query Params**:
- `from` (optional): Start date (ISO timestamp)
- `to` (optional): End date (ISO timestamp)
- `env` (optional): Environment filter (dev, test, prod)

**Response** (`DashboardSummaryResponse`):
```json
{
  "totalCost": 4892.47,
  "totalExecutions": 12847,
  "totalCalls": 47293,
  "avgLatencyMs": 847.0,
  "avgCallsPerExecution": 3.68,
  "executionErrorRate": 0.024,
  "callErrorRate": 0.024
}
```

**Data Source**:
- Aggregates from `executions` and `calls` tables
- Joins: `executions` LEFT JOIN `calls` ON `execution_id`

#### 2. Functions List

**Endpoint**: `GET /api/functions?from=&to=&env=&category=&status=&q=&page=&size=`

**Query Params**:
- `from`, `to`, `env`: Date/environment filters
- `category`: Filter by category
- `status`: Filter by status (success, error)
- `q`: Search by function name
- `page`, `size`: Pagination (0-indexed)

**Response** (`Page<FunctionAggregateResponse>`):
```json
{
  "content": [
    {
      "functionName": "generate-summary",
      "category": "reporting",
      "tags": ["finance"],
      "executions": 1247,
      "calls": 8432,
      "callsPerExecution": 6.76,
      "totalCost": 2847.32,
      "avgExecutionTimeMs": 3247,
      "errorRate": 0.015
    }
  ],
  "totalElements": 142,
  "totalPages": 6,
  "size": 25,
  "number": 0
}
```

**Data Source**:
- Groups `executions` by `function_name`
- Aggregates: COUNT, SUM, AVG

#### 3. Function Detail

**Endpoint**: `GET /api/functions/{functionName}`

**Path Param**: `functionName` (URL-encoded)

**Response** (`FunctionDetailResponse`):
```json
{
  "functionName": "generate-summary",
  "category": "reporting",
  "tags": ["finance"],
  "totalExecutions": 1247,
  "totalCalls": 8432,
  "totalCost": 2847.32,
  "avgExecutionTimeMs": 3247,
  "errorRate": 0.015,
  "topProviders": [
    { "provider": "OpenAI", "calls": 6000, "cost": 2000.00 },
    { "provider": "Anthropic", "calls": 2432, "cost": 847.32 }
  ],
  "topModels": [
    { "model": "gpt-4-turbo", "calls": 6000, "cost": 2000.00 },
    { "model": "claude-3-opus", "calls": 2432, "cost": 847.32 }
  ]
}
```

**Data Source**:
- `executions` WHERE `function_name = ?`
- Joins `calls` for provider/model breakdown

#### 4. Function Executions

**Endpoint**: `GET /api/functions/{functionName}/executions?from=&to=&page=&size=`

**Response** (`Page<ExecutionSummaryResponse>`):
```json
{
  "content": [
    {
      "executionId": "exec-7f3a2b9c",
      "functionName": "generate-summary",
      "environment": "PROD",
      "startedAt": "2024-01-15T14:32:07Z",
      "durationMs": 2847,
      "status": "success",
      "callsCount": 5,
      "totalCost": 0.0847
    }
  ],
  "totalElements": 1247,
  "totalPages": 50,
  "size": 25,
  "number": 0
}
```

**Data Source**:
- `executions` WHERE `function_name = ?`
- Ordered by `started_at DESC`

#### 5. Execution Detail

**Endpoint**: `GET /api/executions/{executionId}`

**Response** (`ExecutionDetailResponse`):
```json
{
  "executionId": "exec-7f3a2b9c",
  "functionName": "generate-summary",
  "category": "reporting",
  "tags": ["finance"],
  "environment": "PROD",
  "startedAt": "2024-01-15T14:32:07Z",
  "finishedAt": "2024-01-15T14:32:10Z",
  "durationMs": 2847,
  "status": "success",
  "callsCount": 5,
  "totalTokens": 12847,
  "totalCost": 0.0847,
  "calls": [
    {
      "callId": "call-1",
      "provider": "OpenAI",
      "model": "gpt-4-turbo",
      "promptTokens": 1247,
      "completionTokens": 892,
      "totalTokens": 2139,
      "cost": 0.0124,
      "latencyMs": 847,
      "status": "success",
      "wasTruncated": false,
      "requestPreview": "{...}",
      "responsePreview": "{...}",
      "createdAt": "2024-01-15T14:32:08Z"
    }
  ]
}
```

**Data Source**:
- `executions` WHERE `id = ?`
- Joins `calls` WHERE `execution_id = ?`
- Ordered by `created_at ASC`

#### 6. Calls List

**Endpoint**: `GET /api/calls?provider=&model=&status=&from=&to=&page=&size=`

**Response** (`Page<CallResponse>`):
```json
{
  "content": [
    {
      "callId": "call-abc123",
      "executionId": "exec-7f3a2b9c",
      "functionName": "generate-summary",
      "provider": "OpenAI",
      "model": "gpt-4-turbo",
      "promptTokens": 1247,
      "completionTokens": 892,
      "totalTokens": 2139,
      "cost": 0.0124,
      "latencyMs": 847,
      "status": "success",
      "wasTruncated": false,
      "requestPreview": "{...}",
      "responsePreview": "{...}",
      "createdAt": "2024-01-15T14:32:08Z"
    }
  ],
  "totalElements": 47293,
  "totalPages": 1892,
  "size": 25,
  "number": 0
}
```

**Data Source**:
- `calls` table
- Joins `executions` for `function_name`

---

## 8. Extensibility Points

### How to Add a New AI Provider

**Scenario**: You're using a custom AI provider at `api.myai.com`

**Option 1: Implement CustomProviderMatcher** (recommended)

```java
@Component
public class MyAIProviderMatcher implements CustomProviderMatcher {

    @Override
    public String matchProvider(String host, String path) {
        if (host != null && host.contains("myai.com")) {
            return "MyAI";
        }
        return null;  // Not matched
    }

    @Override
    public int getOrder() {
        return 50;  // Higher priority than default (100)
    }
}
```

**How It Works**:
- `ProviderClassifier` auto-injects all `CustomProviderMatcher` beans
- Matchers checked in order (lower = higher priority)
- First non-null match wins

**Option 2: Configure Unknown Provider Capture**

```yaml
ai-prompts:
  tracking:
    capture-unknown-providers: true  # Track even if provider not recognized
```

**Then**:
- Unknown hosts tracked as `provider='Unknown'`
- Can analyze in dashboard

### How to Add a New HTTP Client

**Scenario**: You're using Apache HttpClient

**Steps**:

1. **Create Interceptor**:
```java
@Component
public class TrackingHttpClientInterceptor implements HttpRequestInterceptor {

    @Autowired
    private ProviderClassifier providerClassifier;

    @Autowired
    private CallCollector callCollector;

    @Override
    public void process(HttpRequest request, HttpContext context) {
        // Check if tracking
        if (!TrackingContext.isTracking()) {
            return;
        }

        // Extract host/path
        String host = request.getRequestLine().getUri();
        String provider = providerClassifier.classifyProvider(host, "");

        // Capture request/response
        // ...

        // Record call
        CallRecordInput input = CallRecordInput.builder()
            .provider(provider)
            .model("unknown")
            .latencyMs(latency)
            .status("success")
            .build();

        callCollector.recordCall(input);
    }
}
```

2. **Register Interceptor**:
```java
@Bean
public HttpClient httpClient(TrackingHttpClientInterceptor interceptor) {
    return HttpClients.custom()
        .addInterceptorFirst(interceptor)
        .build();
}
```

**Key Requirements**:
- Check `TrackingContext.isTracking()`
- Call `CallCollector.recordCall()`
- Link to current execution via `TrackingContext.getCurrentExecution()`

### How to Add New Metrics (e.g., Cost)

**Current State**: `cost` field exists but always `null`

**Why**: No pricing table implemented

**Steps to Implement**:

1. **Create Pricing Service**:
```java
@Service
public class PricingService {

    private final Map<String, ModelPricing> pricingTable = Map.of(
        "gpt-4-turbo", new ModelPricing(0.01, 0.03),  // input/output per 1K tokens
        "claude-3-opus", new ModelPricing(0.015, 0.075),
        // ...
    );

    public Double calculateCost(String model, int inputTokens, int outputTokens) {
        ModelPricing pricing = pricingTable.get(model);
        if (pricing == null) {
            return null;  // Unknown model
        }

        double inputCost = (inputTokens / 1000.0) * pricing.getInputPrice();
        double outputCost = (outputTokens / 1000.0) * pricing.getOutputPrice();

        return inputCost + outputCost;
    }
}
```

2. **Update Interceptors**:
```java
// In TrackingWebClientFilter.handleSuccessWithCapture()
UsageMetricsParser.ParsedUsageMetrics metrics = usageMetricsParser.parse(responsePreview, provider);

// Calculate cost
Double cost = pricingService.calculateCost(
    model,
    metrics.getInputTokens(),
    metrics.getOutputTokens()
);

CallRecordInput input = CallRecordInput.builder()
    .provider(provider)
    .model(model)
    .promptTokens(metrics.getInputTokens())
    .completionTokens(metrics.getOutputTokens())
    .cost(cost)  // Now populated!
    .build();
```

3. **Verify**:
- Check `calls.cost` column populated
- Check `executions.total_cost` aggregated correctly
- Dashboard shows costs

**Extensibility Point**: User can provide custom `PricingService` bean to override defaults

---

## 9. Architectural Strengths and Limitations

### Strengths (Well-Designed)

#### 1. **Non-Invasive Annotation-Based Tracking**
- **Why**: User only adds `@AIPrompt` annotation, no code changes
- **Benefit**: Works with any return type, any business logic
- **Example**:
  ```java
  @AIPrompt("my-func")
  public void myMethod() { ... }  // void return

  @AIPrompt("my-func")
  public Mono<String> myMethod() { ... }  // reactive return

  @AIPrompt("my-func")
  public CompletableFuture<String> myMethod() { ... }  // async return
  ```

#### 2. **Clean Separation of Concerns**
- **AOP**: Execution lifecycle
- **Interceptors**: HTTP call capture
- **Classifiers**: Provider detection
- **Collectors**: Persistence
- **API**: Query/aggregation
- Each layer has single responsibility

#### 3. **Automatic HTTP Client Support**
- **Zero Config**: WebClient, RestTemplate, RestClient auto-tracked
- **Spring AI Compatible**: Works seamlessly with Spring AI
- **Extensible**: OkHttp support available, can add more clients

#### 4. **Memory Safety**
- **Bounded Capture**: `max-in-memory-bytes` hard cap prevents OOM
- **Truncation Awareness**: `wasTruncated` flag signals partial data
- **ThreadLocal Cleanup**: `finally` block prevents memory leaks

#### 5. **Production-Safe Defaults**
- **Graceful Degradation**: Tracking failure doesn't break app
- **Configurable Capture**: Can disable request/response in prod
- **Provider Normalization**: Handles alias variations (openai, OpenAI, azureopenai)

#### 6. **Flexible Data Model**
- **1 Execution → N Calls**: Models real-world AI usage patterns
- **Function-Level Analytics**: Aggregates calls by business operation
- **Provider-Level Analytics**: Compare OpenAI vs Anthropic vs Google
- **Temporal Filtering**: Query by date range, environment

#### 7. **Comprehensive REST API**
- **Dashboard Summary**: High-level KPIs
- **Function Drill-Down**: Analyze specific operations
- **Execution Timeline**: Debug individual invocations
- **Call Explorer**: Search all HTTP calls

#### 8. **Extensibility**
- **Custom Providers**: `CustomProviderMatcher` interface
- **Custom Clients**: Follow interceptor pattern
- **Custom Metrics**: Inject pricing/cost logic

### Intentionally Simplified

#### 1. **No Built-In Cost Calculation**
- **Why**: Pricing varies by provider, tier, date
- **Trade-off**: Keeps library simple, but users must implement `PricingService`
- **Solution**: Extensibility point for custom cost logic

#### 2. **ThreadLocal for Context**
- **Why**: Simple, thread-safe, works for most use cases
- **Limitation**: Doesn't propagate across async boundaries
- **Trade-off**: Acceptable for synchronous AI calls (95% use case)

#### 3. **Immediate Call Persistence**
- **Why**: Ensures call data not lost if execution fails
- **Trade-off**: More DB writes (N calls + 1 execution) vs single batch insert
- **Benefit**: No risk of data loss, simpler transaction model

#### 4. **No Environment-Specific Behavior**
- **Why**: Keeps logic simple, user controls via config
- **Trade-off**: Must configure `store-raw-data` per environment
- **Benefit**: No magic behavior, explicit configuration

#### 5. **Best-Effort WebClient Request Capture**
- **Why**: WebClient request bodies are one-shot streams
- **Limitation**: Request capture returns empty string (comment in code)
- **Trade-off**: Response capture works, request capture TODO
- **Impact**: Can see response tokens/cost, but not request prompt

### Current Limitations (Not Implemented)

#### 1. **Cost Calculation**
- **Status**: `cost` field exists, always `null`
- **Why Not Implemented**: Requires pricing table maintenance
- **Plan**: Extensibility point ready, users can add `PricingService`

#### 2. **Async/Reactive Context Propagation**
- **Status**: ThreadLocal works for synchronous calls only
- **Why Not Implemented**: Requires Reactor Context integration
- **Plan**: Could refactor `TrackingContext` to use Reactor Context
- **Workaround**: Use `.block()` for synchronous consumption

#### 3. **WebClient Request Body Capture**
- **Status**: Returns empty string (see line 110 in `TrackingWebClientFilter`)
- **Why Not Implemented**: Requires complex `BodyInserter` wrapping
- **Plan**: TODO comment in code
- **Workaround**: Only response captured (usually sufficient for token counting)

#### 4. **Distributed Tracing Integration**
- **Status**: No correlation with Spring Cloud Sleuth/Micrometer Tracing
- **Why Not Implemented**: Scope limited to AI call tracking
- **Plan**: Could add `traceId` field to link with distributed traces

#### 5. **Rate Limiting / Circuit Breaker**
- **Status**: No built-in rate limiting
- **Why Not Implemented**: Orthogonal concern (use Resilience4j)
- **Plan**: User can wrap AI client with Resilience4j

#### 6. **Real-Time Metrics (Prometheus, etc.)**
- **Status**: Only database persistence
- **Why Not Implemented**: Keeps dependencies minimal
- **Plan**: Could add Micrometer metrics publisher

#### 7. **Elasticsearch/Kibana Integration**
- **Status**: PostgreSQL only
- **Why Not Implemented**: Keeps setup simple
- **Plan**: Could add async publisher to Elasticsearch

#### 8. **Multi-Tenancy**
- **Status**: No tenant isolation
- **Why Not Implemented**: Assumes single-tenant app
- **Plan**: Could add `tenant_id` field if needed

---

## Summary

**AI Prompt Tracker** is a **production-ready, minimally-invasive observability library** for tracking AI API usage in Spring Boot applications.

**Core Strengths**:
- Annotation-based (zero code changes)
- Supports multiple HTTP clients (auto-tracked)
- Memory-safe (bounded capture)
- Flexible data model (1 Execution → N Calls)
- Comprehensive REST API + Dashboard

**Intentional Simplifications**:
- No cost calculation (extensible)
- ThreadLocal context (works for 95% use cases)
- Immediate persistence (data safety > performance)

**Known Limitations**:
- WebClient request body capture (TODO)
- Async/reactive propagation (ThreadLocal limitation)
- Cost calculation (user-provided)

**Extensibility**:
- Custom providers via `CustomProviderMatcher`
- Custom clients via interceptor pattern
- Custom metrics via injected services

**Target Users**: Teams using AI APIs who need:
- Cost visibility
- Performance monitoring
- Debug capabilities
- Provider comparison
- Function-level analytics

**NOT Intended For**:
- Low-latency streaming AI (capture overhead)
- Multi-tenant SaaS (no tenant isolation)
- Real-time alerting (batch-oriented)
