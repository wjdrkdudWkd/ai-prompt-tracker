# Dashboard Architecture: Dual UI Strategy

## 개요

AI Prompt Tracker는 **두 개의 독립적인 대시보드 UI**를 제공합니다. 이 문서는 각 UI의 역할, 기술 스택, 접속 방법을 명확히 정의합니다.

---

## 1. Embedded MVP Dashboard (Starter 포함)

### 위치
```
tracker-starter/src/main/resources/META-INF/resources/aiprompt-tracker/
├── index.html          → dashboard.html로 리다이렉트
└── dashboard.html      → 메인 MVP UI (단일 파일, ~600줄)
```

### 기술 스택
- **HTML** + **Vanilla JavaScript** + **Fetch API**
- **CSS:** 인라인 (external dependencies 없음)
- **빌드 과정:** 없음 (정적 파일 그대로 JAR에 포함)

### 접속 방법
```
http://localhost:8080/aiprompt-tracker/              → 자동 리다이렉트
http://localhost:8080/aiprompt-tracker/dashboard.html → 직접 접속
```

### 특징
- ✅ **Zero-config:** Starter 의존성 추가 시 자동 포함
- ✅ **경량:** 단일 HTML 파일, 외부 라이브러리 없음
- ✅ **즉시 사용 가능:** 브라우저만 있으면 동작
- ✅ **Spring Boot 임베디드:** JAR에 포함되어 배포 불필요

### 제공 화면
1. **Overview (Summary)**
   - 7개 KPI 카드: totalExecutions, totalCalls, totalCost, avgLatencyMs, avgCallsPerExecution, executionErrorRate, callErrorRate

2. **Functions 목록**
   - Function별 집계 테이블
   - 클릭 → Execution 목록으로 전환

3. **Function Executions**
   - 특정 function의 execution 목록
   - 클릭 → Execution 상세로 전환

4. **Execution Detail**
   - Execution 기본 정보 (grid layout)
   - Call Timeline 테이블 (Execution → Call 관계 명시)

### Persistence Mode 처리
- **mode=NONE:** HTTP 503 감지 → 빨간 경고 박스 + 설정 가이드
- **데이터 0개:** "No data yet" 빈 상태 메시지
- **데이터 있음:** 정상 UI 표시

### 사용 사례
- ✅ 개발 중 빠른 디버깅
- ✅ Tracking 작동 여부 확인
- ✅ 간단한 프로덕션 모니터링
- ✅ 설정 가이드 확인 (persistence disabled 상태)

---

## 2. Next.js Dashboard (별도 배포)

### 위치
```
frontend/
├── app/                           # Next.js App Router
│   ├── dashboard/page.tsx         # Overview
│   ├── functions/
│   │   ├── page.tsx               # Functions 목록
│   │   └── [functionName]/page.tsx # Function 상세
│   ├── calls/page.tsx             # Calls 탐색
│   ├── providers/page.tsx         # Provider 분석
│   ├── settings/page.tsx          # 설정
│   └── layout.tsx                 # Root layout (sidebar)
├── components/                    # UI 컴포넌트
├── lib/                           # API hooks, types, utils
└── package.json
```

### 기술 스택
- **Framework:** Next.js 14 (App Router)
- **Language:** TypeScript
- **Styling:** Tailwind CSS
- **UI Components:** shadcn/ui (Radix UI)
- **Data Fetching:** @tanstack/react-query
- **Icons:** lucide-react
- **Build:** `npm run build` → `out/` 디렉토리 (정적 export)

### 접속 방법

**개발 모드:**
```bash
cd frontend
npm install
npm run dev

# 접속:
http://localhost:3000/aiprompt-tracker/dashboard
```

**프로덕션 배포:**
```bash
# 정적 export 빌드
npm run build

# out/ 디렉토리를 Nginx, Vercel, Docker 등으로 배포
# 예: npx serve out
```

### 특징
- ✅ **프로덕션급 UX:** shadcn/ui, Tailwind, 반응형 디자인
- ✅ **고급 기능:** Provider/Model breakdown, Execution Drawer, React Query 캐싱
- ✅ **확장 가능:** 컴포넌트 기반, 쉬운 커스터마이징
- ✅ **타입 안전:** TypeScript end-to-end
- ❌ **빌드 필요:** npm install + build 과정 필수
- ❌ **별도 배포:** Spring Boot와 독립적으로 배포

### 제공 화면
1. **Dashboard (Overview)**
   - 6개 KPI 카드 (MVP와 유사하지만 UI 개선)
   - Functions 테이블 (하단)

2. **Functions 목록 페이지**
   - 전체 function 목록 (100개까지)
   - 클릭 → Function 상세 페이지로 라우팅

3. **Function 상세 페이지**
   - 6개 KPI 카드
   - **Provider Breakdown** (MVP에 없음)
   - **Model Breakdown** (MVP에 없음)
   - Execution History 테이블
   - 클릭 → Execution Drawer 열림

4. **Execution Drawer**
   - 우측에서 슬라이드되는 패널
   - Execution 상세 정보
   - Call Timeline (확장 가능)
   - Request/Response Preview 지원

5. **Calls 페이지** (MVP에 없음)
   - 모든 AI call 목록
   - Provider/Model/Status 필터링

6. **Providers 페이지** (MVP에 없음)
   - Provider 비교 분석
   - 비용 집계

### 사용 사례
- ✅ 프로덕션 고급 분석
- ✅ 팀 협업 (컴포넌트 커스터마이징)
- ✅ 커스텀 배포 요구사항
- ✅ 실시간 업데이트 (향후 WebSocket 지원)

---

## 3. 두 UI의 비교

| 특징 | Embedded MVP | Next.js Dashboard |
|------|--------------|-------------------|
| **기술** | Vanilla JS (~600줄) | Next.js + React (~3000줄) |
| **배포** | Spring Boot JAR에 포함 | 별도 배포 필요 |
| **설정** | Zero-config | npm install + build |
| **UI 프레임워크** | Custom CSS | shadcn/ui + Tailwind |
| **상태 관리** | 없음 (직접 fetch) | React Query |
| **라우팅** | In-page navigation | Next.js App Router |
| **Overview** | ✅ 7 KPIs | ✅ 6 KPIs (UI 개선) |
| **Functions 목록** | ✅ In-page 전환 | ✅ 별도 페이지 |
| **Function 상세** | ✅ Basic | ✅ + Provider/Model breakdown |
| **Execution 상세** | ✅ 별도 화면 | ✅ Drawer (더 나은 UX) |
| **Provider Breakdown** | ❌ | ✅ |
| **Model Breakdown** | ❌ | ✅ |
| **Calls 페이지** | ❌ | ✅ |
| **Providers 페이지** | ❌ | ✅ |
| **실시간 업데이트** | ❌ | 🔄 향후 지원 |
| **Export 기능** | ❌ | 🔄 향후 지원 |

---

## 4. 언제 어떤 UI를 사용할까?

### Embedded MVP Dashboard 사용 권장

- ✅ 개발 중 빠른 현황 파악
- ✅ Tracking 작동 여부 확인
- ✅ 데이터가 저장되는지 확인
- ✅ Persistence 설정 문제 디버깅
- ✅ 간단한 프로덕션 모니터링 (기본 지표만 필요)
- ✅ 별도 UI 배포를 원하지 않는 경우

### Next.js Dashboard 사용 권장

- ✅ 프로덕션 환경에서 고급 분석 필요
- ✅ Provider/Model별 비용 분석
- ✅ 팀 협업 및 UI 커스터마이징
- ✅ 향후 기능 확장 계획 (차트, 실시간 등)
- ✅ 별도 도메인/서브도메인 배포 가능

---

## 5. 아키텍처 다이어그램

```
┌──────────────────────────────────────────────────────┐
│                   User Browser                       │
└──────────────────┬───────────────────────────────────┘
                   │
       ┌───────────┴───────────┐
       │                       │
       ▼                       ▼
┌─────────────────┐   ┌──────────────────┐
│  Embedded MVP   │   │  Next.js Dash    │
│  (dashboard.html)│   │  (port 3000)     │
│                 │   │                  │
│  /aiprompt-     │   │  /aiprompt-      │
│  tracker/       │   │  tracker/        │
│                 │   │  dashboard       │
└────────┬────────┘   └────────┬─────────┘
         │                     │
         │                     │
         └──────────┬──────────┘
                    │
                    ▼
        ┌────────────────────────┐
        │  Spring Boot Backend   │
        │  (port 8080)           │
        │                        │
        │  /aiprompt-tracker/    │
        │  api/*                 │
        └────────────────────────┘
```

---

## 6. API 연동

### Embedded MVP
```javascript
// Vanilla JS fetch
const response = await fetch('/aiprompt-tracker/api/dashboard/summary');
const data = await response.json();
```

### Next.js Dashboard
```typescript
// React Query hook
const { data, isLoading } = useDashboardSummary();

// Internally uses:
// fetch('/api/dashboard/summary')
// → proxied to http://localhost:8080/aiprompt-tracker/api/dashboard/summary
```

**Next.js Proxy 설정 (`next.config.js`):**
```javascript
async rewrites() {
  return [
    {
      source: '/api/:path*',
      destination: `${process.env.NEXT_PUBLIC_API_BASE_URL}/aiprompt-tracker/api/:path*`,
    },
  ];
}
```

---

## 7. 배포 시나리오

### 시나리오 1: MVP만 사용 (가장 간단)

```
1. Starter 의존성 추가
2. 앱 실행: java -jar myapp.jar
3. 접속: http://localhost:8080/aiprompt-tracker/
```

**장점:**
- Zero-config
- 추가 인프라 불필요

**단점:**
- 기능 제한적
- UI 커스터마이징 어려움

---

### 시나리오 2: Next.js Dashboard 별도 배포

#### Option A: Docker + Nginx

```dockerfile
# Dockerfile
FROM node:18-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/out /usr/share/nginx/html/aiprompt-tracker
COPY nginx.conf /etc/nginx/conf.d/default.conf
```

```nginx
# nginx.conf
server {
  listen 80;
  location /aiprompt-tracker/ {
    alias /usr/share/nginx/html/aiprompt-tracker/;
    try_files $uri $uri/ /aiprompt-tracker/index.html;
  }
  location /api/ {
    proxy_pass http://backend:8080/aiprompt-tracker/api/;
  }
}
```

#### Option B: Vercel/Netlify 배포

```bash
cd frontend
npm run build

# Vercel
vercel deploy

# Netlify
netlify deploy --prod
```

**환경 변수 설정:**
```
NEXT_PUBLIC_API_BASE_URL=https://api.example.com
```

---

### 시나리오 3: 두 UI 모두 사용 (권장)

**배포 구성:**
```
http://myapp.com/                         → Spring Boot App
http://myapp.com/aiprompt-tracker/        → Embedded MVP (JAR 포함)
http://dashboard.myapp.com/               → Next.js Dashboard (별도 배포)
```

**사용 사례 분리:**
- 개발자 → Embedded MVP (빠른 확인)
- 운영팀 → Next.js Dashboard (고급 분석)

---

## 8. 향후 로드맵

### Embedded MVP (Phase 2)
- [ ] 간단한 날짜 범위 필터 (HTML5 date input)
- [ ] Provider별 비용 비율 (SVG 원형 차트, 라이브러리 없이)
- [ ] 페이지네이션 UI

### Next.js Dashboard (Phase 2)
- [ ] 실시간 업데이트 (WebSocket)
- [ ] 비용 트렌드 차트 (Chart.js)
- [ ] Latency 트렌드 차트
- [ ] Call request/response preview
- [ ] Export 기능 (CSV, JSON)

### Next.js Dashboard (Phase 3)
- [ ] 커스텀 대시보드 레이아웃
- [ ] 알림 설정 (비용 threshold 등)
- [ ] 팀 협업 기능
- [ ] 사용자별 권한 관리

---

## 9. FAQ

### Q: 왜 두 개의 UI를 만들었나요?

**A:**
- **Embedded MVP:** 모든 사용자가 즉시 사용할 수 있는 기본 대시보드
- **Next.js Dashboard:** 고급 사용자를 위한 확장 가능한 프로덕션급 UI

### Q: Embedded MVP를 React로 포팅하지 않나요?

**A:** 아니오. 이유:
- React 앱을 Spring Boot static resources로 서빙하면 client-side routing 문제 발생
- 빌드 과정 추가 시 starter 사용성 저하
- Vanilla JS MVP의 장점 (단일 파일, 즉시 수정 가능) 상실

### Q: Next.js Dashboard를 Starter JAR에 포함할 수 있나요?

**A:** 이론적으로 가능하지만 권장하지 않음:
- `output: 'export'`로 정적 export 가능
- 하지만 React Router가 Spring Boot static resources와 호환 안 됨
- 복잡도 증가, 유지보수 부담

### Q: 어떤 UI를 사용해야 하나요?

**A:** Use case에 따라 선택:
- **간단한 모니터링:** Embedded MVP
- **고급 분석:** Next.js Dashboard
- **둘 다:** 개발은 MVP, 프로덕션은 Next.js

### Q: Next.js Dashboard를 커스터마이징할 수 있나요?

**A:** 네, frontend/ 디렉토리를 fork하고:
1. 컴포넌트 수정
2. 새 페이지 추가
3. 빌드 후 배포

---

## 10. 관련 문서

- [Embedded MVP 구현 문서](./DASHBOARD_UI_IMPLEMENTATION.md)
- [Next.js Dashboard README](./frontend/README.md)
- [Schema Management Guide](./docs/SCHEMA_MANAGEMENT.md)
- [API Documentation](./tracker-starter/src/main/java/com/galoong/aiprompttracker/api/controller/)

---

## 결론

**AI Prompt Tracker의 Dual UI 전략:**

1. **Embedded MVP (dashboard.html)**
   - 역할: 빠른 관측, Zero-config
   - 대상: 모든 사용자
   - 배포: Starter JAR에 포함

2. **Next.js Dashboard (frontend/)**
   - 역할: 고급 분석, 프로덕션급 UX
   - 대상: 고급 사용자, 팀 협업
   - 배포: 별도 인프라

**이 전략으로 80%의 사용자는 MVP로 충분하고, 20%의 파워 유저는 Next.js로 확장 가능.**
