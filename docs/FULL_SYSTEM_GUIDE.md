# AI Prompt Tracker - Full System Guide

## Overview

AI Prompt Tracker is a comprehensive observability platform for monitoring AI API usage across Spring Boot applications. This guide covers the complete system: backend library + frontend dashboard.

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                   │
│                                                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │           @AIPrompt Annotated Methods                 │  │
│  │  ┌────────────────────────────────────────────────┐  │  │
│  │  │  @AIPrompt(name = "generate-summary")          │  │  │
│  │  │  public String generateSummary(String input) { │  │  │
│  │  │      return chatClient.call(input);            │  │  │
│  │  │  }                                              │  │  │
│  │  └────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────┘  │
│                           ↓                                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │          AI Prompt Tracker Library (Backend)          │  │
│  │                                                        │  │
│  │  • AOP Interceptor (@AIPrompt)                        │  │
│  │  • HTTP Client Interceptors:                          │  │
│  │    - WebClient Filter                                 │  │
│  │    - RestTemplate Interceptor                         │  │
│  │    - RestClient Customizer                            │  │
│  │    - OkHttp Interceptor                               │  │
│  │  • Provider Classifier (OpenAI, Anthropic, etc.)      │  │
│  │  • Usage Metrics Parser                               │  │
│  │  • Call Collector + Execution Tracker                 │  │
│  └──────────────────────────────────────────────────────┘  │
│                           ↓                                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              PostgreSQL Database                      │  │
│  │                                                        │  │
│  │  • executions table (1 per @AIPrompt call)            │  │
│  │  • calls table (N per execution)                      │  │
│  │  • Elasticsearch sync (optional)                      │  │
│  └──────────────────────────────────────────────────────┘  │
│                           ↓                                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Dashboard REST APIs                      │  │
│  │                                                        │  │
│  │  GET /api/dashboard/summary                           │  │
│  │  GET /api/functions                                   │  │
│  │  GET /api/functions/{name}                            │  │
│  │  GET /api/functions/{name}/executions                 │  │
│  │  GET /api/executions/{id}                             │  │
│  │  GET /api/calls                                       │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────┐
│                   Frontend Dashboard (Next.js)               │
│                                                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                  React Query Client                   │  │
│  │  • Automatic caching                                  │  │
│  │  • Refetching on window focus                        │  │
│  │  • Loading/error states                              │  │
│  └──────────────────────────────────────────────────────┘  │
│                           ↓                                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                    UI Pages                           │  │
│  │                                                        │  │
│  │  • Dashboard (KPI cards + functions table)            │  │
│  │  • Functions List                                     │  │
│  │  • Function Detail (KPIs + provider breakdown)        │  │
│  │  • Execution Drawer (call timeline)                   │  │
│  │  • Calls Explorer                                     │  │
│  │  • Providers Comparison                               │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## Quick Start (5 Minutes)

### 1. Start Backend

```bash
cd backend
./gradlew bootRun
```

Backend runs on [http://localhost:8080](http://localhost:8080)

### 2. Start Frontend

```bash
cd frontend
npm install
echo "NEXT_PUBLIC_API_BASE_URL=http://localhost:8080" > .env.local
npm run dev
```

Frontend runs on [http://localhost:3000](http://localhost:3000)

### 3. Open Dashboard

Navigate to [http://localhost:3000/dashboard](http://localhost:3000/dashboard)

---

## Backend Setup

### Add Dependency

```xml
<dependency>
    <groupId>com.galoong</groupId>
    <artifactId>ai-prompt-tracker</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Configure Properties

```yaml
ai-prompts:
  tracking:
    store-raw-data: true
    max-request-bytes: 16384
    max-response-bytes: 32768
    capture-content-types:
      - application/json
```

### Annotate Methods

```java
@Service
public class MyAIService {

    @AIPrompt(name = "generate-summary", category = "reporting")
    public String generateSummary(String content) {
        // Any AI API calls here are automatically tracked
        return chatClient.call(content);
    }
}
```

**Supported HTTP Clients**:
- ✅ Spring WebClient (auto-tracked)
- ✅ Spring RestTemplate (auto-tracked)
- ✅ Spring RestClient (auto-tracked)
- ✅ Spring AI (auto-tracked via underlying HTTP clients)
- ✅ OkHttp (manual interceptor registration)

For details, see [INTEGRATIONS_GUIDE.md](INTEGRATIONS_GUIDE.md)

---

## Frontend Setup

### Install Dependencies

```bash
cd frontend
npm install
```

### Configure Backend URL

Create `frontend/.env.local`:

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

### Run Development Server

```bash
npm run dev
```

For details, see [FRONTEND_SETUP_GUIDE.md](FRONTEND_SETUP_GUIDE.md)

---

## Dashboard Features

### 1. Dashboard Page (`/dashboard`)

**What it shows**:
- Total cost across all executions
- Total executions and AI calls
- Average calls per execution
- Average latency
- Error rate
- Functions table with drill-down

**Use case**: Get a quick overview of AI usage across your entire system

---

### 2. Functions List (`/functions`)

**What it shows**:
- All AI-integrated functions
- Per-function metrics (executions, calls, cost, avg time, error %)

**Use case**: Identify which functions are most expensive or error-prone

---

### 3. Function Detail (`/functions/{functionName}`)

**What it shows**:
- Function-level KPI cards
- Top providers used by this function
- Top models used by this function
- Recent execution history

**Use case**: Analyze a specific function's AI usage patterns

**Interaction**: Click execution row → Opens Execution Drawer

---

### 4. Execution Drawer (Critical Component)

**What it shows**:
- Execution summary (duration, status, total cost, total tokens)
- Call timeline (chronological list of AI API calls)
- Per-call details (provider, model, tokens, latency, cost)
- Request/response previews (expandable)
- wasTruncated badge (if response was truncated)

**Use case**: Debug individual executions, see exact AI calls made

**Interaction**: Click call → Expand to see request/response JSON

---

### 5. Calls Explorer (`/calls`)

**What it shows**:
- All AI API calls across the system
- Provider, model, status, tokens, latency, cost per call

**Use case**: Analyze individual AI API calls, debug errors

---

### 6. Providers Page (`/providers`)

**What it shows**:
- Provider comparison cards (cost, calls, latency, success rate)
- Provider & model breakdown table

**Use case**: Compare AI provider performance and costs

---

## Navigation Flow

```
Dashboard
  │
  ├─> Click Function Row
  │      │
  │      └─> Function Detail Page
  │             │
  │             ├─> View Provider/Model Breakdown
  │             │
  │             └─> Click Execution Row
  │                    │
  │                    └─> Execution Drawer (right panel)
  │                           │
  │                           ├─> View Execution Summary
  │                           │
  │                           └─> Click Call to Expand
  │                                  │
  │                                  └─> View Request/Response JSON
  │
  ├─> Navigate to /functions
  │      │
  │      └─> View all functions table
  │
  ├─> Navigate to /calls
  │      │
  │      └─> View all AI API calls
  │
  └─> Navigate to /providers
         │
         └─> Compare provider stats
```

---

## Data Model

### Execution Record (1 per @AIPrompt method call)

```json
{
  "executionId": "exec-7f3a2b9c",
  "functionName": "generate-summary",
  "category": "reporting",
  "tags": ["finance", "reports"],
  "environment": "PROD",
  "startedAt": "2024-01-15T14:32:07Z",
  "finishedAt": "2024-01-15T14:32:10Z",
  "durationMs": 2847,
  "status": "success",
  "callsCount": 5,
  "totalTokens": 12847,
  "totalCost": 0.0847
}
```

### Call Record (N per execution)

```json
{
  "callId": "call-1",
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
  "requestPreview": "{\"messages\":[...]}",
  "responsePreview": "{\"choices\":[...]}",
  "createdAt": "2024-01-15T14:32:08Z"
}
```

---

## Backend API Reference

### Dashboard Summary

```http
GET /api/dashboard/summary?from=2024-01-01&to=2024-01-31&env=PROD
```

**Response**:
```json
{
  "totalCost": 4892.47,
  "totalExecutions": 12847,
  "totalCalls": 47293,
  "avgLatencyMs": 847,
  "avgCallsPerExecution": 3.68,
  "executionErrorRate": 0.024,
  "callErrorRate": 0.024
}
```

### Functions List

```http
GET /api/functions?from=2024-01-01&to=2024-01-31&page=0&size=25
```

**Response**:
```json
{
  "content": [
    {
      "functionName": "generate-summary",
      "category": "reporting",
      "tags": ["finance"],
      "executions": 1247,
      "calls": 8432,
      "callsPerExecution": 6.8,
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

### Function Detail

```http
GET /api/functions/generate-summary
```

**Response**:
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

### Execution Detail

```http
GET /api/executions/exec-7f3a2b9c
```

**Response**:
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

### Calls List

```http
GET /api/calls?provider=OpenAI&status=success&page=0&size=100
```

**Response**: Paginated list of Call objects

---

## Configuration Reference

### Backend Configuration

```yaml
# Full configuration options
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

### Frontend Configuration

```env
# Required
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080

# Optional
PORT=3000
NODE_ENV=development
```

---

## Troubleshooting

### Backend Issues

#### Issue: Database connection error

**Solution**: Ensure PostgreSQL is running and connection details are correct in `application.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ai_prompt_tracker
    username: postgres
    password: your_password
```

#### Issue: Calls not being tracked

**Solution**:
1. Verify `@AIPrompt` annotation is present
2. Check HTTP client is supported (WebClient, RestTemplate, RestClient, OkHttp)
3. For OkHttp, verify interceptor is registered
4. Check logs for "Registered X custom provider matcher(s)"

---

### Frontend Issues

#### Issue: Dashboard shows "No data found"

**Solution**:
1. Verify backend is running: `curl http://localhost:8080/api/dashboard/summary`
2. Check `.env.local` has correct backend URL
3. Check CORS configuration in backend
4. Check browser console for errors

#### Issue: CORS errors

**Solution**: Add CORS configuration to backend:

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE");
    }
}
```

---

## Documentation Index

### Backend
- [INTEGRATIONS_GUIDE.md](INTEGRATIONS_GUIDE.md) - Multi-client integration guide
- [MULTI_CLIENT_SUPPORT_SUMMARY.md](MULTI_CLIENT_SUPPORT_SUMMARY.md) - Implementation details
- [USAGE_METRICS_PARSER_REFACTORING.md](USAGE_METRICS_PARSER_REFACTORING.md) - Parser documentation
- [DASHBOARD_API_IMPLEMENTATION.md](DASHBOARD_API_IMPLEMENTATION.md) - API documentation

### Frontend
- [FRONTEND_ARCHITECTURE.md](FRONTEND_ARCHITECTURE.md) - Architecture design
- [FRONTEND_SETUP_GUIDE.md](FRONTEND_SETUP_GUIDE.md) - Setup instructions
- [FRONTEND_IMPLEMENTATION_SUMMARY.md](FRONTEND_IMPLEMENTATION_SUMMARY.md) - Implementation summary
- [frontend/README.md](frontend/README.md) - Frontend documentation

### This Guide
- [FULL_SYSTEM_GUIDE.md](FULL_SYSTEM_GUIDE.md) - This file

---

## Next Steps

1. ✅ Start backend: `cd backend && ./gradlew bootRun`
2. ✅ Start frontend: `cd frontend && npm install && npm run dev`
3. ✅ Open dashboard: [http://localhost:3000/dashboard](http://localhost:3000/dashboard)
4. ✅ Add `@AIPrompt` annotations to your AI-calling methods
5. ✅ Make some AI API calls
6. ✅ View metrics in dashboard
7. ✅ Drill down into function details
8. ✅ View execution timelines
9. ✅ Analyze costs and performance

**Happy tracking!** 🚀
