"use client";

import { useCalls } from "@/lib/hooks/use-calls";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  formatCurrency,
  formatTokens,
  formatLatency,
  formatDateTime,
  getStatusBadgeVariant,
} from "@/lib/utils/format";

export default function CallsPage() {
  const { data: callsData, isLoading } = useCalls({ size: 100 });

  return (
    <div className="flex flex-col gap-6 p-8">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Calls</h1>
        <p className="text-muted-foreground">Explore all AI API calls across your system</p>
      </div>

      {/* Calls Table */}
      <Card>
        <CardHeader>
          <CardTitle>All Calls</CardTitle>
          <CardDescription>
            Showing {callsData?.content.length || 0} of {callsData?.totalElements || 0} calls
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
                    <th className="pb-3 font-medium">Created At</th>
                    <th className="pb-3 font-medium">Function</th>
                    <th className="pb-3 font-medium">Provider</th>
                    <th className="pb-3 font-medium">Model</th>
                    <th className="pb-3 font-medium">Status</th>
                    <th className="pb-3 font-medium text-right">Tokens</th>
                    <th className="pb-3 font-medium text-right">Latency</th>
                    <th className="pb-3 font-medium text-right">Cost</th>
                  </tr>
                </thead>
                <tbody>
                  {callsData?.content.map((call) => (
                    <tr
                      key={call.callId}
                      className="border-b border-border transition-colors hover:bg-accent"
                    >
                      <td className="py-3 text-sm">{formatDateTime(call.createdAt)}</td>
                      <td className="py-3 font-medium">{call.functionName}</td>
                      <td className="py-3">
                        <Badge variant="outline">{call.provider}</Badge>
                      </td>
                      <td className="py-3 text-sm text-muted-foreground">{call.model}</td>
                      <td className="py-3">
                        <Badge variant={getStatusBadgeVariant(call.status)}>
                          {call.status.toUpperCase()}
                        </Badge>
                      </td>
                      <td className="py-3 text-right">
                        <div className="text-sm">{formatTokens(call.totalTokens)}</div>
                        <div className="text-xs text-muted-foreground">
                          {call.promptTokens} / {call.completionTokens}
                        </div>
                      </td>
                      <td className="py-3 text-right">{formatLatency(call.latencyMs)}</td>
                      <td className="py-3 text-right">{formatCurrency(call.cost)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {!callsData?.content.length && (
                <div className="py-12 text-center text-muted-foreground">No calls found</div>
              )}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
