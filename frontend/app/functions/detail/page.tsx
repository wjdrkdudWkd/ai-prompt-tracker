import { Suspense } from "react";
import FunctionDetailClient from "./FunctionDetailClient";

/**
 * Server component wrapper for function detail page
 *
 * This component wraps the client component in a Suspense boundary
 * to allow static export with useSearchParams() usage.
 *
 * Pattern:
 * - Server Component (page.tsx) → Provides Suspense boundary
 * - Client Component (FunctionDetailClient.tsx) → Uses useSearchParams()
 *
 * This is required for Next.js static export (output: 'export')
 * to avoid "useSearchParams should be wrapped in a suspense boundary" error.
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
