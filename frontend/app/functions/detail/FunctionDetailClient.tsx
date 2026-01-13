"use client";

import { useSearchParams, useRouter } from "next/navigation";
import { useFunctionDetail } from "@/lib/hooks/use-functions";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { ArrowLeft } from "lucide-react";
import {
  formatCurrency,
  formatNumber,
  formatLatency,
  formatPercentage,
} from "@/lib/utils/format";

/**
 * Client component for function detail page
 *
 * Uses useSearchParams() to read the 'name' query parameter.
 * Must be wrapped in Suspense boundary by parent Server Component.
 */
export default function FunctionDetailClient() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const functionName = searchParams?.get("name") || null;

  const { data: functionDetail, isLoading } = useFunctionDetail(functionName || "");

  if (!functionName) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <div className="text-center">
          <h2 className="text-2xl font-bold">No Function Selected</h2>
          <p className="mt-2 text-muted-foreground">Please select a function from the list</p>
          <Button
            variant="outline"
            className="mt-4"
            onClick={() => router.push("/aiprompt-tracker/functions")}
          >
            <ArrowLeft className="mr-2 h-4 w-4" />
            Back to Functions
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-6 p-8">
      {/* Header */}
      <div className="flex items-center gap-4">
        <Button
          variant="ghost"
          size="icon"
          onClick={() => router.push("/aiprompt-tracker/functions")}
        >
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <div>
          <h1 className="text-3xl font-bold tracking-tight">{functionName}</h1>
          <p className="text-muted-foreground">Detailed analytics and execution history</p>
        </div>
      </div>

      {isLoading ? (
        <div className="space-y-4">
          {[...Array(3)].map((_, i) => (
            <div key={i} className="h-32 animate-pulse rounded bg-muted" />
          ))}
        </div>
      ) : !functionDetail ? (
        <Card>
          <CardContent className="py-12 text-center">
            <p className="text-muted-foreground">Function not found</p>
          </CardContent>
        </Card>
      ) : (
        <>
          {/* Stats Overview */}
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
            <Card>
              <CardHeader className="pb-3">
                <CardTitle className="text-sm font-medium text-muted-foreground">Total Executions</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{formatNumber(functionDetail.totalExecutions)}</div>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="pb-3">
                <CardTitle className="text-sm font-medium text-muted-foreground">Total Calls</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{formatNumber(functionDetail.totalCalls)}</div>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="pb-3">
                <CardTitle className="text-sm font-medium text-muted-foreground">Total Cost</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{formatCurrency(functionDetail.totalCost)}</div>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="pb-3">
                <CardTitle className="text-sm font-medium text-muted-foreground">Error Rate</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{formatPercentage(functionDetail.errorRate)}</div>
              </CardContent>
            </Card>
          </div>

          {/* Additional Details */}
          <Card>
            <CardHeader>
              <CardTitle>Performance Metrics</CardTitle>
              <CardDescription>Average execution time and call efficiency</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex justify-between">
                <span className="text-muted-foreground">Avg Execution Time</span>
                <span className="font-medium">{formatLatency(functionDetail.avgExecutionTimeMs)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">Calls per Execution</span>
                <span className="font-medium">
                  {functionDetail.totalExecutions > 0
                    ? (functionDetail.totalCalls / functionDetail.totalExecutions).toFixed(2)
                    : "0.00"}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">Category</span>
                <span className="rounded-full bg-secondary px-2 py-1 text-xs">{functionDetail.category}</span>
              </div>
            </CardContent>
          </Card>
        </>
      )}
    </div>
  );
}
