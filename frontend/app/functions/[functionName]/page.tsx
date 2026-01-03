"use client";

import { useFunctionDetail, useFunctionExecutions } from "@/lib/hooks/use-functions";
import { KPICard } from "@/components/dashboard/kpi-card";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { ExecutionDrawer } from "@/components/executions/execution-drawer";
import {
  DollarSign,
  Activity,
  Phone,
  Clock,
  AlertCircle,
  Layers,
} from "lucide-react";
import {
  formatCurrency,
  formatNumber,
  formatLatency,
  formatPercentage,
  formatDateTime,
  formatDuration,
  getStatusBadgeVariant,
} from "@/lib/utils/format";
import { useState } from "react";

interface PageProps {
  params: {
    functionName: string;
  };
}

export default function FunctionDetailPage({ params }: PageProps) {
  const functionName = decodeURIComponent(params.functionName);
  const { data: functionDetail, isLoading: detailLoading } = useFunctionDetail(functionName);
  const { data: executionsData, isLoading: executionsLoading } = useFunctionExecutions(functionName, {
    size: 50,
  });

  const [selectedExecutionId, setSelectedExecutionId] = useState<string | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);

  const openExecutionDrawer = (executionId: string) => {
    setSelectedExecutionId(executionId);
    setDrawerOpen(true);
  };

  return (
    <div className="flex flex-col gap-6 p-8">
      {/* Header */}
      <div>
        <div className="flex items-center gap-2">
          <h1 className="text-3xl font-bold tracking-tight">{functionName}</h1>
          {functionDetail && (
            <Badge variant="outline" className="text-sm">
              {functionDetail.category}
            </Badge>
          )}
        </div>
        <p className="mt-1 text-muted-foreground">Function execution metrics and history</p>
      </div>

      {/* KPI Cards */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        <KPICard
          title="Executions"
          value={formatNumber(functionDetail?.totalExecutions)}
          icon={Activity}
          loading={detailLoading}
        />
        <KPICard
          title="AI Calls"
          value={formatNumber(functionDetail?.totalCalls)}
          icon={Phone}
          loading={detailLoading}
        />
        <KPICard
          title="Total Cost"
          value={formatCurrency(functionDetail?.totalCost)}
          icon={DollarSign}
          loading={detailLoading}
        />
        <KPICard
          title="Calls/Execution"
          value={
            functionDetail?.totalCalls && functionDetail?.totalExecutions
              ? (functionDetail.totalCalls / functionDetail.totalExecutions).toFixed(2)
              : "-"
          }
          icon={Layers}
          loading={detailLoading}
        />
        <KPICard
          title="Avg Exec Time"
          value={formatLatency(functionDetail?.avgExecutionTimeMs)}
          icon={Clock}
          loading={detailLoading}
        />
        <KPICard
          title="Error Rate"
          value={formatPercentage(functionDetail?.errorRate)}
          icon={AlertCircle}
          loading={detailLoading}
        />
      </div>

      {/* Provider & Model Breakdown */}
      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Top Providers</CardTitle>
            <CardDescription>Provider breakdown by usage</CardDescription>
          </CardHeader>
          <CardContent>
            {detailLoading ? (
              <div className="space-y-2">
                {[...Array(3)].map((_, i) => (
                  <div key={i} className="h-12 animate-pulse rounded bg-muted" />
                ))}
              </div>
            ) : (
              <div className="space-y-3">
                {functionDetail?.topProviders.map((provider) => (
                  <div key={provider.provider} className="flex items-center justify-between">
                    <div>
                      <Badge variant="outline">{provider.provider}</Badge>
                      <div className="mt-1 text-sm text-muted-foreground">
                        {formatNumber(provider.calls)} calls
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="font-medium">{formatCurrency(provider.cost)}</div>
                    </div>
                  </div>
                ))}
                {!functionDetail?.topProviders.length && (
                  <div className="py-6 text-center text-sm text-muted-foreground">No provider data</div>
                )}
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Top Models</CardTitle>
            <CardDescription>Model breakdown by usage</CardDescription>
          </CardHeader>
          <CardContent>
            {detailLoading ? (
              <div className="space-y-2">
                {[...Array(3)].map((_, i) => (
                  <div key={i} className="h-12 animate-pulse rounded bg-muted" />
                ))}
              </div>
            ) : (
              <div className="space-y-3">
                {functionDetail?.topModels.map((model) => (
                  <div key={model.model} className="flex items-center justify-between">
                    <div>
                      <div className="font-medium">{model.model}</div>
                      <div className="text-sm text-muted-foreground">{formatNumber(model.calls)} calls</div>
                    </div>
                    <div className="text-right">
                      <div className="font-medium">{formatCurrency(model.cost)}</div>
                    </div>
                  </div>
                ))}
                {!functionDetail?.topModels.length && (
                  <div className="py-6 text-center text-sm text-muted-foreground">No model data</div>
                )}
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Execution History */}
      <Card>
        <CardHeader>
          <CardTitle>Execution History</CardTitle>
          <CardDescription>Recent executions for this function</CardDescription>
        </CardHeader>
        <CardContent>
          {executionsLoading ? (
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
                    <th className="pb-3 font-medium">Started At</th>
                    <th className="pb-3 font-medium">Duration</th>
                    <th className="pb-3 font-medium">Status</th>
                    <th className="pb-3 font-medium">Environment</th>
                    <th className="pb-3 font-medium text-right">Calls</th>
                    <th className="pb-3 font-medium text-right">Cost</th>
                  </tr>
                </thead>
                <tbody>
                  {executionsData?.content.map((execution) => (
                    <tr
                      key={execution.executionId}
                      className="cursor-pointer border-b border-border transition-colors hover:bg-accent"
                      onClick={() => openExecutionDrawer(execution.executionId)}
                    >
                      <td className="py-3">{formatDateTime(execution.startedAt)}</td>
                      <td className="py-3">{formatDuration(execution.durationMs)}</td>
                      <td className="py-3">
                        <Badge variant={getStatusBadgeVariant(execution.status)}>
                          {execution.status.toUpperCase()}
                        </Badge>
                      </td>
                      <td className="py-3">
                        <Badge variant="outline">{execution.environment}</Badge>
                      </td>
                      <td className="py-3 text-right">{execution.callsCount}</td>
                      <td className="py-3 text-right">{formatCurrency(execution.totalCost)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {!executionsData?.content.length && (
                <div className="py-12 text-center text-muted-foreground">No executions found</div>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Execution Drawer */}
      <ExecutionDrawer
        executionId={selectedExecutionId}
        open={drawerOpen}
        onOpenChange={setDrawerOpen}
      />
    </div>
  );
}
