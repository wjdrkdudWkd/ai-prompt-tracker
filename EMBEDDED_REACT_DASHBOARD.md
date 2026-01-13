# Embedded React Dashboard - Implementation Guide

## 개요

AI Prompt Tracker는 **React (Next.js) 기반 대시보드를 Spring Boot Starter JAR에 임베드**하여 제공합니다.

**핵심 경험:**
```
1. Starter 의존성 추가
2. 서버 실행
3. http://localhost:8080/aiprompt-tracker/ 접속
4. ✅ React 대시보드가 즉시 표시됨 (Swagger처럼)
```

---

## 아키텍처

### 기본 전략: Embedded UI First

```
Spring Boot Starter JAR
├── Java Classes (tracking, API, etc.)
└── META-INF/resources/aiprompt-tracker/
    ├── index.html           ← React SPA 진입점
    ├── dashboard/
    │   └── index.html       ← /dashboard/ 라우트
    ├── functions/
    │   ├── index.html       ← /functions/ 라우트
    │   └── detail/
    │       └── index.html   ← /functions/detail?name=X (query param)
    ├── _next/               ← Next.js 정적 자산
    │   ├── static/
    │   └── ...
    └── dashboard-mvp.html   ← Fallback (vanilla MVP)
```

**라우팅 전략:**

1. **정적 파일 우선 서빙** (Spring Boot ResourceHttpRequestHandler)
   - `/aiprompt-tracker/dashboard/` → `dashboard/index.html`
   - `/aiprompt-tracker/_next/static/...` → `_next/static/...`
   - `.js`, `.css`, `.png` 등 → 그대로 서빙

2. **SPA Fallback** (UiRedirectController)
   - `/aiprompt-tracker/` → `forward:/aiprompt-tracker/index.html`
   - `/aiprompt-tracker/dashboard/` (정적 파일 없으면) → `index.html`
   - **단, API는 제외:** `/aiprompt-tracker/api/**` → REST Controller

3. **React Router 동작**
   - `index.html` 로드 → React hydration
   - 클라이언트 사이드 라우팅으로 `/dashboard`, `/functions` 등 렌더링
   - **Query Param 라우팅**: `/functions/detail?name=X` → `useSearchParams()`로 파라미터 읽음

---

## 구현 상세

### 1. Next.js 설정 (`frontend/next.config.js`)

```javascript
{
  output: 'export',                    // 정적 export
  basePath: '/aiprompt-tracker',       // Spring Boot 경로와 일치
  assetPrefix: '/aiprompt-tracker/',   // 자산 경로 prefix
  trailingSlash: true,                 // /dashboard/ → /dashboard/index.html
  images: {
    unoptimized: true                  // 정적 export에서는 이미지 최적화 불가
  }
}
```

**결과:**
- `npm run build` → `out/` 디렉토리 생성
- `out/index.html`, `out/dashboard/index.html`, `out/_next/static/...`

---

### 2. API 클라이언트 설정 (`frontend/lib/api/client.ts`)

```typescript
// Embedded mode: same-origin (default)
const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "";

// API calls:
// Embedded: fetch("/aiprompt-tracker/api/dashboard/summary")
// Standalone: fetch("http://backend:8080/aiprompt-tracker/api/dashboard/summary")
```

**환경별 설정:**

| 환경 | `NEXT_PUBLIC_API_BASE_URL` | API 호출 경로 |
|------|----------------------------|--------------|
| **Embedded** (기본) | "" (빈 문자열) | `/aiprompt-tracker/api/*` (same-origin) |
| **Standalone Dev** | `http://localhost:8080` | `http://localhost:8080/aiprompt-tracker/api/*` |
| **Standalone Prod** | `https://api.example.com` | `https://api.example.com/aiprompt-tracker/api/*` |

---

### 3. Spring MVC SPA Fallback (`UiRedirectController.java`)

```java
@Controller
public class UiRedirectController {

    @GetMapping({
        "/aiprompt-tracker",
        "/aiprompt-tracker/",
        "/aiprompt-tracker/**"
    })
    public String handleSpaRouting(HttpServletRequest request) {
        String path = request.getRequestURI();

        // API 요청은 절대 forward 안 함
        if (path.startsWith("/aiprompt-tracker/api/")) {
            return null; // REST Controller로 진행
        }

        // 정적 파일(확장자 있는 파일)은 ResourceHandler가 처리
        if (hasFileExtension(path)) {
            return null;
        }

        // 그 외 → index.html로 forward (SPA 라우팅)
        return "forward:/aiprompt-tracker/index.html";
    }
}
```

**동작 예시:**

| 요청 경로 | 처리 방식 |
|----------|----------|
| `/aiprompt-tracker/` | → `forward:/aiprompt-tracker/index.html` |
| `/aiprompt-tracker/dashboard/` | → 정적 파일 (`dashboard/index.html`) 또는 fallback |
| `/aiprompt-tracker/functions/abc` | → `forward:/aiprompt-tracker/index.html` (SPA 라우팅) |
| `/aiprompt-tracker/api/dashboard/summary` | → `DashboardController` (forward 안 함) |
| `/aiprompt-tracker/_next/static/xyz.js` | → 정적 파일 서빙 (forward 안 함) |

---

## 빌드 파이프라인

### 로컬 개발 (수동)

```bash
# 1. Frontend 빌드
cd frontend
npm install
npm run build

# 2. Starter로 복사
./copy-to-starter.sh

# 3. Starter 빌드
cd ..
./gradlew :tracker-starter:build

# 4. 실행 (backend 프로젝트 사용)
cd backend
./gradlew bootRun

# 5. 접속
http://localhost:8080/aiprompt-tracker/
```

---

### Gradle 통합 빌드

```bash
# Frontend 빌드 + 복사 (Node.js 필요)
./gradlew :tracker-starter:buildFrontend :tracker-starter:copyFrontend

# 또는 processResources에 통합 (build.gradle.kts 주석 해제)
# tasks.named("processResources") {
#     dependsOn("copyFrontend")
# }

# 이후 일반 빌드
./gradlew :tracker-starter:build
```

**Gradle Tasks:**

| Task | 설명 |
|------|------|
| `buildFrontend` | `cd frontend && npm run build` |
| `copyFrontend` | `frontend/out/` → `tracker-starter/src/main/resources/...` |
| `processResources` | (선택) `copyFrontend` 의존성 추가 가능 |

---

### CI/CD 환경

**Option 1: Node.js 설치된 환경**

```yaml
# .github/workflows/build.yml
steps:
  - uses: actions/setup-node@v3
    with:
      node-version: '18'

  - name: Build Frontend
    run: |
      cd frontend
      npm ci
      npm run build
      ./copy-to-starter.sh

  - name: Build Starter
    run: ./gradlew :tracker-starter:build
```

**Option 2: Node.js 없는 환경 (Skip Frontend)**

```bash
# Gradle은 Node.js가 없으면 자동으로 skip
./gradlew :tracker-starter:build

# 경고 출력:
# ⚠️  Node.js not found - skipping frontend build
#    React dashboard will not be included in this build
```

**Workaround:**
- 사전에 frontend를 빌드하고 `copy-to-starter.sh` 실행
- 결과물을 Git에 커밋 (선택적)

---

## 검증 시나리오

### 시나리오 1: Root 접속

```bash
# 요청
curl http://localhost:8080/aiprompt-tracker/

# 응답
200 OK
Content-Type: text/html
<html>...<div id="__next">...</div>...</html>
```

**확인:**
- React 앱 로드됨
- `/dashboard`로 redirect (React Router)

---

### 시나리오 2: 딥링크 직접 접속

```bash
# 요청
curl http://localhost:8080/aiprompt-tracker/dashboard/

# 응답
200 OK (정적 파일 dashboard/index.html)
또는
200 OK (fallback: forward to index.html)
```

**확인:**
- 404 발생 안 함
- React 앱 로드 후 `/dashboard` 렌더링

---

### 시나리오 3: Query Param 라우팅 (Function Detail)

```bash
# 요청
curl "http://localhost:8080/aiprompt-tracker/functions/detail?name=myFunction"

# 응답
200 OK (forward to index.html)
<html>...<div id="__next">...</div>...</html>
```

**확인:**
- Query param이 보존됨
- React Router가 `/functions/detail` 매칭
- `useSearchParams()`로 `name=myFunction` 읽음

### 시나리오 4: 새로고침 (Refresh)

```
1. 브라우저에서 /aiprompt-tracker/functions/detail?name=testFunc 접속
2. F5 새로고침
3. 결과: 200 OK, React 앱 유지, query param 유지
```

**확인:**
- SPA fallback 작동
- 404 발생 안 함
- Query param이 URL에 그대로 유지

---

### 시나리오 5: API 호출

```bash
# 요청
curl http://localhost:8080/aiprompt-tracker/api/dashboard/summary

# 응답
200 OK
Content-Type: application/json
{
  "totalCost": 123.45,
  "totalExecutions": 456,
  ...
}
```

**확인:**
- API 요청이 React 라우팅에 간섭받지 않음
- REST Controller가 정상 처리

---

### 시나리오 6: 정적 자산

```bash
# 요청
curl http://localhost:8080/aiprompt-tracker/_next/static/chunks/abc123.js

# 응답
200 OK
Content-Type: application/javascript
...
```

**확인:**
- JS/CSS/이미지 등 정적 파일 정상 서빙
- Forward 안 됨

---

## 왜 이 구조가 작동하는가?

### 1. Embedded UI가 기본인 이유

**✅ Zero-config Experience**
```
dependencies {
    implementation("com.galoong:ai-prompt-tracker-starter:1.0.0")
}
```
→ 의존성 추가만으로 UI 포함됨 (Swagger와 동일한 경험)

**✅ JAR에 포함됨**
- Next.js `output: 'export'` → 순수 정적 파일
- Spring Boot `META-INF/resources/` → 자동 서빙
- 추가 서버 불필요

**✅ Same-Origin API 호출**
- CORS 불필요
- Proxy 설정 불필요
- 보안 단순화

---

### 2. 별도 배포도 가능한 이유

**동일한 코드베이스:**
- `frontend/` 디렉토리는 standalone Next.js 프로젝트
- `npm run dev` → 개발 서버 실행 가능
- `npm run build` → 어디든 배포 가능

**환경 변수로 API 경로 변경:**
```bash
# Embedded (기본)
NEXT_PUBLIC_API_BASE_URL=""
→ fetch("/aiprompt-tracker/api/...")

# Standalone
NEXT_PUBLIC_API_BASE_URL="https://api.example.com"
→ fetch("https://api.example.com/aiprompt-tracker/api/...")
```

**배포 옵션:**
1. **Nginx + Static Files**
   ```nginx
   server {
     location /aiprompt-tracker/ {
       alias /var/www/dashboard/;
       try_files $uri $uri/ /aiprompt-tracker/index.html;
     }
     location /aiprompt-tracker/api/ {
       proxy_pass http://backend:8080/aiprompt-tracker/api/;
     }
   }
   ```

2. **Vercel/Netlify**
   ```bash
   cd frontend
   npm run build
   vercel deploy
   ```

3. **Docker**
   ```dockerfile
   FROM nginx:alpine
   COPY frontend/out /usr/share/nginx/html/aiprompt-tracker
   ```

---

## vanilla dashboard.html 처리 정책

**현재 위치:**
```
tracker-starter/src/main/resources/META-INF/resources/aiprompt-tracker/
├── index.html           ← React (기본)
├── dashboard-mvp.html   ← vanilla MVP (fallback)
└── ...
```

**역할:**
- `dashboard-mvp.html` = 개발 환경에서 React 빌드 없을 때 fallback
- 릴리즈 아티팩트에서는 React UI가 기본

**Fallback 시나리오:**
```
IF React build 없음 (out/ 디렉토리 비어있음)
  AND Node.js 없음 (빌드 불가능)
THEN
  dashboard-mvp.html을 index.html로 복사 (수동 또는 스크립트)
ELSE
  React UI가 기본
```

---

## 운영 시나리오

### 시나리오 A: Embedded Only (기본)

```
[Spring Boot App with Starter]
  ├── /aiprompt-tracker/       → React UI
  └── /aiprompt-tracker/api/   → REST API
```

**장점:**
- Zero-config
- Same-origin
- 배포 간단

**단점:**
- UI 업데이트 시 JAR 재배포 필요
- CDN 사용 불가

---

### 시나리오 B: Standalone Only

```
[React Dashboard (Vercel)]  →  [Spring Boot Backend]
  /aiprompt-tracker/          →  /aiprompt-tracker/api/
  (fetch with CORS)
```

**장점:**
- UI 독립 배포
- CDN 활용 가능
- 프론트팀 독립 개발

**단점:**
- CORS 설정 필요
- 인프라 복잡도 증가

---

### 시나리오 C: Hybrid (권장)

```
[Spring Boot App]
  ├── /aiprompt-tracker/       → Embedded React UI (기본)
  └── /aiprompt-tracker/api/   → REST API

[별도 Dashboard (선택적)]
  → https://dashboard.example.com → API 호출 with CORS
```

**사용 사례:**
- 개발자 → Embedded UI (빠른 확인)
- 운영팀 → Standalone UI (고급 기능)

---

## Troubleshooting

### 문제: `/dashboard` 접속 시 404

**원인:** React 빌드 결과물이 없음

**해결:**
```bash
cd frontend
npm install
npm run build
./copy-to-starter.sh
./gradlew :tracker-starter:build
```

---

### 문제: API 호출 시 404

**원인:** SPA fallback이 API 요청을 가로챔

**해결:** `UiRedirectController`에서 `/api/**` 제외 확인
```java
if (path.startsWith("/aiprompt-tracker/api/")) {
    return null; // REST Controller로 진행
}
```

---

### 문제: 정적 자산 404

**원인:** `basePath` 또는 `assetPrefix` 설정 오류

**해결:**
```javascript
// next.config.js
basePath: '/aiprompt-tracker',
assetPrefix: '/aiprompt-tracker/',  // trailing slash 중요!
```

---

### 문제: Dynamic Route Static Export 오류

```
Page "/functions/[functionName]" is missing "generateStaticParams()"
so it cannot be used with "output: export"
```

**원인:** 동적 라우트 `[functionName]`는 빌드 시점에 모든 경로가 확정되어야 함

**해결:** Query param 패턴으로 변경
```tsx
// Before: /functions/[functionName]/page.tsx
// After:  /functions/detail/page.tsx with ?name=X

// Navigation
router.push(`/aiprompt-tracker/functions/detail?name=${encodeURIComponent(name)}`)

// Page
const searchParams = useSearchParams();
const functionName = searchParams.get("name");
```

**참고:** [STATIC_EXPORT_FIX.md](./STATIC_EXPORT_FIX.md)

---

### 문제: Node.js 버전 오류

```
You are using Node.js 18.13.0. For Next.js, Node.js version >= v18.17.0 is required.
```

**해결:**
```bash
# Node.js 업그레이드
nvm install 18.17
nvm use 18.17

# 또는
brew upgrade node
```

---

## 관련 문서

- [DASHBOARD_ARCHITECTURE.md](./DASHBOARD_ARCHITECTURE.md) - Dual UI 전략
- [DASHBOARD_UI_IMPLEMENTATION.md](./DASHBOARD_UI_IMPLEMENTATION.md) - MVP 구현
- [frontend/README.md](./frontend/README.md) - Next.js 개발 가이드

---

## 요약

**Embedded React Dashboard는:**

✅ **기본 UX**: Starter 의존성 추가 → 즉시 UI 사용 가능
✅ **기술 스택**: Next.js static export + Spring Boot static resources
✅ **확장성**: 동일 코드로 standalone 배포도 가능
✅ **라우팅**: SPA fallback (API 제외)
✅ **빌드**: Gradle 통합 또는 수동 스크립트

**이 구조로 "Swagger처럼 쉬운 UI"와 "프로덕션 확장성"을 모두 달성합니다.**
