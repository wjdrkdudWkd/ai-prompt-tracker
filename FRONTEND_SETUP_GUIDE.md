# AI Prompt Tracker - Frontend Setup Guide

## Quick Start (3 Steps)

### 1. Install Dependencies

```bash
cd frontend
npm install
```

### 2. Configure Backend URL

Create `frontend/.env.local`:

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

### 3. Run Development Server

```bash
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) in your browser.

---

## Detailed Setup

### Prerequisites

- **Node.js**: 18.x or higher
- **npm**: 9.x or higher
- **Backend**: AI Prompt Tracker backend running on port 8080

### Verify Prerequisites

```bash
node --version  # Should be v18.x or higher
npm --version   # Should be 9.x or higher
```

### Installation Steps

#### 1. Navigate to Frontend Directory

```bash
cd /path/to/ai-prompt-tracker/frontend
```

#### 2. Install Dependencies

```bash
npm install
```

This will install:
- Next.js 14
- React 18
- TanStack Query (React Query)
- Radix UI components
- Tailwind CSS
- TypeScript
- And all other dependencies from `package.json`

Expected output:
```
added 350 packages in 45s
```

#### 3. Configure Environment Variables

Create `.env.local` file in the `frontend/` directory:

```env
# Backend API URL
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

**Important**: Never commit `.env.local` to version control. It's already in `.gitignore`.

#### 4. Verify Backend is Running

Before starting the frontend, ensure the backend is running:

```bash
# In a separate terminal
cd ../backend
./gradlew bootRun
```

Verify backend health:
```bash
curl http://localhost:8080/api/dashboard/summary
```

Expected response: JSON with dashboard metrics

#### 5. Start Development Server

```bash
npm run dev
```

Expected output:
```
▲ Next.js 14.2.18
- Local:        http://localhost:3000
- Ready in 2.3s
```

#### 6. Open in Browser

Navigate to [http://localhost:3000](http://localhost:3000)

You should see the Dashboard page with KPI cards and functions table.

---

## Production Build

### Build for Production

```bash
npm run build
```

Expected output:
```
Route (app)                              Size     First Load JS
┌ ○ /                                    137 B          87.2 kB
├ ○ /dashboard                           5.24 kB        95.6 kB
├ ○ /functions                           4.18 kB        94.5 kB
└ ○ /functions/[functionName]            6.32 kB        96.6 kB
```

### Run Production Server

```bash
npm start
```

Server will run on [http://localhost:3000](http://localhost:3000)

---

## Troubleshooting

### Issue 1: Dependencies Won't Install

**Symptoms**:
```
npm ERR! code ERESOLVE
npm ERR! ERESOLVE unable to resolve dependency tree
```

**Solution**:
```bash
# Clear npm cache
npm cache clean --force

# Delete node_modules and package-lock.json
rm -rf node_modules package-lock.json

# Reinstall
npm install
```

### Issue 2: Backend Connection Error

**Symptoms**:
- Dashboard shows "No data found"
- Console shows: `Network error: Failed to fetch`

**Solution**:

1. Verify backend is running:
```bash
curl http://localhost:8080/api/dashboard/summary
```

2. Check `.env.local` has correct backend URL:
```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

3. Restart frontend dev server:
```bash
# Ctrl+C to stop, then:
npm run dev
```

### Issue 3: CORS Errors

**Symptoms**:
```
Access to fetch at 'http://localhost:8080/api/dashboard/summary' from origin 'http://localhost:3000' has been blocked by CORS policy
```

**Solution**:

Add CORS configuration to backend:

```java
// backend/src/main/java/com/galoong/aiprompttracker/config/CorsConfig.java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
```

Restart backend after adding CORS config.

### Issue 4: Port 3000 Already in Use

**Symptoms**:
```
Error: listen EADDRINUSE: address already in use :::3000
```

**Solution**:

Option 1: Kill process using port 3000
```bash
# Find process
lsof -i :3000

# Kill process
kill -9 <PID>
```

Option 2: Use different port
```bash
PORT=3001 npm run dev
```

### Issue 5: TypeScript Errors

**Symptoms**:
```
Type error: Property 'totalCost' does not exist on type 'DashboardSummary'
```

**Solution**:

1. Verify backend DTOs haven't changed
2. Update types in `lib/types/api.ts` to match backend DTOs
3. Restart TypeScript server in IDE

### Issue 6: Page Not Found (404)

**Symptoms**:
- Navigating to `/functions/my-function` shows 404

**Solution**:

1. Ensure function name is URL-encoded:
```typescript
router.push(`/functions/${encodeURIComponent(functionName)}`);
```

2. Verify dynamic route exists:
```
app/functions/[functionName]/page.tsx
```

3. Restart dev server

---

## Development Workflow

### 1. Adding New Pages

```bash
# Create new page
touch app/my-page/page.tsx

# Add to sidebar navigation
# Edit: components/layout/app-sidebar.tsx
```

### 2. Adding New API Endpoints

```typescript
// 1. Add API function
// lib/api/my-feature.ts
export async function getMyData(filters: Filters) {
  const queryString = buildQueryString(filters);
  return get<MyData>(`/api/my-endpoint${queryString}`);
}

// 2. Add React Query hook
// lib/hooks/use-my-feature.ts
export function useMyData(filters: Filters = {}) {
  return useQuery({
    queryKey: ["my-data", filters],
    queryFn: () => getMyData(filters),
  });
}

// 3. Use in component
const { data, isLoading } = useMyData({ filter: 'value' });
```

### 3. Adding New Components

```bash
# Create component
touch components/my-feature/my-component.tsx

# Import and use
import { MyComponent } from "@/components/my-feature/my-component";
```

### 4. Formatting Code

```bash
# Install Prettier (optional)
npm install --save-dev prettier

# Format all files
npx prettier --write .
```

---

## Environment Variables Reference

### Required

| Variable | Description | Example |
|----------|-------------|---------|
| `NEXT_PUBLIC_API_BASE_URL` | Backend API base URL | `http://localhost:8080` |

### Optional

| Variable | Description | Default |
|----------|-------------|---------|
| `PORT` | Frontend port | `3000` |
| `NODE_ENV` | Environment | `development` |

---

## Testing the Setup

### 1. Verify Dashboard Loads

Navigate to [http://localhost:3000/dashboard](http://localhost:3000/dashboard)

Expected:
- 6 KPI cards with metrics
- Functions table with data

### 2. Verify Function Detail Page

Click any function row in dashboard table

Expected:
- Navigate to `/functions/{functionName}`
- Show function-level KPI cards
- Show provider/model breakdown
- Show execution history table

### 3. Verify Execution Drawer

Click any execution row in function detail page

Expected:
- Right-side drawer opens
- Shows execution summary
- Shows call timeline
- Click call to expand request/response previews

### 4. Verify Calls Explorer

Navigate to [http://localhost:3000/calls](http://localhost:3000/calls)

Expected:
- Table of all AI API calls
- Provider/model badges
- Token counts
- Cost and latency metrics

### 5. Verify Providers Page

Navigate to [http://localhost:3000/providers](http://localhost:3000/providers)

Expected:
- Provider comparison cards
- Provider & model breakdown table
- Aggregated statistics

---

## Next Steps

### 1. Customize Theme

Edit `app/globals.css` to customize colors:

```css
:root {
  --primary: 210 40% 98%;
  --secondary: 217.2 32.6% 17.5%;
  /* ... more variables */
}
```

### 2. Add Custom Filters

Future enhancement: Add Swagger-style filter panels with URL query param syncing.

### 3. Add Charts

Future enhancement: Integrate charting library (e.g., Recharts, Chart.js) for visualizations.

### 4. Add Authentication

Future enhancement: Add authentication middleware (e.g., NextAuth.js).

---

## Additional Resources

- [Next.js Documentation](https://nextjs.org/docs)
- [TanStack Query Documentation](https://tanstack.com/query/latest)
- [Tailwind CSS Documentation](https://tailwindcss.com/docs)
- [shadcn/ui Documentation](https://ui.shadcn.com)
- [Radix UI Documentation](https://www.radix-ui.com/docs/primitives)

---

## Support

For questions or issues:
1. Check [Frontend README](frontend/README.md)
2. Check [Backend README](backend/README.md)
3. Check [INTEGRATIONS_GUIDE](INTEGRATIONS_GUIDE.md)
4. Check [DASHBOARD_API_IMPLEMENTATION](DASHBOARD_API_IMPLEMENTATION.md)

---

## Summary

You now have:
✅ Next.js 14 application with TypeScript
✅ React Query for data fetching
✅ shadcn/ui components
✅ Tailwind CSS styling
✅ Dashboard with KPI cards
✅ Functions list and detail pages
✅ Execution drawer component
✅ Calls explorer page
✅ Providers comparison page
✅ Full type safety from backend DTOs
✅ Loading and error states
✅ Development and production builds

**Next**: Start the backend, then run `npm run dev` and open [http://localhost:3000](http://localhost:3000)!
