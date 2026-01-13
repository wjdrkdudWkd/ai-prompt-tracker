# Query Parameter Patterns - Production Guide

## Overview

This document describes the production-ready query parameter handling patterns implemented for the AI Prompt Tracker frontend.

**Problem Solved**: `useSearchParams()` returns nullable types, causing frequent build failures and runtime errors.

**Solution**: Centralized, null-safe query parameter hooks with consistent error UX.

---

## Architecture

```
frontend/
├── lib/hooks/
│   └── useQueryParam.ts          # Core hooks (5 utility functions)
├── components/errors/
│   └── missing-param.tsx         # Error UI components
└── app/functions/detail/
    ├── page.tsx                  # Server Component (Suspense wrapper)
    └── FunctionDetailClient.tsx  # Client Component (uses hooks)
```

**Key Pattern**: Server Component + Suspense + Client Component

---

## Static Export Requirement: Suspense Boundary

### Problem

Next.js static export (`output: 'export'`) requires `useSearchParams()` to be wrapped in a `<Suspense>` boundary:

```
Error: useSearchParams() should be wrapped in a suspense boundary at page /functions/detail
Error occurred prerendering page /functions/detail
```

### Solution: Server Component + Client Component Pattern

**Step 1**: Create Client Component with hooks

```tsx
// app/functions/detail/FunctionDetailClient.tsx
"use client";

import { useRequiredQueryParam } from "@/lib/hooks/useQueryParam";

export default function FunctionDetailClient() {
  const functionName = useRequiredQueryParam("name", {
    autoRedirect: false,
  });

  if (!functionName) {
    return <MissingParamError paramName="name" />;
  }

  return <div>{/* ... */}</div>;
}
```

**Step 2**: Wrap in Server Component with Suspense

```tsx
// app/functions/detail/page.tsx
import { Suspense } from "react";
import FunctionDetailClient from "./FunctionDetailClient";

export default function FunctionDetailPage() {
  return (
    <Suspense
      fallback={
        <div className="flex items-center justify-center min-h-[60vh]">
          <div className="animate-spin rounded-full h-8 w-8 border-4 border-primary border-t-transparent" />
        </div>
      }
    >
      <FunctionDetailClient />
    </Suspense>
  );
}
```

**Why This Works**:
- ✅ Server Component (`page.tsx`) provides Suspense boundary
- ✅ Client Component (`FunctionDetailClient.tsx`) uses `useSearchParams()`
- ✅ Static export succeeds during prerendering
- ✅ Client-side hydration works normally

---

## Core Hooks

### 1. `useOptionalQueryParam(key)` - Optional Parameters

**Use Case**: Parameters that may or may not be present (filters, sorting, etc.)

**Returns**: `string | null`

**Example**:
```tsx
import { useOptionalQueryParam } from "@/lib/hooks/useQueryParam";

function MyPage() {
  const filter = useOptionalQueryParam("filter");
  const sortBy = useOptionalQueryParam("sort");

  // filter and sortBy are string | null
  // Safe to use in conditions
  const filteredData = filter ? data.filter(...) : data;
}
```

**Null-Safety**: Returns `null` during SSR and when param is absent.

---

### 2. `useRequiredQueryParam(key, options)` - Required Parameters

**Use Case**: Parameters that MUST be present (IDs, names, etc.)

**Returns**: `string | null` (null only during redirect)

**Options**:
- `fallbackUrl`: Where to redirect on missing param (default: `/aiprompt-tracker/functions`)
- `autoRedirect`: Auto-redirect vs show error UI (default: `true`)
- `defaultValue`: Default value (overrides redirect)

**Example 1: Auto-Redirect**
```tsx
import { useRequiredQueryParam } from "@/lib/hooks/useQueryParam";

function FunctionDetailPage() {
  // Automatically redirects to /aiprompt-tracker/functions if ?name is missing
  const functionName = useRequiredQueryParam("name");

  if (!functionName) return null; // During redirect

  return <div>Function: {functionName}</div>;
}
```

**Example 2: Error UI**
```tsx
import { useRequiredQueryParam } from "@/lib/hooks/useQueryParam";
import { MissingParamError } from "@/components/errors/missing-param";

function FunctionDetailPage() {
  const functionName = useRequiredQueryParam("name", {
    autoRedirect: false, // Don't auto-redirect
  });

  if (!functionName) {
    return <MissingParamError paramName="name" />;
  }

  return <div>Function: {functionName}</div>;
}
```

**Example 3: Default Value**
```tsx
const page = useRequiredQueryParam("page", {
  defaultValue: "1", // Use "1" if ?page is missing
});

// page is always string, never null
```

---

### 3. `useQueryParams(keys)` - Multiple Parameters

**Use Case**: Reading multiple params at once

**Returns**: `Record<string, string | null>`

**Example**:
```tsx
import { useQueryParams } from "@/lib/hooks/useQueryParam";

function FiltersPage() {
  const { provider, model, status } = useQueryParams([
    "provider",
    "model",
    "status"
  ]);

  // All values are string | null
  const filters = {
    provider: provider || undefined,
    model: model || undefined,
    status: status || undefined,
  };
}
```

---

### 4. `useValidatedQueryParam(key, validator, defaultValue)` - Type-Safe Parameters

**Use Case**: Parameters that need validation/transformation (numbers, dates, enums)

**Returns**: `T` (generic type, always non-null)

**Example: Pagination**
```tsx
import { useValidatedQueryParam } from "@/lib/hooks/useQueryParam";

function MyPage() {
  const page = useValidatedQueryParam(
    "page",
    (val) => {
      const num = parseInt(val, 10);
      return !isNaN(num) && num > 0 ? num : null;
    },
    1 // default
  );

  // page is number (guaranteed)
  // page = 1 if ?page is missing or invalid
}
```

**Example: Enum Validation**
```tsx
type SortOrder = "asc" | "desc";

const sortOrder = useValidatedQueryParam<SortOrder>(
  "order",
  (val) => {
    return val === "asc" || val === "desc" ? val : null;
  },
  "asc"
);

// sortOrder is "asc" | "desc" (guaranteed)
```

**Example: Date Validation**
```tsx
const startDate = useValidatedQueryParam(
  "startDate",
  (val) => {
    const date = new Date(val);
    return !isNaN(date.getTime()) ? date : null;
  },
  new Date()
);

// startDate is Date (guaranteed)
```

---

### 5. `usePaginationParams(defaults)` - Pagination (Common Pattern)

**Use Case**: Page and size parameters for paginated lists

**Returns**: `{ page: number, size: number }`

**Example**:
```tsx
import { usePaginationParams } from "@/lib/hooks/useQueryParam";

function FunctionsList() {
  const { page, size } = usePaginationParams({
    page: 1,
    size: 20,
  });

  // page and size are numbers (guaranteed)
  // Validates: page > 0, size > 0 && size <= 100

  const { data } = useFunctions({ page, size });
}
```

---

## Error UI Components

### `MissingParamError` - Missing Required Parameter

**Props**:
- `paramName`: Parameter name (e.g., "name")
- `backUrl`: URL to navigate back to (default: `/aiprompt-tracker/functions`)
- `backText`: Back link text (default: "Back to Functions")
- `message`: Custom error message (optional)

**Example**:
```tsx
import { MissingParamError } from "@/components/errors/missing-param";

if (!functionName) {
  return (
    <MissingParamError
      paramName="name"
      backUrl="/aiprompt-tracker/functions"
      backText="Back to Functions"
    />
  );
}
```

**UI Output**:
```
┌─────────────────────────────────────────┐
│ ⚠️  Missing Parameter                   │
│     Required query parameter not found  │
├─────────────────────────────────────────┤
│ The required query parameter            │
│ ?name=... is missing from the URL.      │
│                                          │
│ Expected URL format:                    │
│ /aiprompt-tracker/functions/detail      │
│ ?name=value                             │
│                                          │
│ ← Back to Functions                     │
└─────────────────────────────────────────┘
```

---

### `InvalidParamError` - Invalid Parameter Value

**Props**:
- `paramName`: Parameter name
- `paramValue`: Invalid value provided
- `expectedFormat`: Expected format description (optional)
- `backUrl`: URL to navigate back to
- `backText`: Back link text

**Example**:
```tsx
import { InvalidParamError } from "@/components/errors/missing-param";

const executionId = useOptionalQueryParam("executionId");

if (executionId && !isValidUUID(executionId)) {
  return (
    <InvalidParamError
      paramName="executionId"
      paramValue={executionId}
      expectedFormat="UUID format (e.g., 550e8400-e29b-41d4-a716-446655440000)"
    />
  );
}
```

---

## Usage Patterns

### Pattern 1: Required Parameter with Error UI (Recommended)

**Best for**: Detail pages, specific resource views

```tsx
import { useRequiredQueryParam } from "@/lib/hooks/useQueryParam";
import { MissingParamError } from "@/components/errors/missing-param";

export default function FunctionDetailPage() {
  const functionName = useRequiredQueryParam("name", {
    autoRedirect: false,
  });

  if (!functionName) {
    return <MissingParamError paramName="name" />;
  }

  // functionName is guaranteed non-null here
  const { data } = useFunctionDetail(functionName);

  return <div>{/* ... */}</div>;
}
```

**Why This Pattern**:
- ✅ Clear error UX for users
- ✅ No unexpected redirects
- ✅ Type-safe (functionName is string after null check)
- ✅ Static export compatible

---

### Pattern 2: Required Parameter with Auto-Redirect

**Best for**: Navigation flows where redirect is expected

```tsx
import { useRequiredQueryParam } from "@/lib/hooks/useQueryParam";

export default function MyPage() {
  const id = useRequiredQueryParam("id", {
    fallbackUrl: "/aiprompt-tracker/dashboard",
    autoRedirect: true, // default
  });

  if (!id) return null; // Redirecting...

  return <div>{/* ... */}</div>;
}
```

**Why This Pattern**:
- ✅ Silent redirect for better UX in some flows
- ✅ Less UI boilerplate
- ⚠️ User might not understand why they were redirected

---

### Pattern 3: Server + Client Component with Suspense (Required for Static Export)

**Best for**: ALL pages using query parameters in static export mode

**File Structure**:
```
app/functions/detail/
├── page.tsx                  # Server Component (Suspense wrapper)
└── FunctionDetailClient.tsx  # Client Component (uses hooks)
```

**Server Component** (`page.tsx`):
```tsx
import { Suspense } from "react";
import FunctionDetailClient from "./FunctionDetailClient";

export default function FunctionDetailPage() {
  return (
    <Suspense fallback={<LoadingSpinner />}>
      <FunctionDetailClient />
    </Suspense>
  );
}
```

**Client Component** (`FunctionDetailClient.tsx`):
```tsx
"use client";

import { useRequiredQueryParam } from "@/lib/hooks/useQueryParam";
import { MissingParamError } from "@/components/errors/missing-param";

export default function FunctionDetailClient() {
  const functionName = useRequiredQueryParam("name", {
    autoRedirect: false,
  });

  if (!functionName) {
    return <MissingParamError paramName="name" />;
  }

  return <div>Function: {functionName}</div>;
}
```

**Why This Pattern**:
- ✅ Required for Next.js static export (`output: 'export'`)
- ✅ Prevents "useSearchParams should be wrapped in suspense" error
- ✅ Server Component provides Suspense boundary
- ✅ Client Component can use hooks safely
- ✅ Build succeeds without prerendering errors

**Important**: This is the **only supported pattern** for static export with query params.

---

### Pattern 4: Optional Parameter with Defaults

**Best for**: Filters, sorting, pagination

```tsx
import { useOptionalQueryParam } from "@/lib/hooks/useQueryParam";

export default function FunctionsList() {
  const filter = useOptionalQueryParam("filter");
  const sortBy = useOptionalQueryParam("sort") || "name";
  const order = useOptionalQueryParam("order") || "asc";

  // All params have fallback values
  const { data } = useFunctions({
    filter: filter || undefined,
    sortBy,
    order,
  });

  return <div>{/* ... */}</div>;
}
```

---

### Pattern 4: Validated Parameters

**Best for**: Numeric IDs, dates, enums

```tsx
import { useValidatedQueryParam } from "@/lib/hooks/useQueryParam";

export default function ReportPage() {
  const startDate = useValidatedQueryParam(
    "startDate",
    (val) => {
      const date = new Date(val);
      return !isNaN(date.getTime()) ? date : null;
    },
    new Date(Date.now() - 7 * 24 * 60 * 60 * 1000) // 7 days ago
  );

  const limit = useValidatedQueryParam(
    "limit",
    (val) => {
      const num = parseInt(val, 10);
      return num > 0 && num <= 1000 ? num : null;
    },
    100
  );

  // startDate is Date, limit is number (both guaranteed)
  const { data } = useReport({ startDate, limit });
}
```

---

## Migration Guide

### Before (Unsafe)

```tsx
import { useSearchParams } from "next/navigation";

export default function FunctionDetailPage() {
  const searchParams = useSearchParams(); // Nullable!
  const functionName = searchParams.get("name"); // Error if searchParams is null

  if (!functionName) {
    return <div>No function</div>;
  }

  return <div>{functionName}</div>;
}
```

**Problems**:
- ❌ `searchParams` can be null (TypeScript error)
- ❌ No consistent error UX
- ❌ Build fails: "Object is possibly 'null'"

---

### After (Safe)

```tsx
import { useRequiredQueryParam } from "@/lib/hooks/useQueryParam";
import { MissingParamError } from "@/components/errors/missing-param";

export default function FunctionDetailPage() {
  const functionName = useRequiredQueryParam("name", {
    autoRedirect: false,
  });

  if (!functionName) {
    return <MissingParamError paramName="name" />;
  }

  return <div>{functionName}</div>;
}
```

**Improvements**:
- ✅ Null-safe internally
- ✅ Consistent error UI
- ✅ Build succeeds
- ✅ Type-safe

---

## Error UX Policy

### Policy: Show Error UI (Not Auto-Redirect)

**Rationale**: Better user experience for required parameters

**When to use**:
- Detail pages (function detail, execution detail, etc.)
- Forms with pre-filled data from URL
- Any page where missing param is an error state

**Implementation**:
```tsx
const param = useRequiredQueryParam("key", { autoRedirect: false });
if (!param) return <MissingParamError paramName="key" />;
```

**UX Flow**:
1. User accesses `/detail` without `?name=X`
2. Error UI shows: "Missing parameter ?name=..."
3. User clicks "Back to Functions"
4. User navigates back to list

---

### Exception: Auto-Redirect for Navigation Flows

**When to use**:
- Redirect after form submission
- Deep linking with fallback
- Multi-step flows

**Implementation**:
```tsx
const param = useRequiredQueryParam("key", {
  autoRedirect: true,
  fallbackUrl: "/aiprompt-tracker/dashboard",
});
if (!param) return null; // Redirecting...
```

---

## Static Export Compatibility

### Why This Works

1. **Hooks handle SSR/CSR gracefully**
   - `useSearchParams()` returns `null` during SSR
   - Hooks check for `null` internally
   - Return safe defaults or `null`

2. **No build-time dependency**
   - Query params are runtime-only
   - No `generateStaticParams()` needed
   - Works with `output: 'export'`

3. **Type-safe**
   - All hooks return non-null or handle null explicitly
   - No TypeScript errors
   - Build succeeds

---

## Testing

### Test Cases

```tsx
// Test 1: Missing required param
// URL: /functions/detail
// Expected: MissingParamError UI

// Test 2: Valid required param
// URL: /functions/detail?name=myFunc
// Expected: Page renders with functionName="myFunc"

// Test 3: Optional param absent
// URL: /functions
// Expected: filter is null, list shows all

// Test 4: Optional param present
// URL: /functions?filter=ai
// Expected: filter is "ai", list is filtered

// Test 5: Invalid validated param
// URL: /report?limit=abc
// Expected: limit defaults to 100

// Test 6: Valid validated param
// URL: /report?limit=50
// Expected: limit is 50
```

---

## Best Practices

### ✅ DO

- Use `useRequiredQueryParam` for IDs, names, and essential params
- Use `useOptionalQueryParam` for filters, sorting, search
- Use `useValidatedQueryParam` for numbers, dates, enums
- Show `MissingParamError` UI for required params
- Provide sensible defaults for optional params

### ❌ DON'T

- Don't use `useSearchParams()` directly in pages
- Don't ignore null checks (TypeScript will fail)
- Don't auto-redirect without user understanding
- Don't validate in components (use hooks)

---

## Summary

**Files Created**:
- `frontend/lib/hooks/useQueryParam.ts` - 5 production hooks
- `frontend/components/errors/missing-param.tsx` - Error UI components

**Files Updated**:
- `frontend/app/functions/detail/page.tsx` - Uses new hooks

**Benefits**:
- ✅ Null-safe by design
- ✅ Type-safe (no TypeScript errors)
- ✅ Consistent error UX
- ✅ Static export compatible
- ✅ Production-ready patterns
- ✅ Extensible for future params

**Build Status**: ✅ TypeScript-safe, ready for `npm run build`
