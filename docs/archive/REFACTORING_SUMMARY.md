# Architecture Refactoring Summary

## From SDK to Observability Tool

The project has been refactored from an AI SDK (that calls AI APIs directly) to an **Observability/Monitoring Tool** that tracks how users call AI APIs in their own code.

---

## Key Architectural Changes

### 1. Core Concept Shift

**Before (SDK approach):**
- Users forced to return `AIProviderResponse`
- Project calls AI APIs through providers (OpenAIProvider, ClaudeProvider)
- One method = one AI call

**After (Observability approach):**
- Users can return ANY type
- Users call AI APIs however they want (WebClient, RestTemplate, OkHttp, SpringAI)
- One method execution = Multiple AI calls
- `1 Execution → N Calls` relationship

### 2. New Data Model

#### Execution (Semantic Unit)
```
ExecutionRecord
  ├─ id: UUID
  ├─ functionName: String (from @AIPrompt)
  ├─ category: String
  ├─ tags: String[]
  ├─ environment: String (dev/test/prod/advanced)
  ├─ startedAt/finishedAt: Instant
  ├─ status: "success" or "error"
  └─ aggregated: callsCount, totalCost, totalTokens
```

#### Call (Individual HTTP Request)
```
CallRecord
  ├─ id: UUID
  ├─ executionId: UUID (FK to executions)
  ├─ provider: String (OpenAI, Anthropic, Google)
  ├─ model: String (gpt-4, claude-3, gemini-pro)
  ├─ tokens: promptTokens, completionTokens, totalTokens
  ├─ cost: Double
  ├─ latencyMs: Long
  ├─ status: "success" or "error"
  └─ debug data (requestPreview, responsePreview, rawJson)
```

### 3. Tracking Flow

```
User's Code:
  @AIPrompt(name="extractWords")
  public MyResult analyze(String text) {
    call OpenAI 3 times
    call Claude 1 time
    return myCustomResult
  }

Tracking System:
  ┌─ AIPromptAspect (AOP)
  │   └─ Creates ExecutionContext (ThreadLocal)
  │
  ├─ TrackingWebClientFilter (HTTP Interceptor)
  │   ├─ Detects AI provider (ProviderClassifier)
  │   ├─ Extracts model (ModelExtractor)
  │   ├─ Parses usage metrics (UsageMetricsParser)
  │   └─ Records call (CallCollector)
  │
  └─ Result:
      Execution(id=exec-1, functionName="extractWords")
        ├─ Call(provider=OpenAI, model=gpt-4, tokens=100)
        ├─ Call(provider=OpenAI, model=gpt-4, tokens=80)
        ├─ Call(provider=OpenAI, model=gpt-3.5, tokens=50)
        └─ Call(provider=Anthropic, model=claude-3, tokens=120)
```

---

## Files Changed/Created

### Deleted Files
```
❌ backend/src/main/java/com/galoong/aiprompttracker/providers/
   - OpenAIProvider.java
   - OpenAIResponse.java
   - OpenAIPricingLoader.java

❌ backend/src/main/java/com/galoong/aiprompttracker/core/provider/
   - AIProvider.java
   - AIProviderResponse.java
   - UsageMetrics.java
   - PricingModel.java

❌ backend/src/main/java/com/galoong/aiprompttracker/domain/entity/
   - AICallRecord.java
   - DailyStats.java

❌ backend/src/main/java/com/galoong/aiprompttracker/tracking/
   - detector/AIProviderDetector.java
   - service/TrackingService.java
```

### New Core Components

#### Tracking Context (ThreadLocal)
```
✅ tracking/context/ExecutionContext.java
✅ tracking/context/CallRecordData.java
✅ tracking/context/TrackingContext.java (static ThreadLocal manager)
```

#### Call Collection
```
✅ tracking/collector/CallRecordInput.java (DTO)
✅ tracking/collector/CallCollector.java (interface)
✅ tracking/collector/DefaultCallCollector.java (implementation)
```

#### HTTP Interception
```
✅ tracking/interceptor/TrackingWebClientFilter.java (WebClient filter)
✅ tracking/interceptor/UsageMetricsParser.java (response parser)
✅ tracking/classifier/ProviderClassifier.java (host → provider)
✅ tracking/classifier/ModelExtractor.java (JSON → model name)
```

#### Database
```
✅ domain/entity/ExecutionRecord.java (new)
✅ domain/entity/CallRecord.java (new)
✅ domain/repository/ExecutionRepository.java (new)
✅ domain/repository/CallRepository.java (new)
```

#### Configuration
```
✅ config/WebClientConfig.java (provides tracked WebClient bean)
```

### Modified Files

#### AOP Aspect (Major Refactor)
```
✅ tracking/aspect/AIPromptAspect.java
   - Removed: AIProviderResponse return type dependency
   - Added: ExecutionContext creation
   - Added: ThreadLocal context management
   - Now works with ANY return type
```

#### Annotation
```
✅ core/annotation/AIPrompt.java
   - Added: name() field
   - Removed: provider(), model() (not needed)
   - Simplified: Removed SDK-related fields
```

#### Database Schema
```
✅ db/migration/V3__refactor_to_execution_call.sql
   - Drops: ai_call_records, daily_stats
   - Creates: executions, calls
   - Adds: proper foreign keys and indexes
```

#### Application Configuration
```
✅ application.yml
   - Removed: ai.providers.* config
   - Added: ai-prompts.tracking.store-raw-data
   - Added: management.health.elasticsearch.enabled = false

✅ application-dev.yml
   - Added: store-raw-data = true

✅ application-test.yml
   - Added: store-raw-data = true

✅ application-advanced.yml (NEW)
   - Enables Elasticsearch
   - Configures advanced features
```

---

## Profile Configuration

### Default Profiles (dev, test, prod)
- ✅ Elasticsearch health check **disabled**
- ✅ No ES connection required
- ✅ Basic tracking only (Executions + Calls)

### Advanced Profile
- ✅ Elasticsearch health check **enabled**
- ✅ Connects to Elasticsearch
- ✅ Advanced features (similarity detection, analytics)

```bash
# Run with default profile (no ES)
./gradlew bootRun

# Run with advanced profile (requires ES)
./gradlew bootRun -Dspring.profiles.active=advanced
```

---

## Usage Example

### Before (SDK approach)
```java
@Service
public class MyService {
    @Autowired
    private OpenAIProvider openAIProvider;  // ❌ Forced dependency

    @AIPrompt(provider="OpenAI", model="gpt-4")
    public AIProviderResponse analyze(String text) {  // ❌ Forced return type
        return openAIProvider.execute("gpt-4", text, params);
    }
}
```

### After (Observability approach)
```java
@Service
public class MyService {
    @Autowired
    private WebClient webClient;  // ✅ User's own WebClient

    @AIPrompt(
        name = "extractWords",
        description = "Extract Japanese words",
        category = "nlp"
    )
    public MyCustomResult analyze(String subtitle) {  // ✅ ANY return type
        // User calls AI however they want
        String result1 = callOpenAI(subtitle);
        String result2 = callOpenAI(result1);
        String result3 = callClaude(result2);

        // All 3 calls are automatically tracked!
        return new MyCustomResult(result3);
    }

    private String callOpenAI(String input) {
        return webClient.post()
            .uri("https://api.openai.com/v1/chat/completions")
            .bodyValue(Map.of("model", "gpt-4", "messages", ...))
            .retrieve()
            .bodyToMono(String.class)
            .block();
    }
}
```

**What gets tracked:**
- 1 Execution: `extractWords`
- 3 Calls: 2 × OpenAI + 1 × Claude
- All tokens, costs, latencies automatically recorded

---

## Database Queries

### Find all calls in an execution
```sql
SELECT * FROM calls WHERE execution_id = 'exec-uuid';
```

### Total cost by function
```sql
SELECT function_name, SUM(total_cost)
FROM executions
WHERE started_at >= NOW() - INTERVAL '7 days'
GROUP BY function_name
ORDER BY SUM(total_cost) DESC;
```

### Provider usage breakdown
```sql
SELECT provider, COUNT(*), SUM(cost)
FROM calls
WHERE created_at >= NOW() - INTERVAL '7 days'
GROUP BY provider;
```

---

## Future Work (TODO Comments Added)

### Additional Interceptors
```java
// TODO: Implement RestTemplateInterceptor
// TODO: Implement OkHttpInterceptor
// TODO: Implement SpringAI observability integration
```

### Async Support
```java
// TODO: Async/Reactive context propagation (Context.Reactor)
// For reactive streams and async executions
```

### Cost Calculation
```java
// TODO: Implement automatic cost calculation based on pricing models
// Currently returns null, needs pricing database
```

---

## Benefits

### For Users
- ✅ No forced dependencies (use any HTTP client)
- ✅ No forced return types (use your own models)
- ✅ No code changes (just add @AIPrompt)
- ✅ Automatic tracking (all AI calls captured)

### For Observability
- ✅ 1 Execution → N Calls relationship
- ✅ Tracks actual HTTP requests
- ✅ Provider-agnostic (works with any AI provider)
- ✅ Detailed metrics (tokens, cost, latency)
- ✅ Debug support (raw request/response in dev/test)

### For Analytics
- ✅ Function-level insights
- ✅ Provider comparison
- ✅ Cost breakdown
- ✅ Performance monitoring
- ✅ Error tracking

---

## Migration from Old Code

If you had code using the old SDK approach:

1. **Remove** `implements AIProvider`
2. **Remove** return type restrictions
3. **Keep** `@AIPrompt` annotation
4. **Update** to use `name` instead of `provider/model`
5. **Use** your own HTTP client (WebClient, etc.)

Old migrations (V1, V2) are dropped by V3 migration.

---

## Project Status

- ✅ Core tracking infrastructure complete
- ✅ WebClient interceptor implemented
- ✅ Database schema migrated
- ✅ Profile separation (default vs advanced)
- ✅ Elasticsearch health check properly configured
- ⏳ RestTemplate/OkHttp interceptors (TODO)
- ⏳ Cost calculation (TODO)
- ⏳ Dashboard API (future)
- ⏳ Frontend UI (future)
