# Quick Start: Embedded React Dashboard

## For Consumers (Zero-Config)

Just add the starter dependency and run:

```gradle
dependencies {
    implementation("com.galoong:ai-prompt-tracker-starter:1.0.0")
}
```

Then access the React dashboard at:
```
http://localhost:8080/aiprompt-tracker/
```

✅ **That's it!** The React dashboard is already embedded in the starter JAR.

---

## For Contributors (Building from Source)

### Prerequisites

- Node.js 18.17+
- Java 17+
- Gradle 8+

### Quick Build

```bash
# 1. Build React dashboard
cd frontend
npm install
npm run build

# 2. Copy to starter resources
./copy-to-starter.sh

# 3. Build starter JAR
cd ..
./gradlew :tracker-starter:build

# 4. Test with backend
cd backend
./gradlew bootRun

# 5. Access
open http://localhost:8080/aiprompt-tracker/
```

### Using Gradle Tasks (Alternative)

```bash
# Build frontend + copy in one step
./gradlew :tracker-starter:buildFrontend :tracker-starter:copyFrontend

# Then build starter
./gradlew :tracker-starter:build
```

**Note:** Gradle tasks gracefully skip if Node.js is unavailable.

---

## Verification Checklist

After building, verify these scenarios:

- ✅ **Root access**: `http://localhost:8080/aiprompt-tracker/` → React UI loads
- ✅ **Direct route**: `http://localhost:8080/aiprompt-tracker/dashboard/` → Dashboard page
- ✅ **API access**: `http://localhost:8080/aiprompt-tracker/api/dashboard/summary` → JSON response
- ✅ **Refresh**: F5 on `/aiprompt-tracker/functions/` → No 404
- ✅ **Static assets**: `http://localhost:8080/aiprompt-tracker/_next/static/...` → JS/CSS loaded

---

## Architecture Overview

```
Spring Boot JAR
├── Java Classes (tracking, API)
└── META-INF/resources/aiprompt-tracker/
    ├── index.html           ← React SPA entry
    ├── dashboard/index.html ← Pre-rendered route
    ├── functions/index.html ← Pre-rendered route
    ├── _next/static/        ← JS/CSS bundles
    └── dashboard-mvp.html   ← Vanilla fallback
```

**Routing Logic:**
1. API requests (`/api/**`) → REST Controllers (NOT intercepted)
2. Static files (`.js`, `.css`) → Spring Boot static resources
3. Everything else → Forward to `index.html` (SPA fallback)

---

## Troubleshooting

### Node.js version error

```
You are using Node.js 18.13.0. For Next.js, Node.js version >= v18.17.0 is required.
```

**Fix:**
```bash
nvm install 18.17
nvm use 18.17
```

### Dashboard shows 404

**Cause:** React build not copied to starter resources

**Fix:**
```bash
cd frontend
npm run build
./copy-to-starter.sh
cd ..
./gradlew :tracker-starter:build
```

### API calls fail

**Cause:** SPA fallback intercepting API requests

**Fix:** Check `UiRedirectController.java` excludes `/api/**`:
```java
if (path.startsWith("/aiprompt-tracker/api/")) {
    return null; // ✅ Must NOT forward API requests
}
```

---

## Deployment Modes

### Mode 1: Embedded (Default)

```
Consumer adds starter → React UI included in JAR
No separate deployment needed
```

**Pros:** Zero-config, same-origin API calls
**Cons:** UI updates require JAR rebuild

### Mode 2: Standalone (Optional)

```
Deploy frontend separately (Vercel, Nginx, Docker)
Set NEXT_PUBLIC_API_BASE_URL=https://backend.com
```

**Pros:** Independent UI deployment, CDN support
**Cons:** CORS setup required, infrastructure complexity

### Mode 3: Hybrid (Recommended)

```
Embedded for dev/quick checks
Standalone for production analytics
```

---

## Key Files

| File | Purpose |
|------|---------|
| `frontend/next.config.js` | Next.js export config (`basePath`, `trailingSlash`) |
| `frontend/lib/api/client.ts` | API client (same-origin vs CORS) |
| `UiRedirectController.java` | SPA fallback routing |
| `frontend/copy-to-starter.sh` | Manual copy script |
| `tracker-starter/build.gradle.kts` | Gradle build tasks |

---

## Documentation

- **Comprehensive Guide:** [EMBEDDED_REACT_DASHBOARD.md](./EMBEDDED_REACT_DASHBOARD.md)
- **Architecture:** [DASHBOARD_ARCHITECTURE.md](./DASHBOARD_ARCHITECTURE.md)
- **Frontend README:** [frontend/README.md](./frontend/README.md)

---

## Summary

**Embedded React Dashboard achieves:**

✅ **Swagger-like UX**: Add dependency → UI immediately available
✅ **Zero-config**: No frontend build required for consumers
✅ **Dual deployment**: Same code for embedded AND standalone
✅ **API protection**: `/api/**` never intercepted by frontend routing
✅ **Production-ready**: Next.js + shadcn/ui + TypeScript

**This is the best of both worlds: simplicity for consumers, flexibility for power users.**
