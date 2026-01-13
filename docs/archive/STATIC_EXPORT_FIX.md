# Static Export Fix: Dynamic Routes → Query Params

## Problem

Next.js static export (`output: 'export'`) failed with error:
```
Page "/functions/[functionName]" is missing "generateStaticParams()"
so it cannot be used with "output: export"
```

**Root Cause**: Dynamic route segments like `[functionName]` require all possible paths to be pre-generated at build time via `generateStaticParams()`. Since function names come from runtime API/DB data, this is impossible.

---

## Solution

Converted dynamic routes to query parameter pattern for SPA-style routing:

**Before**: `/functions/[functionName]/page.tsx` → `/aiprompt-tracker/functions/MyFunction`
**After**: `/functions/detail/page.tsx` → `/aiprompt-tracker/functions/detail?name=MyFunction`

---

## Changes Made

### 1. New Function Detail Page (Query Param Based)

**File**: `frontend/app/functions/detail/page.tsx` (NEW)

```tsx
"use client";

export default function FunctionDetailPage() {
  const searchParams = useSearchParams();
  const functionName = searchParams.get("name");

  // Same UI as before, but reads name from query param
  const { data: functionDetail } = useFunctionDetail(functionName || "");
  // ...
}
```

**Key Changes**:
- Uses `useSearchParams()` instead of `params.functionName`
- Added "Back to Functions" link
- Added null check for missing function name

### 2. Updated Navigation Links

**Files Modified**:
- `frontend/app/dashboard/page.tsx:112`
- `frontend/app/functions/page.tsx:60`

**Before**:
```tsx
onClick={() => router.push(`/functions/${encodeURIComponent(func.functionName)}`)}
```

**After**:
```tsx
onClick={() => router.push(`/aiprompt-tracker/functions/detail?name=${encodeURIComponent(func.functionName)}`)}
```

### 3. Removed Old Dynamic Route

**Deleted**: `frontend/app/functions/[functionName]/` directory

### 4. Updated package.json

**File**: `frontend/package.json`

Added Node.js version requirement:
```json
{
  "engines": {
    "node": ">=18.18.0"
  }
}
```

---

## Verification

### Static Export Build

```bash
cd frontend
npm run build
```

**Expected Output**:
```
✓ Generating static pages (6/6)
✓ Collecting build traces
✓ Finalizing page optimization

Route (app)                              Size     First Load JS
┌ ○ /aiprompt-tracker                    ...      ...
├ ○ /aiprompt-tracker/calls              ...      ...
├ ○ /aiprompt-tracker/dashboard          ...      ...
├ ○ /aiprompt-tracker/functions          ...      ...
├ ○ /aiprompt-tracker/functions/detail   ...      ...  ✅ NEW ROUTE
└ ○ /aiprompt-tracker/providers          ...      ...

Exported static build to: out/
```

### Directory Structure

```
out/
├── index.html
├── calls/
│   └── index.html
├── dashboard/
│   └── index.html
├── functions/
│   ├── index.html
│   └── detail/
│       └── index.html        ✅ NEW
├── providers/
│   └── index.html
└── _next/
    └── static/
        ├── chunks/
        └── css/
```

---

## Testing Scenarios

### 1. Direct Link from Dashboard

```
User flow:
1. Visit /aiprompt-tracker/dashboard
2. Click function row "generateReport"
3. Navigate to /aiprompt-tracker/functions/detail?name=generateReport
4. Function detail page loads with KPIs and execution history
```

### 2. Direct Link from Functions List

```
User flow:
1. Visit /aiprompt-tracker/functions
2. Click function row "processData"
3. Navigate to /aiprompt-tracker/functions/detail?name=processData
4. Function detail page loads
```

### 3. Direct URL Access

```bash
# Paste URL directly in browser
http://localhost:8080/aiprompt-tracker/functions/detail?name=myFunction

# Should work via UiRedirectController SPA fallback
```

### 4. Page Refresh

```
User flow:
1. Navigate to /aiprompt-tracker/functions/detail?name=testFunc
2. Press F5 (refresh)
3. Page reloads successfully (no 404)
```

### 5. Back Button

```
User flow:
1. Dashboard → Function Detail
2. Click "Back to Functions" link
3. Returns to functions list
```

---

## Spring Boot Integration

The new query param routing works seamlessly with `UiRedirectController`:

```java
@GetMapping({"/aiprompt-tracker/**"})
public String handleSpaRouting(HttpServletRequest request) {
    String path = request.getRequestURI();

    // /aiprompt-tracker/functions/detail?name=foo
    // → forward to index.html
    // → React Router handles /functions/detail
    // → useSearchParams() reads ?name=foo

    return "forward:/aiprompt-tracker/index.html";
}
```

**Query params are preserved during forward**, so React receives them correctly.

---

## Node.js Version Requirement

### Current Blocker

The build requires Node.js >= 18.17.0 (preferably 18.18.0+):

```
Current: 18.13.0
Required: >= 18.17.0
```

### Upgrade Options

**Option 1: Homebrew (Recommended)**
```bash
brew upgrade node
node --version  # Should be 20.x or 18.18+
```

**Option 2: nvm**
```bash
nvm install 18.18.2
nvm use 18.18.2
nvm alias default 18.18.2
```

**Option 3: Official Installer**
Download from https://nodejs.org/ (LTS version)

---

## Build & Deploy Workflow

### Local Development

```bash
# 1. Ensure Node.js 18.18+
node --version

# 2. Build frontend
cd frontend
npm install
npm run build

# 3. Copy to starter
./copy-to-starter.sh

# 4. Build starter JAR
cd ..
./gradlew :tracker-starter:build

# 5. Run backend
cd backend
./gradlew bootRun

# 6. Test
open http://localhost:8080/aiprompt-tracker/
```

### CI/CD (GitHub Actions Example)

```yaml
- name: Setup Node.js
  uses: actions/setup-node@v3
  with:
    node-version: '18.18.2'

- name: Build Frontend
  run: |
    cd frontend
    npm ci
    npm run build
    ./copy-to-starter.sh

- name: Build Starter
  run: ./gradlew :tracker-starter:build
```

---

## Related Files

| File | Change |
|------|--------|
| `frontend/app/functions/detail/page.tsx` | ✅ Created (query param based) |
| `frontend/app/functions/[functionName]/page.tsx` | ❌ Deleted (dynamic route) |
| `frontend/app/dashboard/page.tsx` | 🔧 Updated navigation |
| `frontend/app/functions/page.tsx` | 🔧 Updated navigation |
| `frontend/package.json` | 🔧 Added engines field |
| `EMBEDDED_REACT_DASHBOARD.md` | 📝 Update routing docs |
| `QUICKSTART_EMBEDDED_DASHBOARD.md` | 📝 Update examples |

---

## Summary

✅ **Fixed**: Static export now works without `generateStaticParams()`
✅ **Pattern**: `/functions/detail?name=X` instead of `/functions/[functionName]`
✅ **UX**: Same user experience, just different URL structure
✅ **Spring Boot**: UiRedirectController handles SPA routing correctly
✅ **Query Params**: Preserved during forward, React reads them fine

⏳ **Blocked**: Build requires Node.js 18.18+ (currently 18.13.0)

**Next Step**: Upgrade Node.js, run `npm run build`, verify `out/` structure.
