# AI Prompt Tracker - Frontend

A modern, type-safe admin dashboard for monitoring and analyzing AI API usage across your system.

## Overview

This Next.js application provides a comprehensive UI for visualizing:
- **Function Executions**: Track `@AIPrompt` annotated method invocations
- **AI API Calls**: Monitor HTTP calls to AI providers (OpenAI, Anthropic, Google, etc.)
- **Provider Analytics**: Compare performance and costs across providers
- **Call Timeline**: Drill down into individual execution details

## Tech Stack

- **Framework**: Next.js 14 (App Router)
- **Language**: TypeScript
- **Styling**: Tailwind CSS
- **UI Components**: shadcn/ui (Radix UI primitives)
- **Data Fetching**: TanStack Query (React Query)
- **State Management**: URL query params + React Query cache
- **Icons**: Lucide React

## Features

### ✅ Implemented

- **Dashboard Page**
  - KPI cards (cost, executions, calls, latency, error rate)
  - Functions table with click-through navigation
  - Real-time data loading with React Query

- **Functions List Page**
  - Comprehensive table of all AI-integrated functions
  - Sortable columns
  - Click-to-detail navigation

- **Function Detail Page**
  - Function-level KPI cards
  - Provider/model breakdown
  - Execution history table
  - Integration with Execution Drawer

- **Execution Drawer** (Critical Component)
  - Right-side panel (slides from right)
  - Execution summary (duration, status, cost, tokens)
  - Call timeline (chronological list of AI calls)
  - Expandable call details (request/response previews)
  - wasTruncated badge support

- **Calls Explorer Page**
  - Full table of all AI API calls
  - Provider/model/status filtering
  - Token breakdown (prompt/completion/total)

- **Providers Page**
  - Provider comparison cards
  - Provider & model breakdown table
  - Aggregated statistics from calls data

- **Loading States**
  - Skeleton loaders for cards and tables
  - React Query automatic loading indicators

- **Error States**
  - API error handling
  - Empty state messages

- **Type Safety**
  - Full TypeScript coverage
  - Types mapped from backend DTOs
  - End-to-end type safety

## Project Structure

```
frontend/
├── app/                          # Next.js App Router pages
│   ├── dashboard/page.tsx        # Dashboard with KPI cards
│   ├── functions/
│   │   ├── page.tsx              # Functions list
│   │   └── [functionName]/page.tsx  # Function detail
│   ├── calls/page.tsx            # Calls explorer
│   ├── providers/page.tsx        # Provider comparison
│   ├── settings/page.tsx         # Settings (placeholder)
│   ├── layout.tsx                # Root layout with sidebar
│   ├── page.tsx                  # Redirect to /dashboard
│   ├── providers.tsx             # React Query provider
│   └── globals.css               # Global styles
├── components/
│   ├── layout/
│   │   └── app-sidebar.tsx       # Left navigation sidebar
│   ├── dashboard/
│   │   └── kpi-card.tsx          # Metric card component
│   ├── executions/
│   │   └── execution-drawer.tsx  # Right panel drawer
│   └── ui/                       # shadcn/ui components
│       ├── button.tsx
│       ├── card.tsx
│       ├── badge.tsx
│       └── drawer.tsx
├── lib/
│   ├── api/                      # API client functions
│   │   ├── client.ts             # Base fetch wrapper
│   │   ├── dashboard.ts          # Dashboard endpoints
│   │   ├── functions.ts          # Functions endpoints
│   │   ├── executions.ts         # Executions endpoints
│   │   └── calls.ts              # Calls endpoints
│   ├── hooks/                    # React Query hooks
│   │   ├── use-dashboard.ts
│   │   ├── use-functions.ts
│   │   ├── use-executions.ts
│   │   └── use-calls.ts
│   ├── types/
│   │   └── api.ts                # TypeScript types from DTOs
│   └── utils/
│       ├── cn.ts                 # Class name utility
│       ├── format.ts             # Formatting utilities
│       └── filters.ts            # URL query param utilities
├── package.json
├── tsconfig.json
├── tailwind.config.ts
├── next.config.js
└── README.md
```

## Getting Started

### Prerequisites

- Node.js 18+ and npm
- Backend API running on `http://localhost:8080` (or configure `NEXT_PUBLIC_API_BASE_URL`)

### Installation

1. **Install dependencies**

```bash
cd frontend
npm install
```

2. **Configure environment**

Create `.env.local` file:

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

3. **Run development server**

```bash
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) in your browser.

### Build for Production

```bash
npm run build
npm start
```

## API Integration

### Backend Endpoints Used

```typescript
// Dashboard
GET /api/dashboard/summary?from=&to=&env=

// Functions
GET /api/functions?from=&to=&env=&category=&status=&q=&page=&size=
GET /api/functions/{functionName}
GET /api/functions/{functionName}/executions?from=&to=&page=&size=

// Executions
GET /api/executions/{executionId}

// Calls
GET /api/calls?provider=&model=&status=&from=&to=&page=&size=
```

### Type Safety

All API responses are typed using interfaces mapped from backend DTOs:

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

// From ExecutionDetailResponse.java
interface ExecutionDetail {
  executionId: string;
  functionName: string;
  category: string;
  tags: string[];
  environment: string;
  startedAt: string;
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

## React Query Integration

All data fetching uses React Query for automatic caching, refetching, and loading states:

```typescript
// Example usage
const { data, isLoading, error } = useDashboardSummary({
  from: '2024-01-01',
  to: '2024-01-31',
  env: 'PROD'
});
```

### Query Keys

```typescript
["dashboard", "summary", filters]
["functions", filters]
["functions", functionName]
["functions", functionName, "executions", filters]
["executions", executionId]
["calls", filters]
```

## UI Components

### KPI Card

```typescript
<KPICard
  title="Total Cost"
  value={formatCurrency(summary?.totalCost)}
  icon={DollarSign}
  loading={summaryLoading}
/>
```

### Execution Drawer

```typescript
<ExecutionDrawer
  executionId={selectedExecutionId}
  open={drawerOpen}
  onOpenChange={setDrawerOpen}
/>
```

## Formatting Utilities

```typescript
formatCurrency(0.0124)      // "$0.0124"
formatNumber(12847)         // "12,847"
formatPercentage(0.024)     // "2.4%"
formatLatency(847)          // "847ms"
formatDuration(2847)        // "2.8s"
formatDateTime(isoString)   // "Jan 15, 2024 14:32:07"
formatTokens(12847)         // "12.8K"
```

## Navigation Flow

```
Dashboard
  └─> Click Function Row
      └─> Function Detail Page
          └─> Click Execution Row
              └─> Execution Drawer (right panel)
                  └─> Expand Call
                      └─> View Request/Response Previews
```

## Key UX Patterns

### 1. Execution Drawer
- **Trigger**: Click execution row in function detail table
- **Layout**: Right-side panel (60% width)
- **Content**: Execution summary + call timeline
- **Interaction**: Click call to expand request/response previews

### 2. Loading States
- Skeleton loaders for cards (pulsing gray rectangles)
- Skeleton loaders for table rows
- React Query automatic loading indicators

### 3. Empty States
- "No data found" messages in tables
- Suggestions for changing filters (future enhancement)

### 4. Error States
- API error messages inline
- React Query error boundaries
- Retry functionality (future enhancement)

## Future Enhancements

### URL Query Params (Swagger-style Filters)
- Sync filters to URL for shareable links
- Browser back/forward support
- Example: `/dashboard?from=2024-01-01&to=2024-01-31&env=PROD`

### Advanced Filters
- Collapsible filter panels
- Date range picker
- Provider/model/category dropdowns
- Search input

### Pagination
- Table pagination controls
- Page size selector
- URL-synced page state

### Charts & Visualizations
- Cost trends over time
- Provider comparison charts
- Error rate trends
- Token usage distribution

### Responsive Design
- Mobile-optimized layouts
- Collapsible sidebar
- Touch-friendly interactions

## Troubleshooting

### Backend Not Running

**Error**: `Network error: Failed to fetch`

**Solution**: Ensure backend is running on `http://localhost:8080` or update `NEXT_PUBLIC_API_BASE_URL` in `.env.local`

### CORS Errors

**Error**: `CORS policy: No 'Access-Control-Allow-Origin' header`

**Solution**: Configure backend to allow frontend origin:

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

### Data Not Loading

1. Check Network tab in browser DevTools
2. Verify API endpoints are returning valid JSON
3. Check React Query DevTools (install separately)
4. Verify TypeScript types match backend DTOs

### Build Errors

**Error**: `Module not found`

**Solution**: Install dependencies: `npm install`

**Error**: `Type errors`

**Solution**: Ensure backend DTOs haven't changed. Update types in `lib/types/api.ts`

## Development Tips

### Adding New Pages

1. Create page in `app/` directory
2. Create API function in `lib/api/`
3. Create React Query hook in `lib/hooks/`
4. Add navigation link in `components/layout/app-sidebar.tsx`

### Adding New Components

1. Create component in appropriate `components/` subdirectory
2. Export from index file (if needed)
3. Use TypeScript for props interface
4. Use Tailwind CSS for styling

### Debugging API Calls

```typescript
// Enable React Query DevTools
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';

<QueryClientProvider client={queryClient}>
  {children}
  <ReactQueryDevtools initialIsOpen={false} />
</QueryClientProvider>
```

## License

MIT

## Support

For questions or issues, refer to:
- [Backend README](../backend/README.md)
- [INTEGRATIONS_GUIDE](../INTEGRATIONS_GUIDE.md)
- [DASHBOARD_API_IMPLEMENTATION](../DASHBOARD_API_IMPLEMENTATION.md)
