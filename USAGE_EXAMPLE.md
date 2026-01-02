# Usage Examples

## Basic Setup

### 1. Add Dependency to Your Spring Boot Project

The AI Prompt Tracker is an observability library, not a client SDK.

```gradle
dependencies {
    // Your existing dependencies...
    implementation 'com.galoong:ai-prompt-tracker:1.0.0'
}
```

### 2. Configure Database

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ai_prompt_tracker
    username: postgres
    password: postgres

  jpa:
    hibernate:
      ddl-auto: validate

  flyway:
    enabled: true
```

### 3. Enable Tracking (Automatic)

Just add `@AIPrompt` to your methods - that's it!

---

## Example 1: Simple OpenAI Call

```java
@Service
@RequiredArgsConstructor
public class TranslationService {

    private final WebClient webClient;

    @AIPrompt(
        name = "translateText",
        description = "Translate text to another language",
        category = "translation"
    )
    public String translate(String text, String targetLanguage) {
        String response = webClient.post()
            .uri("https://api.openai.com/v1/chat/completions")
            .header("Authorization", "Bearer " + apiKey)
            .bodyValue(Map.of(
                "model", "gpt-4",
                "messages", List.of(
                    Map.of("role", "user", "content",
                        "Translate to " + targetLanguage + ": " + text)
                )
            ))
            .retrieve()
            .bodyToMono(String.class)
            .block();

        return extractTranslation(response);
    }
}
```

**What gets tracked:**
- 1 Execution: `translateText`
- 1 Call: OpenAI gpt-4
- Tokens, cost, latency automatically recorded

---

## Example 2: Multiple AI Calls (Chain of Thought)

```java
@Service
public class ContentAnalyzer {

    @Autowired
    private WebClient webClient;

    @AIPrompt(
        name = "analyzeContent",
        description = "Multi-step content analysis",
        category = "nlp",
        tags = {"analysis", "summarization"}
    )
    public AnalysisResult analyze(String content) {
        // Step 1: Extract key points (OpenAI)
        String keyPoints = extractKeyPoints(content);

        // Step 2: Summarize (OpenAI)
        String summary = summarize(keyPoints);

        // Step 3: Verify summary quality (Claude)
        boolean isGood = verifyQuality(summary);

        // Step 4: If not good, regenerate with different model (Gemini)
        if (!isGood) {
            summary = regenerateSummary(keyPoints);
        }

        return new AnalysisResult(summary, keyPoints);
    }

    private String extractKeyPoints(String content) {
        return callOpenAI("gpt-4", "Extract key points: " + content);
    }

    private String summarize(String keyPoints) {
        return callOpenAI("gpt-3.5-turbo", "Summarize: " + keyPoints);
    }

    private boolean verifyQuality(String summary) {
        String result = callClaude("claude-3-haiku",
            "Is this summary good? " + summary);
        return result.contains("yes");
    }

    private String regenerateSummary(String keyPoints) {
        return callGemini("gemini-pro", "Summarize: " + keyPoints);
    }
}
```

**What gets tracked:**
- 1 Execution: `analyzeContent`
- 4-5 Calls depending on quality check:
  - Call 1: OpenAI gpt-4 (extract)
  - Call 2: OpenAI gpt-3.5-turbo (summarize)
  - Call 3: Anthropic claude-3-haiku (verify)
  - Call 4 (conditional): Google gemini-pro (regenerate)
- Total cost = sum of all calls
- Total tokens = sum of all calls

---

## Example 3: Using Injected WebClient

```java
@Configuration
public class MyConfig {

    @Bean
    public WebClient myCustomWebClient(WebClient.Builder builder) {
        // The builder already has TrackingWebClientFilter!
        return builder
            .baseUrl("https://api.openai.com")
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .build();
    }
}

@Service
public class MyService {

    @Autowired
    private WebClient myCustomWebClient;  // Automatically tracked!

    @AIPrompt(name = "generateIdeas")
    public List<String> generateIdeas(String topic) {
        String response = myCustomWebClient.post()
            .uri("/v1/chat/completions")
            .bodyValue(createRequest(topic))
            .retrieve()
            .bodyToMono(String.class)
            .block();

        return parseIdeas(response);
    }
}
```

---

## Example 4: Mixed Providers

```java
@Service
public class SmartAssistant {

    @AIPrompt(
        name = "smartAnswer",
        description = "Uses multiple AI providers for best answer",
        category = "assistant"
    )
    public String answerQuestion(String question) {
        // Get 3 different perspectives
        String openaiAnswer = askOpenAI(question);
        String claudeAnswer = askClaude(question);
        String geminiAnswer = askGemini(question);

        // Use Claude to synthesize the best answer
        String finalAnswer = synthesize(
            openaiAnswer, claudeAnswer, geminiAnswer);

        return finalAnswer;
    }

    private String askOpenAI(String q) {
        return webClient.post()
            .uri("https://api.openai.com/v1/chat/completions")
            .bodyValue(...)
            .retrieve().bodyToMono(String.class).block();
    }

    private String askClaude(String q) {
        return webClient.post()
            .uri("https://api.anthropic.com/v1/messages")
            .bodyValue(...)
            .retrieve().bodyToMono(String.class).block();
    }

    private String askGemini(String q) {
        return webClient.post()
            .uri("https://generativelanguage.googleapis.com/v1/models/gemini-pro:generateContent")
            .bodyValue(...)
            .retrieve().bodyToMono(String.class).block();
    }

    private String synthesize(String a, String b, String c) {
        return askClaude("Synthesize these answers: " + a + b + c);
    }
}
```

**What gets tracked:**
- 1 Execution: `smartAnswer`
- 5 Calls:
  - OpenAI (answer)
  - Anthropic (answer)
  - Google (answer)
  - Anthropic (synthesize)
  - Anthropic (verify - if quality check added)

---

## Querying Tracked Data

### Get all executions for a function
```java
@Repository
public interface ExecutionRepository extends JpaRepository<ExecutionRecord, String> {
    List<ExecutionRecord> findByFunctionNameOrderByStartedAtDesc(String functionName);
}

// Usage
List<ExecutionRecord> executions = repo.findByFunctionNameOrderByStartedAtDesc("analyzeContent");
```

### Get all calls in an execution
```java
@Repository
public interface CallRepository extends JpaRepository<CallRecord, String> {
    List<CallRecord> findByExecutionIdOrderByCreatedAtAsc(String executionId);
}

// Usage
List<CallRecord> calls = callRepo.findByExecutionIdOrderByCreatedAtAsc(execution.getId());
```

### Calculate total cost
```java
Double totalCost = executionRepo.findAll().stream()
    .map(ExecutionRecord::getTotalCost)
    .filter(Objects::nonNull)
    .reduce(0.0, Double::sum);
```

---

## Advanced: Elasticsearch Profile

```bash
# Start Elasticsearch
docker-compose --profile advanced up -d elasticsearch

# Run app with advanced profile
./gradlew bootRun -Dspring.profiles.active=advanced
```

```yaml
# application-advanced.yml
ai-prompts:
  advanced:
    similarity-detection:
      enabled: true
      threshold: 0.85
```

---

## Environment Variables

```bash
# Optional: Store raw request/response in dev
export AI_PROMPTS_STORE_RAW=true

# Database
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=ai_prompt_tracker
export DB_USER=postgres
export DB_PASSWORD=postgres
```

---

## Best Practices

### 1. Use Meaningful Function Names
```java
// Good
@AIPrompt(name = "extractJapaneseWords", description = "...")

// Bad
@AIPrompt(name = "process", description = "...")
```

### 2. Add Categories and Tags
```java
@AIPrompt(
    name = "analyzeCustomerFeedback",
    category = "customer-service",
    tags = {"sentiment", "classification", "feedback"}
)
```

### 3. Return Your Own Types (Not Forced!)
```java
// ✅ Good - Return your domain model
@AIPrompt(name = "...")
public CustomerAnalysis analyze(String feedback) { ... }

// ✅ Also good - Return primitive
@AIPrompt(name = "...")
public String translate(String text) { ... }

// ✅ Also good - Return void
@AIPrompt(name = "...")
public void processAsync(String data) { ... }
```

### 4. Don't Track Non-AI Methods
```java
// ❌ Don't do this
@AIPrompt(name = "calculateSum")  // No AI calls!
public int sum(int a, int b) {
    return a + b;
}
```

---

## Debugging

### Enable Debug Logging
```yaml
logging:
  level:
    com.galoong.aiprompttracker: DEBUG
```

### Check Tracked Data
```sql
-- Recent executions
SELECT function_name, status, calls_count, total_cost, started_at
FROM executions
ORDER BY started_at DESC
LIMIT 10;

-- Recent calls
SELECT provider, model, total_tokens, cost, latency_ms
FROM calls
ORDER BY created_at DESC
LIMIT 20;
```

---

## Common Issues

### Issue: Calls not being tracked
**Solution:** Make sure you're using the injected `WebClient` bean with tracking filter.

### Issue: No cost calculated
**Solution:** Cost calculation requires pricing models (TODO - not yet implemented).

### Issue: Elasticsearch health check fails
**Solution:** Elasticsearch is disabled by default. Only enable in advanced profile.

---

## Next Steps

1. Add `@AIPrompt` to your AI-calling methods
2. Run your application
3. Query the database to see tracked executions and calls
4. (Future) Use the Dashboard UI for visualizations
