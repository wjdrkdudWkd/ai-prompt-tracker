"use client";

import { useCalls } from "@/lib/hooks/use-calls";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { formatCurrency, formatNumber, formatLatency, formatPercentage } from "@/lib/utils/format";
import { useMemo } from "react";

export default function ProvidersPage() {
  const { data: callsData, isLoading } = useCalls({ size: 1000 });

  // Aggregate provider stats from calls data
  const providerStats = useMemo(() => {
    if (!callsData?.content) return [];

    const stats = new Map<
      string,
      {
        provider: string;
        totalCalls: number;
        totalCost: number;
        successCount: number;
        totalLatency: number;
        models: Set<string>;
      }
    >();

    callsData.content.forEach((call) => {
      const existing = stats.get(call.provider) || {
        provider: call.provider,
        totalCalls: 0,
        totalCost: 0,
        successCount: 0,
        totalLatency: 0,
        models: new Set<string>(),
      };

      existing.totalCalls += 1;
      existing.totalCost += call.cost || 0;
      existing.totalLatency += call.latencyMs || 0;
      if (call.status === "success") existing.successCount += 1;
      if (call.model) existing.models.add(call.model);

      stats.set(call.provider, existing);
    });

    return Array.from(stats.values())
      .map((stat) => ({
        provider: stat.provider,
        totalCalls: stat.totalCalls,
        totalCost: stat.totalCost,
        avgLatency: stat.totalLatency / stat.totalCalls,
        successRate: stat.successCount / stat.totalCalls,
        modelCount: stat.models.size,
      }))
      .sort((a, b) => b.totalCost - a.totalCost);
  }, [callsData]);

  return (
    <div className="flex flex-col gap-6 p-8">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Providers</h1>
        <p className="text-muted-foreground">Compare AI provider performance and costs across your system</p>
      </div>

      {/* Provider Overview Cards */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {isLoading ? (
          [...Array(6)].map((_, i) => (
            <Card key={i}>
              <CardContent className="pt-6">
                <div className="h-24 animate-pulse rounded bg-muted" />
              </CardContent>
            </Card>
          ))
        ) : (
          providerStats.map((stat) => (
            <Card key={stat.provider}>
              <CardHeader className="pb-3">
                <Badge variant="outline" className="w-fit">
                  {stat.provider}
                </Badge>
                <CardTitle className="mt-2 text-lg">{stat.provider === "gpt-4-turbo" ? "OpenAI" : stat.provider}</CardTitle>
              </CardHeader>
              <CardContent className="space-y-2">
                <div className="flex justify-between text-sm">
                  <span className="text-muted-foreground">Total Cost</span>
                  <span className="font-medium">{formatCurrency(stat.totalCost)}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-muted-foreground">Total Calls</span>
                  <span className="font-medium">{formatNumber(stat.totalCalls)}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-muted-foreground">Avg Latency</span>
                  <span className="font-medium">{formatLatency(stat.avgLatency)}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-muted-foreground">Success Rate</span>
                  <span className="font-medium text-green-500">{formatPercentage(stat.successRate)}</span>
                </div>
              </CardContent>
            </Card>
          ))
        )}
      </div>

      {/* Provider & Model Breakdown Table */}
      <Card>
        <CardHeader>
          <CardTitle>Provider & Model Breakdown</CardTitle>
          <CardDescription>Detailed breakdown by provider</CardDescription>
        </CardHeader>
        <CardContent>
          {isLoading ? (
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
                    <th className="pb-3 font-medium">Provider</th>
                    <th className="pb-3 font-medium text-right">Calls</th>
                    <th className="pb-3 font-medium text-right">Cost</th>
                    <th className="pb-3 font-medium text-right">Avg Latency</th>
                    <th className="pb-3 font-medium text-right">Success Rate</th>
                    <th className="pb-3 font-medium text-right">Models</th>
                  </tr>
                </thead>
                <tbody>
                  {providerStats.map((stat) => (
                    <tr key={stat.provider} className="border-b border-border">
                      <td className="py-3">
                        <Badge variant="outline">{stat.provider}</Badge>
                      </td>
                      <td className="py-3 text-right">{formatNumber(stat.totalCalls)}</td>
                      <td className="py-3 text-right">{formatCurrency(stat.totalCost)}</td>
                      <td className="py-3 text-right">{formatLatency(stat.avgLatency)}</td>
                      <td className="py-3 text-right">
                        <span className="text-green-500">{formatPercentage(stat.successRate)}</span>
                      </td>
                      <td className="py-3 text-right">{stat.modelCount}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {!providerStats.length && (
                <div className="py-12 text-center text-muted-foreground">No provider data available</div>
              )}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
