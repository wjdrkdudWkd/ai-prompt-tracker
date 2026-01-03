"use client";

import { useDashboardSummary } from "@/lib/hooks/use-dashboard";
import { useFunctions } from "@/lib/hooks/use-functions";
import { KPICard } from "@/components/dashboard/kpi-card";
import {
  DollarSign,
  Activity,
  Phone,
  Clock,
  Layers,
  AlertCircle,
} from "lucide-react";
import {
  formatCurrency,
  formatNumber,
  formatLatency,
  formatPercentage,
} from "@/lib/utils/format";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { useRouter } from "next/navigation";

export default function DashboardPage() {
  const router = useRouter();
  const { data: summary, isLoading: summaryLoading } = useDashboardSummary();
  const { data: functionsData, isLoading: functionsLoading } = useFunctions({ size: 25 });

  return (
    <div className="flex flex-col gap-6 p-8">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Dashboard</h1>
        <p className="text-muted-foreground">
          Monitor and analyze AI API usage across your system
        </p>
      </div>

      {/* KPI Cards */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        <KPICard
          title="Total Cost"
          value={formatCurrency(summary?.totalCost)}
          icon={DollarSign}
          loading={summaryLoading}
        />
        <KPICard
          title="Executions"
          value={formatNumber(summary?.totalExecutions)}
          icon={Activity}
          loading={summaryLoading}
        />
        <KPICard
          title="AI Calls"
          value={formatNumber(summary?.totalCalls)}
          icon={Phone}
          loading={summaryLoading}
        />
        <KPICard
          title="Avg Calls/Exec"
          value={summary?.avgCallsPerExecution?.toFixed(2) || "-"}
          icon={Layers}
          subtitle="Flat"
          loading={summaryLoading}
        />
        <KPICard
          title="Avg Latency"
          value={formatLatency(summary?.avgLatencyMs)}
          icon={Clock}
          loading={summaryLoading}
        />
        <KPICard
          title="Error Rate"
          value={formatPercentage(summary?.callErrorRate)}
          icon={AlertCircle}
          loading={summaryLoading}
        />
      </div>

      {/* Functions Table */}
      <Card>
        <CardHeader>
          <CardTitle>Functions</CardTitle>
          <CardDescription>All AI-integrated functions across your project</CardDescription>
        </CardHeader>
        <CardContent>
          {functionsLoading ? (
            <div className="space-y-2">
              {[...Array(5)].map((_, i) => (
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
                      onClick={() => router.push(`/functions/${encodeURIComponent(func.functionName)}`)}
                    >
                      <td className="py-3 font-medium">{func.functionName}</td>
                      <td className="py-3">
                        <span className="rounded-full bg-secondary px-2 py-1 text-xs">
                          {func.category}
                        </span>
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
                <div className="py-12 text-center text-muted-foreground">
                  No functions found
                </div>
              )}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
