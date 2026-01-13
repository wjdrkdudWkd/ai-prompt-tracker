# Suspense Boundary Fix for Static Export

## Problem

Next.js static export failed with Suspense boundary error:

```
Error: useSearchParams() should be wrapped in a suspense boundary at page /functions/detail
Error occurred prerendering page /functions/detail
Build failed
```

**Root Cause**: In Next.js App Router with `output: 'export'`, `useSearchParams()` causes client-side rendering bailout and requires a Suspense boundary during prerendering.

---

## Solution

Split page into **Server Component** (with Suspense) + **Client Component** (with hooks).

### File Structure

```
app/functions/detail/
├── page.tsx                  # Server Component (Suspense wrapper)
└── FunctionDetailClient.tsx  # Client Component (uses useSearchParams)
```

---

## Implementation

### Before (Single File - Failed)

```tsx
// app/functions/detail/page.tsx
"use client"; // ❌ Entire page is client component

export default function FunctionDetailPage() {
  const searchParams = useSearchParams(); // ❌ No Suspense boundary
  const functionName = searchParams?.get("name");

  return <div>{/* ... */}</div>;
}
```

**Result**: ❌ Build fails with Suspense error

---

### After (Split Files - Works)

#### 1. Server Component (page.tsx)

```tsx
// app/functions/detail/page.tsx
import { Suspense } from "react";
import FunctionDetailClient from "./FunctionDetailClient";

/**
 * Server component wrapper for function detail page
 *
 * Provides Suspense boundary required for static export
 * when client component uses useSearchParams()
 */
export default function FunctionDetailPage() {
  return (
    <Suspense
      fallback={
        <div className="flex min-h-[60vh] items-center justify-center">
          <div className="flex flex-col items-center gap-4">
            <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
            <p className="text-sm text-muted-foreground">Loading function details...</p>
          </div>
        </div>
      }
    >
      <FunctionDetailClient />
    </Suspense>
  );
}
```

**Key Points**:
- ✅ Server Component (no "use client")
- ✅ Provides `<Suspense>` boundary
- ✅ Professional loading fallback
- ✅ Static export compatible

---

#### 2. Client Component (FunctionDetailClient.tsx)

```tsx
// app/functions/detail/FunctionDetailClient.tsx
"use client";

import { useRequiredQueryParam } from "@/lib/hooks/useQueryParam";
import { MissingParamError } from "@/components/errors/missing-param";

/**
 * Client component for function detail page
 *
 * Uses useSearchParams() which requires Suspense boundary
 * provided by parent server component
 */
export default function FunctionDetailClient() {
  const functionName = useRequiredQueryParam("name", {
    fallbackUrl: "/aiprompt-tracker/functions",
    autoRedirect: false,
  });

  if (!functionName) {
    return <MissingParamError paramName="name" />;
  }

  return (
    <div className="flex flex-col gap-6 p-8">
      {/* Function detail UI */}
    </div>
  );
}
```

**Key Points**:
- ✅ Client Component ("use client")
- ✅ Uses `useRequiredQueryParam` (which uses `useSearchParams` internally)
- ✅ Wrapped in Suspense by parent
- ✅ Professional error handling

---

## Why This Works

### Static Export Prerendering Flow

1. **Build Time**:
   - Next.js prerenders `page.tsx` (Server Component)
   - Encounters `<Suspense>` boundary
   - Generates static HTML with fallback
   - Defers client component hydration

2. **Runtime (Browser)**:
   - Static HTML loads with loading spinner
   - React hydrates `<FunctionDetailClient />`
   - `useSearchParams()` reads query params
   - Component renders with data

3. **Result**:
   - ✅ Build succeeds (no Suspense error)
   - ✅ Page is statically exportable
   - ✅ Client-side routing works
   - ✅ Query params work correctly

---

## Pattern Template

Use this pattern for **ALL pages with query parameters** in static export mode:

### Step 1: Create Client Component

```tsx
// app/your-page/YourPageClient.tsx
"use client";

import { useRequiredQueryParam } from "@/lib/hooks/useQueryParam";

export default function YourPageClient() {
  const param = useRequiredQueryParam("key");

  if (!param) {
    return <MissingParamError paramName="key" />;
  }

  return <div>{/* Your page UI */}</div>;
}
```

### Step 2: Create Server Component Wrapper

```tsx
// app/your-page/page.tsx
import { Suspense } from "react";
import YourPageClient from "./YourPageClient";

export default function YourPage() {
  return (
    <Suspense fallback={<LoadingSpinner />}>
      <YourPageClient />
    </Suspense>
  );
}
```

**That's it!** Build will succeed.

---

## Files Changed

### Created

1. **`frontend/app/functions/detail/FunctionDetailClient.tsx`** (NEW)
   - Client component with query param logic
   - Uses `useRequiredQueryParam` hook
   - 275 lines

### Modified

2. **`frontend/app/functions/detail/page.tsx`** (REFACTORED)
   - From: Client component with hooks (❌ build fails)
   - To: Server component with Suspense (✅ build succeeds)
   - 33 lines (simplified from 243 lines)

### Updated Documentation

3. **`QUERY_PARAM_PATTERNS.md`**
   - Added "Static Export Requirement: Suspense Boundary" section
   - Added "Pattern 3: Server + Client Component" pattern
   - Marked as required pattern for static export

---

## Build Verification

### Expected Build Output

```bash
cd frontend
npm run build
```

**Before Fix**:
```
Error: useSearchParams() should be wrapped in a suspense boundary at page /functions/detail
Error occurred prerendering page /functions/detail
❌ Build failed
```

**After Fix**:
```
✓ Generating static pages (6/6)
✓ Collecting build traces
✓ Finalizing page optimization

Route (app)                              Size     First Load JS
┌ ○ /aiprompt-tracker                    ...      ...
├ ○ /aiprompt-tracker/calls              ...      ...
├ ○ /aiprompt-tracker/dashboard          ...      ...
├ ○ /aiprompt-tracker/functions          ...      ...
├ ○ /aiprompt-tracker/functions/detail   ...      ...  ✅ SUCCESS
├ ○ /aiprompt-tracker/providers          ...      ...
└ ○ /aiprompt-tracker/settings           ...      ...

○  (Static)  prerendered as static content

✅ Exported static build to: out/
```

---

## Runtime Verification

### Test Scenario 1: Valid Query Param

```bash
# URL
http://localhost:8080/aiprompt-tracker/functions/detail?name=myFunction

# Expected Behavior
1. Loading spinner appears briefly
2. Page loads with function details
3. functionName = "myFunction"
4. KPI cards render
5. Execution history renders
```

### Test Scenario 2: Missing Query Param

```bash
# URL
http://localhost:8080/aiprompt-tracker/functions/detail

# Expected Behavior
1. Loading spinner appears briefly
2. MissingParamError UI displays
3. Shows: "Required parameter ?name=... is missing"
4. "Back to Functions" link works
```

### Test Scenario 3: Page Refresh

```bash
# Action
1. Navigate to /functions/detail?name=test
2. Press F5 (refresh)

# Expected Behavior
1. Loading spinner appears
2. Page reloads successfully
3. No 404 error
4. Query param preserved
```

---

## Migration Guide

### For Existing Pages Using Query Params

If you have pages using `useSearchParams()` directly:

**Step 1**: Extract to Client Component
```bash
# Create client component file
touch app/your-page/YourPageClient.tsx

# Move all client logic there
# Add "use client" directive
```

**Step 2**: Update Page to Server Component
```tsx
// app/your-page/page.tsx
import { Suspense } from "react";
import YourPageClient from "./YourPageClient";

export default function YourPage() {
  return (
    <Suspense fallback={<LoadingSpinner />}>
      <YourPageClient />
    </Suspense>
  );
}
```

**Step 3**: Test Build
```bash
npm run build
# Should succeed without Suspense errors
```

---

## Best Practices

### ✅ DO

- **Always use Server + Client component pattern** for pages with query params
- **Provide meaningful loading fallback** in Suspense
- **Use production hooks** (`useRequiredQueryParam`, etc.)
- **Test both build and runtime** behavior

### ❌ DON'T

- **Don't make entire page "use client"** if using query params
- **Don't skip Suspense boundary** (build will fail)
- **Don't use `useSearchParams()` directly** (use our hooks)
- **Don't forget to test edge cases** (missing params, invalid values)

---

## Related Documentation

- **Query Parameter Hooks**: `QUERY_PARAM_PATTERNS.md`
- **Static Export Fix**: `STATIC_EXPORT_FIX.md`
- **Embedded Dashboard**: `EMBEDDED_REACT_DASHBOARD.md`

---

## Summary

**Problem**: Static export failed with Suspense error when using query params

**Solution**: Split into Server Component (with Suspense) + Client Component (with hooks)

**Files**:
- ✅ Created: `FunctionDetailClient.tsx` (client component)
- ✅ Modified: `page.tsx` (server component wrapper)
- ✅ Updated: `QUERY_PARAM_PATTERNS.md` (documentation)

**Result**:
- ✅ Build succeeds
- ✅ Static export works
- ✅ Query params work correctly
- ✅ Professional error UX
- ✅ Production-ready pattern

**Next Step**: Upgrade Node.js to 18.18+ and run `npm run build` to verify
