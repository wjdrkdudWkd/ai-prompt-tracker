# AI Prompt Tracker - Integrations Guide

## Overview

AI Prompt Tracker supports automatic tracking across multiple HTTP clients and AI SDKs. The tracker is **plug-and-play** - just annotate your methods with `@AIPrompt` and calls are automatically captured.

**Supported Clients:**
- ✅ Spring WebClient (reactive)
- ✅ Spring RestTemplate (blocking)
- ✅ Spring RestClient (Spring Boot 3)
- ✅ OkHttp
- ✅ Spring AI (ChatClient, OpenAiChatModel, AnthropicChatModel, etc.)
- ✅ Any HTTP client (with manual adapter)

---

## Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>com.galoong</groupId>
    <artifactId>ai-prompt-tracker</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. Configure Properties

```yaml
ai-prompts:
  tracking:
    store-raw-data: true
    max-request-bytes: 16384    # 16KB
    max-response-bytes: 32768   # 32KB
    max-in-memory-bytes: 2097152 # 2MB hard cap
    capture-content-types:
      - application/json
    truncation-suffix: " ... (truncated)"
```

### 3. Annotate Your Methods

```java
@Service
public class MyAIService {

    @AIPrompt(name = "generate-summary", category = "reporting")
    public String generateSummary(String content) {
        // Any AI API calls here are automatically tracked
        // Works with WebClient, RestTemplate, RestClient, OkHttp, Spring AI
        return chatClient.call(content);
    }
}
```

That's it! Tracking works automatically for all supported clients.

---

## WebClient Integration

### Auto-Configuration (Zero Code)

WebClient tracking is **automatically enabled** via `WebClientCustomizer`.

```java
@Service
public class OpenAIService {

    private final WebClient webClient;

    public OpenAIService(WebClient.Builder builder) {
        // Filter is automatically applied!
        this.webClient = builder.baseUrl("https://api.openai.com").build();
    }

    @AIPrompt(name = "chat-completion")
    public Mono<String> chat(String prompt) {
        return webClient.post()
                .uri("/v1/chat/completions")
                .bodyValue(Map.of("model", "gpt-4", "messages", List.of(...)))
                .retrieve()
                .bodyToMono(String.class);
        // Automatically tracked!
    }
}
```

### Manual Setup (Optional)

If you create WebClient without dependency injection:

```java
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient customWebClient(TrackingWebClientFilter trackingFilter) {
        return WebClient.builder()
                .filter(trackingFilter) // Add filter manually
                .build();
    }
}
```

---

## Spring AI Integration

### Auto-Configuration (Zero Code)

Spring AI uses WebClient under the hood, so tracking **works automatically** once you include our dependency.

```java
@Service
public class ChatService {

    private final ChatClient chatClient;

    public ChatService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @AIPrompt(name = "customer-support", category = "chat")
    public String handleQuestion(String question) {
        // Automatically tracked!
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }
}
```

### With OpenAI ChatModel

```java
@Service
public class OpenAIService {

    private final OpenAiChatModel chatModel;

    public OpenAIService(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @AIPrompt(name = "openai-completion")
    public String complete(String prompt) {
        // Automatically tracked!
        return chatModel.call(prompt);
    }
}
```

### With Anthropic ChatModel

```java
@Service
public class AnthropicService {

    private final AnthropicChatModel chatModel;

    public AnthropicService(AnthropicChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @AIPrompt(name = "claude-analysis", category = "analysis")
    public String analyze(String text) {
        // Automatically tracked!
        return chatModel.call(text);
    }
}
```

**How it works:**
- Spring AI internally uses `WebClient` (or `RestClient`) to call AI APIs
- Our `WebClientCustomizer` ensures the tracking filter is applied globally
- All calls inside `@AIPrompt` methods are captured automatically

**No code changes needed!**

---

## RestTemplate Integration

### Auto-Configuration (Zero Code)

RestTemplate tracking is **automatically enabled** via `RestTemplateCustomizer`.

```java
@Service
public class LegacyAIService {

    private final RestTemplate restTemplate;

    public LegacyAIService(RestTemplateBuilder builder) {
        // Interceptor is automatically applied!
        this.restTemplate = builder.build();
    }

    @AIPrompt(name = "legacy-completion")
    public String complete(String prompt) {
        Map<String, Object> request = Map.of("model", "gpt-4", "messages", List.of(...));

        ResponseEntity<String> response = restTemplate.postForEntity(
                "https://api.openai.com/v1/chat/completions",
                request,
                String.class
        );
        // Automatically tracked!
        return response.getBody();
    }
}
```

### Manual Setup (Optional)

If you create RestTemplate without `RestTemplateBuilder`:

```java
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(TrackingRestTemplateInterceptor trackingInterceptor) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(trackingInterceptor);
        return restTemplate;
    }
}
```

---

## RestClient Integration (Spring Boot 3)

### Auto-Configuration (Zero Code)

RestClient tracking is **automatically enabled** via `RestClientCustomizer`.

```java
@Service
public class ModernAIService {

    private final RestClient restClient;

    public ModernAIService(RestClient.Builder builder) {
        // Interceptor is automatically applied!
        this.restClient = builder
                .baseUrl("https://api.anthropic.com")
                .defaultHeader("x-api-key", apiKey)
                .build();
    }

    @AIPrompt(name = "claude-completion")
    public String complete(String prompt) {
        return restClient.post()
                .uri("/v1/messages")
                .body(Map.of("model", "claude-3-opus", "messages", List.of(...)))
                .retrieve()
                .body(String.class);
        // Automatically tracked!
    }
}
```

### Manual Setup (Optional)

```java
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient(TrackingRestTemplateInterceptor trackingInterceptor) {
        return RestClient.builder()
                .requestInterceptor(trackingInterceptor)
                .build();
    }
}
```

---

## OkHttp Integration

### Manual Setup (Required)

OkHttp requires manual interceptor registration:

```java
@Configuration
public class OkHttpConfig {

    @Bean
    public OkHttpClient okHttpClient(TrackingOkHttpInterceptor trackingInterceptor) {
        return new OkHttpClient.Builder()
                .addInterceptor(trackingInterceptor)
                .build();
    }
}
```

### Usage

```java
@Service
public class OkHttpAIService {

    private final OkHttpClient client;

    public OkHttpAIService(OkHttpClient client) {
        this.client = client;
    }

    @AIPrompt(name = "okhttp-completion")
    public String complete(String prompt) throws IOException {
        RequestBody body = RequestBody.create(
                "{\"model\":\"gpt-4\",\"messages\":[...]}",
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            // Automatically tracked!
            return response.body().string();
        }
    }
}
```

---

## Custom Provider Support

### Adding Custom AI Providers

Implement `CustomProviderMatcher` to add support for additional AI providers:

```java
@Component
public class MyCustomProviderMatcher implements CustomProviderMatcher {

    @Override
    public String matchProvider(String host, String path) {
        if (host.contains("api.mycustom.ai")) {
            return "MyCustomAI";
        }
        if (host.contains("api.another.ai")) {
            return "AnotherAI";
        }
        return null; // No match
    }

    @Override
    public int getOrder() {
        return 0; // Higher priority than built-in matchers (default: 100)
    }
}
```

### Custom Usage Metrics Parser

Extend `UsageMetricsParser` to parse custom provider response formats:

```java
@Component
public class CustomMetricsParser {

    @Autowired
    private UsageMetricsParser usageMetricsParser;

    // Register custom parser logic by extending UsageMetricsParser
    // or implementing your own provider-specific parsing
}
```

---

## Any SDK Support

### Best-Effort HTTP Layer Capture

Most AI SDKs use HTTP clients under the hood. If the SDK uses one of our supported clients, tracking works automatically.

**Example with unofficial SDK:**

```java
// If SDK uses WebClient, RestTemplate, RestClient, or OkHttp internally:
@AIPrompt(name = "custom-sdk-call")
public String callCustomSDK(String input) {
    // If the SDK uses supported HTTP client, it's tracked automatically!
    return customAiSdk.generate(input);
}
```

### Manual Adapter (Fallback)

If the SDK doesn't expose HTTP layer or uses an unsupported client, manually record calls:

```java
@Service
public class ManualAdapterService {

    @Autowired
    private CallCollector callCollector;

    @AIPrompt(name = "manual-sdk-call")
    public String callProprietarySDK(String input) {
        long startTime = System.currentTimeMillis();

        try {
            String result = proprietaryAiSdk.call(input);
            long latency = System.currentTimeMillis() - startTime;

            // Manually record the call
            callCollector.recordCall(CallRecordInput.builder()
                    .provider("ProprietaryAI")
                    .model("custom-model")
                    .latencyMs(latency)
                    .status("success")
                    // Optionally add tokens/cost if SDK provides them
                    .build());

            return result;
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;

            callCollector.recordCall(CallRecordInput.builder()
                    .provider("ProprietaryAI")
                    .model("custom-model")
                    .latencyMs(latency)
                    .status("error")
                    .errorType(e.getClass().getSimpleName())
                    .errorMessage(e.getMessage())
                    .build());

            throw e;
        }
    }
}
```

---

## Configuration Reference

### Full Properties

```yaml
ai-prompts:
  tracking:
    # Store raw request/response bodies (default: true)
    store-raw-data: true

    # Maximum bytes to capture from request body (default: 16KB)
    max-request-bytes: 16384

    # Maximum bytes to capture from response body (default: 32KB)
    max-response-bytes: 32768

    # Hard cap for in-memory buffering (default: 2MB)
    max-in-memory-bytes: 2097152

    # Content types to capture (default: [application/json])
    capture-content-types:
      - application/json
      - text/plain

    # Capture calls to unknown providers (default: false)
    capture-unknown-providers: false

    # Truncation suffix (default: " ... (truncated)")
    truncation-suffix: " ... (truncated)"
```

### Environment-Specific Configuration

```yaml
# Development (capture everything)
---
spring:
  config:
    activate:
      on-profile: dev

ai-prompts:
  tracking:
    store-raw-data: true
    capture-unknown-providers: true

# Production (metadata only for privacy)
---
spring:
  config:
    activate:
      on-profile: prod

ai-prompts:
  tracking:
    store-raw-data: false  # Don't store request/response bodies
    capture-unknown-providers: false
```

---

## Troubleshooting

### Tracking Not Working?

**Check these:**

1. **Is the method annotated with `@AIPrompt`?**
   ```java
   @AIPrompt(name = "my-function")  // Required!
   public String myMethod() { ... }
   ```

2. **Is the HTTP client configured correctly?**
   - WebClient/RestTemplate/RestClient: Should use Spring's builders (auto-configured)
   - OkHttp: Must manually add `TrackingOkHttpInterceptor`

3. **Check logs for interceptor registration:**
   ```
   INFO: Registering TrackingWebClientFilter for WebClient support
   INFO: Registering RestTemplateCustomizer to auto-apply tracking interceptor
   ```

4. **Is the provider recognized?**
   - Built-in: OpenAI, Anthropic, Google, Cohere, Mistral, HuggingFace, Perplexity
   - Unknown providers: Set `capture-unknown-providers: true` to track all

5. **Check database:**
   ```sql
   SELECT * FROM executions ORDER BY started_at DESC LIMIT 10;
   SELECT * FROM calls ORDER BY created_at DESC LIMIT 10;
   ```

### Performance Concerns?

**Disable body capture in production:**

```yaml
ai-prompts:
  tracking:
    store-raw-data: false  # Only track metadata
```

This reduces memory usage and database storage by ~90%.

---

## Examples by Framework

### Spring Boot 3 + Spring AI + WebClient

```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

@Service
public class ChatService {

    @Autowired
    private ChatClient chatClient;

    @AIPrompt(name = "customer-chat", category = "support")
    public String chat(String message) {
        return chatClient.prompt().user(message).call().content();
        // Automatically tracked! No configuration needed.
    }
}
```

### Spring Boot 3 + OkHttp

```java
@Configuration
public class OkHttpConfig {

    @Bean
    public OkHttpClient okHttpClient(TrackingOkHttpInterceptor interceptor) {
        return new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .build();
    }
}

@Service
public class AIService {

    @Autowired
    private OkHttpClient client;

    @AIPrompt(name = "okhttp-ai-call")
    public String callAI(String prompt) throws IOException {
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .post(RequestBody.create(...))
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
        // Tracked automatically!
    }
}
```

### Legacy Spring Boot + RestTemplate

```java
@Service
public class LegacyService {

    @Autowired
    private RestTemplate restTemplate;

    @AIPrompt(name = "legacy-completion")
    public String complete(String prompt) {
        return restTemplate.postForObject(
                "https://api.openai.com/v1/chat/completions",
                request,
                String.class
        );
        // Tracked automatically!
    }
}
```

---

## Summary

| Client | Auto-Config | Manual Setup | Spring AI Support |
|--------|-------------|--------------|-------------------|
| **WebClient** | ✅ Yes | Optional | ✅ Yes (primary) |
| **RestTemplate** | ✅ Yes | Optional | ✅ Yes (fallback) |
| **RestClient** | ✅ Yes | Optional | ✅ Yes (Spring Boot 3) |
| **OkHttp** | ❌ No | Required | ⚠️ If SDK uses it |
| **Custom SDK** | ⚠️ Maybe | Manual `CallCollector` | Depends on HTTP client |

**Recommendation:** Use Spring AI with WebClient for best out-of-the-box experience.

---

## Next Steps

1. ✅ Add `@AIPrompt` to your AI-calling methods
2. ✅ Configure properties in `application.yml`
3. ✅ View metrics in dashboard: `GET /api/dashboard/summary`
4. ✅ Query executions: `GET /api/executions/{id}`
5. ✅ Analyze costs and performance

For more details, see the main [README](README.md) and [API Documentation](DASHBOARD_API_IMPLEMENTATION.md).
