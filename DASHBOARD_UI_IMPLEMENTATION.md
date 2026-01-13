# Dashboard UI Implementation Summary

## 목표

AI Prompt Tracker Starter에 포함될 최소한의 대시보드 UI (MVP)를 구현하여, 사용자가 @AIPrompt 어노테이션으로 추적된 데이터를 실시간으로 확인할 수 있도록 한다.

---

## 1. 현재 상태 분석 결과

### 사용 가능한 REST API 엔드포인트

| 엔드포인트 | 설명 | 반환 데이터 |
|-----------|------|------------|
| `GET /aiprompt-tracker/api/dashboard/summary` | 전체 통계 요약 | totalExecutions, totalCalls, totalCost, avgLatencyMs, avgCallsPerExecution, executionErrorRate, callErrorRate |
| `GET /aiprompt-tracker/api/functions` | Function 목록 (페이징) | functionName, category, tags, executions, calls, callsPerExecution, totalCost, avgExecutionTimeMs, errorRate |
| `GET /aiprompt-tracker/api/functions/{name}/executions` | 특정 function의 execution 목록 | executionId, status, callsCount, totalTokens, totalCost, durationMs, startedAt |
| `GET /aiprompt-tracker/api/executions/{id}` | Execution 상세 + Call 목록 | **Execution 정보** + calls 배열 (provider, model, tokens, cost, latency, status) |

### Persistence Mode별 API 동작

- **persistence.mode=NONE:** 모든 API가 HTTP 503 반환
  ```json
  {
    "error": "PERSISTENCE_DISABLED",
    "message": "...",
    "howToEnable": "Set ai-prompts.tracking.persistence.mode=jdbc ..."
  }
  ```

- **persistence.mode=JDBC 또는 DEMO:**
  - 데이터 있음: 정상 JSON 반환
  - 데이터 0개: 빈 배열 또는 null 값 반환

### 핵심 데이터 관계

```
@AIPrompt 메서드 실행 (Function)
  → Execution (1회 실행 기록)
    → Call (여러 AI API 호출)
```

이 관계가 UI에서 명확히 드러나야 한다.

---

## 2. UI 설계 결정

### 기술 스택 선택: 정적 HTML + Vanilla JavaScript

**선택 이유:**
- ✅ Starter JAR에 포함되므로 빌드 과정 불필요
- ✅ 외부 의존성 없음 (React, Vue 등 프레임워크 불필요)
- ✅ 브라우저만 있으면 즉시 동작
- ✅ 경량화 (~600 줄, 단일 HTML 파일)

**대안을 배제한 이유:**
- ❌ React/Vue: 빌드 과정 필요, 번들 크기 증가
- ❌ Thymeleaf: 서버 렌더링 필요, API 중복 호출
- ❌ CDN 기반 라이브러리: 외부 의존성 발생

### 화면 구조

```
┌─────────────────────────────────────┐
│  Header: AI Prompt Tracker          │
├─────────────────────────────────────┤
│  [Overview] [Functions]              │ ← Navigation Tabs
├─────────────────────────────────────┤
│                                      │
│  Screen 1: Overview                  │
│  - 7개 stat cards (총 executions,   │
│    calls, cost, latency 등)          │
│                                      │
│  Screen 2: Functions                 │
│  - Function 목록 테이블              │
│  - Click → Execution 목록            │
│  - Click → Execution 상세            │
│                                      │
│  Screen 3: Execution Detail          │
│  - Execution 기본 정보 (grid)        │
│  - Call Timeline 테이블              │
│  - Back 버튼                         │
│                                      │
└─────────────────────────────────────┘
```

---

## 3. 생성/수정된 파일 목록

### 생성된 파일

1. **`tracker-starter/src/main/resources/META-INF/resources/aiprompt-tracker/dashboard.html`**
   - **역할:** 메인 대시보드 UI (단일 파일 SPA)
   - **크기:** ~600 줄 (HTML + CSS + JavaScript)
   - **기능:**
     - Overview 화면 (dashboard/summary API)
     - Functions 목록 (functions API)
     - Function → Executions 전환 (functions/{name}/executions API)
     - Execution 상세 (executions/{id} API)
     - Persistence disabled 상태 처리
     - Empty state 처리 (데이터 0개)

### 수정된 파일

2. **`tracker-starter/src/main/resources/META-INF/resources/aiprompt-tracker/index.html`**
   - **변경 전:** 정적 "coming soon" 메시지 + API 링크
   - **변경 후:** dashboard.html로 자동 리다이렉트
   - **이유:** 기본 URL (`/aiprompt-tracker/`)에서 즉시 대시보드 표시

---

## 4. 화면별 상세 설명

### Screen 1: Overview (Summary)

**API:** `GET /aiprompt-tracker/api/dashboard/summary`

**화면 구성:**
- 7개 stat cards (grid layout)
  1. Total Executions
  2. Total API Calls
  3. Total Cost (USD)
  4. Avg Latency (ms)
  5. Avg Calls / Execution
  6. Execution Error Rate (%)
  7. Call Error Rate (%)

**상태별 표시:**
- Persistence=NONE: 빨간 경고 박스 + 설정 가이드
- 데이터 0개: "No data yet" 빈 상태 + 안내 메시지
- 데이터 있음: 통계 카드 7개 표시

**예시 스크린 구조:**
```
┌──────────────────────────────────────────────────┐
│ Total Executions │ Total API Calls │ Total Cost  │
│       124         │      456        │   $12.45    │
├──────────────────────────────────────────────────┤
│ Avg Latency      │ Avg Calls/Exec  │ Exec Error  │
│     850ms        │      3.7        │    2.4%     │
├──────────────────────────────────────────────────┤
│ Call Error Rate  │                               │
│     1.2%         │                               │
└──────────────────────────────────────────────────┘
```

---

### Screen 2: Functions (목록)

**API:** `GET /aiprompt-tracker/api/functions?size=50`

**화면 구성:**
- 테이블 (8컬럼)
  - Function Name (클릭 가능)
  - Category
  - Executions
  - Calls
  - Calls/Exec
  - Total Cost
  - Avg Time
  - Error Rate

**인터랙션:**
- Function Name 클릭 → 해당 function의 execution 목록으로 전환

**상태별 표시:**
- Persistence=NONE: 경고 박스
- 데이터 0개: "No functions tracked yet" 빈 상태
- 데이터 있음: 테이블 표시

---

### Screen 2-1: Function Executions (전환 화면)

**API:** `GET /aiprompt-tracker/api/functions/{functionName}/executions?size=50`

**화면 구성:**
- Back 버튼 (← Back to Functions)
- 테이블 (7컬럼)
  - Execution ID (앞 8자, 클릭 가능)
  - Status (SUCCESS / ERROR badge)
  - Calls
  - Tokens
  - Cost
  - Duration
  - Started At

**인터랙션:**
- Back 버튼 → Functions 목록으로 복귀
- Execution ID 클릭 → Execution 상세 화면으로 이동

---

### Screen 3: Execution Detail

**API:** `GET /aiprompt-tracker/api/executions/{executionId}`

**화면 구성:**

1. **Back 버튼**
   - ← Back to Functions

2. **Execution 기본 정보 카드 (grid layout)**
   - Execution ID
   - Function Name
   - Category
   - Environment
   - Status (badge)
   - Duration
   - Total Calls
   - Total Tokens
   - Total Cost
   - Started At
   - Finished At
   - Error Message (있을 경우 빨간 박스)

3. **Call Timeline 카드 (테이블)**
   - # (순서)
   - Provider (OpenAI, Anthropic 등)
   - Model (gpt-4, claude-3 등)
   - Tokens
   - Cost
   - Latency
   - Status (badge)
   - Created At

**핵심:** Execution → Call 관계가 명확히 드러남

**예시 스크린 구조:**
```
← Back to Functions

┌─ Execution Detail ────────────────────────────────┐
│ Execution ID: abc123...     Function: chatbot     │
│ Category: customer-service  Environment: prod     │
│ Status: SUCCESS            Duration: 1.2s         │
│ Total Calls: 3             Total Tokens: 1,234    │
│ Total Cost: $0.0456        Started: 2026-01-13... │
└───────────────────────────────────────────────────┘

┌─ Call Timeline (3 calls) ─────────────────────────┐
│ # │ Provider │ Model    │ Tokens │ Cost   │ ... │
│ 1 │ OpenAI   │ gpt-4    │   512  │ $0.02  │ ... │
│ 2 │ Anthropic│ claude-3 │   400  │ $0.01  │ ... │
│ 3 │ OpenAI   │ gpt-3.5  │   322  │ $0.003 │ ... │
└───────────────────────────────────────────────────┘
```

---

## 5. API와 화면 매핑

| 화면 | 사용 API | 용도 |
|------|---------|------|
| Overview | `/api/dashboard/summary` | 전체 통계 7개 표시 |
| Functions 목록 | `/api/functions` | Function별 집계 테이블 |
| Function Executions | `/api/functions/{name}/executions` | 특정 function의 execution 목록 |
| Execution Detail | `/api/executions/{id}` | Execution 상세 + Call 목록 (핵심 관계 표현) |

**API 호출 흐름:**
```
페이지 로드 → dashboard/summary
Functions 탭 클릭 → functions
Function Name 클릭 → functions/{name}/executions
Execution ID 클릭 → executions/{id}
Back 버튼 클릭 → functions (재조회)
```

---

## 6. Persistence Mode별 화면 동작

### Mode 1: persistence.mode=NONE

**API 응답:**
```
HTTP 503 Service Unavailable
{
  "error": "PERSISTENCE_DISABLED",
  "message": "Persistence is not enabled...",
  "howToEnable": "Set ai-prompts.tracking.persistence.mode=jdbc ..."
}
```

**UI 표시:**
```
┌─────────────────────────────────────────────────┐
│ ⚠️ Persistence Disabled                         │
│                                                  │
│ AI Prompt Tracker is running, but data          │
│ persistence is not enabled. Tracking still      │
│ works, but data is not saved to a database.     │
│                                                  │
│ To enable dashboard:                            │
│                                                  │
│ Option 1: Demo Mode (Quick Start)              │
│ ai-prompts.tracking.demo.enabled: true          │
│                                                  │
│ Option 2: Production Mode                       │
│ ai-prompts.tracking.persistence.mode: jdbc      │
│ (Requires datasource configuration)             │
│                                                  │
│ See Schema Management Guide                     │
└─────────────────────────────────────────────────┘
```

### Mode 2: persistence.mode=JDBC/DEMO, 데이터 0개

**API 응답:**
```json
{
  "totalExecutions": 0,
  "totalCalls": 0,
  "totalCost": 0.0,
  ...
}
```

**UI 표시:**
```
┌─────────────────────────────────────────────────┐
│            📊                                    │
│                                                  │
│        No data yet                               │
│                                                  │
│  Start using @AIPrompt annotations in your      │
│  code to see tracking data here.                │
│                                                  │
└─────────────────────────────────────────────────┘
```

### Mode 3: persistence.mode=JDBC/DEMO, 데이터 있음

**API 응답:** 정상 데이터

**UI 표시:** 위 섹션 4에서 설명한 정상 화면

---

## 7. 왜 이 정도가 MVP로 적절한가?

### 판단 근거

#### ✅ 충족 조건

1. **"지금 뭘 하고 있는지 한눈에 보인다"**
   - Overview 화면: 7개 핵심 지표 즉시 확인
   - Functions 목록: 어떤 function이 비용을 많이 쓰는지 확인
   - Execution Detail: Execution → Call 관계 명확히 표현

2. **실제 구현된 백엔드와 정합적**
   - 가능한 API만 사용 (새 API 추가 없음)
   - 실제 반환되는 DTO 필드만 표시
   - Persistence mode 3가지 상태 모두 처리

3. **Starter 포함 목적에 부합**
   - 단일 HTML 파일 (~600줄)
   - 외부 의존성 없음
   - 빌드 과정 불필요
   - JAR에 포함되어 즉시 사용 가능

4. **과하지 않고, 부족하지 않음**
   - 불필요한 기능 배제 (로그인, 복잡한 차트, 실시간 스트림)
   - 필수 기능만 포함 (통계 확인, 목록 조회, 상세 보기)

#### ❌ 의도적으로 배제한 것 (과함)

- 로그인/권한 관리
- 복잡한 시계열 차트 (Chart.js 등)
- 실시간 WebSocket 업데이트
- 고급 필터링 (날짜 범위, 태그 검색 등은 API 지원하지만 UI 복잡도 증가)
- Export (CSV, JSON)
- Call 상세 (requestPreview, responsePreview) - 민감 정보 가능성

#### 🔄 향후 확장 가능한 부분 (P1, P2)

- **P1 (추가 고려):**
  - 간단한 필터 (날짜 범위, category 선택)
  - 페이지네이션 UI (현재는 첫 50개만 표시)
  - Provider별 비용 파이 차트 (경량 SVG)

- **P2 (Full React 버전에서):**
  - 실시간 업데이트
  - 고급 분석 (비용 트렌드, 모델별 성능 비교)
  - Call 상세 보기 (request/response preview)

---

## 8. 사용 시나리오

### 시나리오 1: 처음 Starter 추가한 개발자

1. Starter 의존성 추가 후 앱 실행
2. 브라우저에서 `http://localhost:8080/aiprompt-tracker/` 접속
3. 자동으로 `/dashboard.html`로 리다이렉트
4. Persistence=NONE이므로 경고 메시지 표시
5. 가이드를 보고 `ai-prompts.tracking.demo.enabled: true` 설정
6. 앱 재시작 → Overview 화면에서 "No data yet" 표시
7. @AIPrompt 어노테이션이 있는 메서드 실행
8. 새로고침 → 통계 카드와 Functions 목록 표시

### 시나리오 2: 프로덕션 환경에서 모니터링

1. `persistence.mode: jdbc` 설정으로 운영 중
2. 대시보드 접속 → Overview에서 총 비용 $1,234.56 확인
3. Functions 탭 → "chatbot" function이 비용 1위
4. "chatbot" 클릭 → 최근 100개 execution 확인
5. 특정 execution 클릭 → 3개 call 중 2번째 call이 느림 (1.2s)
6. Provider: OpenAI, Model: gpt-4 확인
7. 비용 최적화를 위해 gpt-3.5로 변경 검토

### 시나리오 3: 에러 디버깅

1. Overview에서 Execution Error Rate 15% 확인 (높음)
2. Functions 탭 → "dataAnalyzer" function의 error rate 50%
3. "dataAnalyzer" 클릭 → 최근 execution 절반이 ERROR 상태
4. 에러 execution 클릭 → Error Message 확인: "API rate limit exceeded"
5. Call Timeline 확인 → 3개 call 모두 실패
6. Rate limit 이슈로 판단 → 재시도 로직 추가 결정

---

## 9. 기술적 상세

### JavaScript 구조

```javascript
// Global State
let currentScreen = 'overview';
let persistenceEnabled = true;

// Navigation
function showScreen(screen)  // 화면 전환

// Utility Functions
formatCost(cost)              // $0.0123
formatNumber(num)             // 1,234
formatPercent(rate)           // 12.3%
formatDuration(ms)            // 1.2s
formatDate(isoString)         // 2026-01-13 12:34:56

// Rendering
renderPersistenceDisabled()   // 503 상태 UI
renderEmptyState(title, msg)  // 데이터 0개 UI

// API Calls
async loadOverview()          // → dashboard/summary
async loadFunctions()         // → functions
async loadFunctionExecutions(name) // → functions/{name}/executions
async loadExecutionDetail(id)     // → executions/{id}
```

### CSS 설계 원칙

- **모던하지만 과하지 않음:** gradient header, subtle shadows
- **가독성 우선:** 충분한 padding, 명확한 색상 구분
- **반응형:** grid layout, max-width, overflow-x: auto
- **상태 표현:** badge (success/error), empty-state, error-state
- **인터랙션:** hover 효과, cursor: pointer, link 색상

### 성능 고려사항

- **단일 HTML 파일:** HTTP 요청 최소화
- **Lazy loading:** 화면 전환 시에만 API 호출
- **페이지네이션 제한:** 기본 50개 (size=50)
- **No polling:** 실시간 업데이트 없음 (수동 새로고침)

---

## 10. 테스트 가이드

### 테스트 시나리오

#### 1. Persistence=NONE 테스트

```yaml
# application.yml
ai-prompts:
  tracking:
    persistence:
      mode: none  # 또는 미설정
```

**예상 결과:**
- Overview, Functions 탭 모두 경고 박스 표시
- "To enable dashboard" 가이드 표시

#### 2. Demo Mode + 데이터 0개 테스트

```yaml
ai-prompts:
  tracking:
    demo:
      enabled: true
```

**예상 결과:**
- Overview: "No data yet" 빈 상태
- Functions: "No functions tracked yet" 빈 상태

#### 3. 정상 데이터 테스트

1. @AIPrompt 어노테이션이 있는 메서드 실행
2. 대시보드 새로고침
3. 모든 화면 동작 확인:
   - Overview: 통계 카드 표시
   - Functions: 테이블 표시
   - Function 클릭 → Execution 목록
   - Execution 클릭 → 상세 + Call 목록
   - Back 버튼 → 이전 화면 복귀

### 브라우저 호환성

- ✅ Chrome 90+
- ✅ Firefox 88+
- ✅ Safari 14+
- ✅ Edge 90+

(ES6 fetch, async/await, template literals 사용)

---

## 11. 향후 개선 방향

### Phase 2: Enhanced MVP (Starter에 포함 가능)

- 간단한 날짜 범위 필터 (HTML5 date input)
- Category 드롭다운 필터
- Provider별 비용 비율 (SVG pie chart, no library)
- 페이지네이션 UI (현재는 첫 50개만)

### Phase 3: Full React Dashboard (별도 프로젝트)

- 실시간 WebSocket 업데이트
- 시계열 차트 (비용 트렌드, latency 트렌드)
- Call request/response preview
- Export (CSV, JSON)
- 고급 검색 및 필터링
- 사용자 설정 (테마, 대시보드 레이아웃)

---

## 12. 결론

이 MVP UI는 다음을 달성한다:

✅ **"지금 뭘 하고 있는지 한눈에 보인다"**
   - 통계 7개로 전체 현황 파악
   - Function → Execution → Call 관계 명확

✅ **실제 백엔드와 100% 정합성**
   - 가능한 API만 사용
   - 실제 데이터 구조 그대로 표현
   - 모든 persistence mode 처리

✅ **Starter 포함 목적에 부합**
   - 경량 (단일 HTML, ~600줄)
   - 외부 의존성 없음
   - 즉시 사용 가능

✅ **과하지 않고, 부족하지 않음**
   - 관측 대시보드의 핵심만 포함
   - 복잡한 분석 도구 기능 배제
   - 향후 확장 가능한 구조

**이 수준이 Starter에 포함될 MVP로서 적절하다.**
