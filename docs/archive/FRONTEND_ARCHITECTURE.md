# AI Prompt Tracker - Frontend Architecture

## Tech Stack

- **Framework**: Next.js 14 (App Router)
- **Language**: TypeScript
- **Styling**: Tailwind CSS
- **UI Components**: shadcn/ui (Radix UI primitives)
- **Data Fetching**: TanStack Query (React Query)
- **State Management**: URL query params + React Query cache
- **Icons**: Lucide React

## Page/Component Tree

```
app/
├── layout.tsx                    # Root layout with sidebar nav
├── page.tsx                      # Redirect to /dashboard
├── dashboard/
│   └── page.tsx                  # Dashboard with KPI cards + functions table
├── functions/
│   ├── page.tsx                  # Functions list with advanced filters
│   └── [functionName]/
│       └── page.tsx              # Function detail with execution history
├── calls/
│   └── page.tsx                  # Calls explorer with advanced filters
└── providers/
    └── page.tsx                  # Provider comparison and breakdown

components/
├── layout/
│   ├── app-sidebar.tsx           # Left navigation sidebar
│   ├── top-header.tsx            # Global filters (env, time range, search)
│   └── page-container.tsx        # Common page wrapper
├── dashboard/
│   ├── kpi-card.tsx              # Metric card component
│   ├── provider-comparison.tsx   # Provider comparison section
│   └── functions-table.tsx       # Functions table
├── functions/
│   ├── functions-filter.tsx      # Advanced filters panel
│   ├── function-card.tsx         # Function overview cards
│   ├── provider-breakdown.tsx    # Provider/model breakdown table
│   └── executions-table.tsx      # Execution history table
├── executions/
│   ├── execution-drawer.tsx      # Right panel drawer for execution detail
│   ├── execution-summary.tsx     # Execution summary card
│   └── call-timeline.tsx         # Call timeline list
├── calls/
│   ├── calls-filter.tsx          # Advanced filters panel
│   ├── calls-table.tsx           # Calls table
│   └── call-detail-dialog.tsx    # Call detail modal
├── providers/
│   ├── provider-card.tsx         # Provider overview card
│   └── provider-model-table.tsx  # Provider & model breakdown table
└── ui/
    ├── button.tsx                # shadcn/ui components
    ├── card.tsx
    ├── table.tsx
    ├── dialog.tsx
    ├── drawer.tsx
    ├── input.tsx
    ├── select.tsx
    ├── badge.tsx
    └── ... (other shadcn/ui primitives)

lib/
├── api/
│   ├── client.ts                 # Base API client (fetch wrapper)
│   ├── dashboard.ts              # Dashboard API calls
│   ├── functions.ts              # Functions API calls
│   ├── executions.ts             # Executions API calls
│   └── calls.ts                  # Calls API calls
├── hooks/
│   ├── use-dashboard.ts          # React Query hooks for dashboard
│   ├── use-functions.ts          # React Query hooks for functions
│   ├── use-executions.ts         # React Query hooks for executions
│   └── use-calls.ts              # React Query hooks for calls
├── types/
│   ├── api.ts                    # TypeScript types from DTOs
│   └── filters.ts                # Filter types
└── utils/
    ├── format.ts                 # Formatting utilities (currency, number, date)
    ├── filters.ts                # URL query param utilities
    └── cn.ts                     # Class name utility

```

## Data Flow

### URL Query Params → API → React Query → UI

1. **URL State**: Filters stored in URL query params
   - `/dashboard?from=2024-01-01&to=2024-01-31&env=PROD`
   - Enables shareable links and browser back/forward

2. **API Client**: Fetch wrapper with type safety
   - Base URL from environment variable
   - Error handling and response parsing
   - TypeScript types from backend DTOs

3. **React Query**: Automatic caching and refetching
   - Cache invalidation on mutations
   - Loading and error states
   - Pagination support

4. **UI Components**: Consume hooks
   - Display loading spinners
   - Show error messages
   - Render empty states

## API → TypeScript Type Mapping

```typescript
// From DashboardSummaryResponse.java
interface DashboardSummary {
  totalCost: number;
  totalExecutions: number;
  totalCalls: number;
  avgLatencyMs: number;
  avgCallsPerExecution: number;
  executionErrorRate: number;
  callErrorRate: number;
}

// From FunctionAggregateResponse.java
interface FunctionAggregate {
  functionName: string;
  category: string;
  tags: string[];
  executions: number;
  calls: number;
  callsPerExecution: number;
  totalCost: number;
  avgExecutionTimeMs: number;
  errorRate: number;
}

// From FunctionDetailResponse.java
interface FunctionDetail {
  functionName: string;
  category: string;
  tags: string[];
  totalExecutions: number;
  totalCalls: number;
  totalCost: number;
  avgExecutionTimeMs: number;
  errorRate: number;
  topProviders: ProviderBreakdown[];
  topModels: ModelBreakdown[];
}

// From ExecutionDetailResponse.java
interface ExecutionDetail {
  executionId: string;
  functionName: string;
  category: string;
  tags: string[];
  environment: string;
  startedAt: string; // ISO timestamp
  finishedAt: string;
  durationMs: number;
  status: string;
  errorMessage?: string;
  callsCount: number;
  totalTokens: number;
  totalCost: number;
  calls: CallDetail[];
}

// From CallResponse.java
interface Call {
  callId: string;
  executionId: string;
  functionName: string;
  provider: string;
  model: string;
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
  cost: number;
  latencyMs: number;
  status: string;
  errorType?: string;
  errorMessage?: string;
  wasTruncated: boolean;
  requestPreview?: string;
  responsePreview?: string;
  createdAt: string;
}
```

## Key UX Patterns

### 1. Dashboard Page
- **Layout**: KPI cards (2x3 grid) → Provider comparison cards → Functions table
- **Filters**: Top header (env, time range) + Swagger-style filter panel (collapsible)
- **Interaction**: Click function row → Navigate to function detail page

### 2. Function Detail Page
- **Layout**: Header (function name, tags) → KPI cards → Provider/Model breakdown → Executions table
- **Interaction**: Click execution row → Open execution drawer (right panel)

### 3. Execution Drawer (Critical Component)
- **Trigger**: Click execution row in table
- **Layout**: Right-side panel (60% width)
  - Top: Execution summary (duration, status, cost, tokens)
  - Bottom: Call timeline (chronological list)
  - Each call: Expandable to show request/response previews
- **Data**: Loads `GET /api/executions/{executionId}` on open
- **Close**: Click outside or close button

### 4. Calls Explorer Page
- **Layout**: Advanced filters (Swagger-style) → Calls table with pagination
- **Filters**: Provider, model, status, date range, search
- **Interaction**: Click call row → Open call detail dialog

### 5. Providers Page
- **Layout**: Provider overview cards → Provider & Model breakdown table
- **Filters**: Time range, environment, function
- **Data**: Aggregated from calls data

## Filter State Management

### URL Query Params Pattern

```typescript
// Example: /dashboard?from=2024-01-01&to=2024-01-31&env=PROD&provider=OpenAI

// Read from URL
const searchParams = useSearchParams();
const filters = {
  from: searchParams.get('from'),
  to: searchParams.get('to'),
  env: searchParams.get('env'),
  provider: searchParams.get('provider'),
};

// Write to URL
const router = useRouter();
const pathname = usePathname();
const updateFilters = (newFilters: Partial<Filters>) => {
  const params = new URLSearchParams(searchParams);
  Object.entries(newFilters).forEach(([key, value]) => {
    if (value) params.set(key, value);
    else params.delete(key);
  });
  router.push(`${pathname}?${params.toString()}`);
};
```

### React Query Integration

```typescript
// Hook automatically refetches when URL params change
const { data, isLoading, error } = useDashboardSummary({
  from: searchParams.get('from'),
  to: searchParams.get('to'),
  env: searchParams.get('env'),
});
```

## Responsive Design

- **Desktop (>1024px)**: Full layout with sidebar + main content
- **Tablet (768-1024px)**: Collapsible sidebar, cards stack 2x2
- **Mobile (<768px)**: Hidden sidebar (hamburger menu), cards stack 1x1

## Loading/Error/Empty States

### Loading
- Skeleton loaders for cards and tables
- Spinner for async operations

### Error
- Error boundary for page-level errors
- Inline error messages for API failures
- Retry button

### Empty
- No data illustration + message
- Suggestions (e.g., "Try changing filters" or "No executions found for this function")

## Environment Configuration

```env
# .env.local
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

## Navigation Structure

```
Sidebar:
- Dashboard (/)
- Functions (/functions)
- Calls (/calls)
- Providers (/providers)
- Settings (/settings) [placeholder]

Top Header:
- Global search (functions, providers, models)
- Environment filter (DEV/TEST/PROD/All)
- Time range filter (7d/30d/90d/custom)
- Refresh indicator
```

## Implementation Priority

1. **Phase 1: Core Structure** ✅
   - Next.js project setup
   - API client + types
   - Layout components (sidebar, header)

2. **Phase 2: Dashboard** ✅
   - KPI cards
   - Functions table
   - Basic filters

3. **Phase 3: Function Detail + Execution Drawer** ✅
   - Function detail page
   - Execution drawer component
   - Call timeline

4. **Phase 4: Calls Explorer** ✅
   - Calls list page
   - Advanced filters
   - Pagination

5. **Phase 5: Providers** ✅
   - Provider comparison
   - Provider/model breakdown

6. **Phase 6: Polish** ✅
   - Loading states
   - Error boundaries
   - Empty states
   - Responsive design

## Key Design Decisions

1. **Swagger-like Filters**: Collapsible filter panels at top of each page
2. **Card + Table Hybrid**: KPI cards for metrics, tables for detailed data
3. **Execution Drawer**: Right-side panel (not modal) for better UX
4. **URL State**: All filters in URL for shareable links
5. **React Query**: Automatic caching and refetching
6. **TypeScript**: Full type safety from backend DTOs
7. **shadcn/ui**: Composable, accessible components

## Security Considerations

1. **Raw JSON Display**: Only show `requestPreview`/`responsePreview` in DEV/TEST
2. **API Keys**: Never expose in frontend
3. **CORS**: Backend must allow frontend origin
4. **Environment Detection**: Check `environment` field from execution data
