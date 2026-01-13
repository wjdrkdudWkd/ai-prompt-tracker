"use client";

import { useFunctions } from "@/lib/hooks/use-functions";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { useRouter } from "next/navigation";
import {
  formatCurrency,
  formatNumber,
  formatLatency,
  formatPercentage,
} from "@/lib/utils/format";

export default function FunctionsPage() {
  const router = useRouter();
  const { data: functionsData, isLoading } = useFunctions({ size: 100 });

  return (
    <div className="flex flex-col gap-6 p-8">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Functions</h1>
        <p className="text-muted-foreground">Monitor and analyze all AI-integrated functions across your project</p>
      </div>

      {/* Functions Table */}
      <Card>
        <CardHeader>
          <CardTitle>All Functions</CardTitle>
          <CardDescription>
            Showing {functionsData?.content.length || 0} of {functionsData?.totalElements || 0} functions
          </CardDescription>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="space-y-2">
              {[...Array(10)].map((_, i) => (
                <div key={i} className="h-12 animate-pulse rounded bg-muted" />
              ))}
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b border-border text-left text-sm text-muted-foreground">
                    <th className="pb-3 font-medium">Function</th>
                    <th className="pb-3 font-medium">Category</th>
                    <th className="pb-3 font-medium text-right">Executions</th>
                    <th className="pb-3 font-medium text-right">Calls</th>
                    <th className="pb-3 font-medium text-right">Calls/Exec</th>
                    <th className="pb-3 font-medium text-right">Cost</th>
                    <th className="pb-3 font-medium text-right">Avg Time</th>
                    <th className="pb-3 font-medium text-right">Error %</th>
                  </tr>
                </thead>
                <tbody>
                  {functionsData?.content.map((func) => (
                    <tr
                      key={func.functionName}
                      className="cursor-pointer border-b border-border transition-colors hover:bg-accent"
                      onClick={() => router.push(`/aiprompt-tracker/functions/detail?name=${encodeURIComponent(func.functionName)}`)}
                    >
                      <td className="py-3 font-medium">{func.functionName}</td>
                      <td className="py-3">
                        <span className="rounded-full bg-secondary px-2 py-1 text-xs">{func.category}</span>
                      </td>
                      <td className="py-3 text-right">{formatNumber(func.executions)}</td>
                      <td className="py-3 text-right">{formatNumber(func.calls)}</td>
                      <td className="py-3 text-right">{func.callsPerExecution.toFixed(1)}</td>
                      <td className="py-3 text-right">{formatCurrency(func.totalCost)}</td>
                      <td className="py-3 text-right">{formatLatency(func.avgExecutionTimeMs)}</td>
                      <td className="py-3 text-right">{formatPercentage(func.errorRate)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {!functionsData?.content.length && (
                <div className="py-12 text-center text-muted-foreground">No functions found</div>
              )}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
