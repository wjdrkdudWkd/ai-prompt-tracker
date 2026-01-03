"use client";

import { useExecutionDetail } from "@/lib/hooks/use-executions";
import {
  Drawer,
  DrawerClose,
  DrawerContent,
  DrawerHeader,
  DrawerTitle,
} from "@/components/ui/drawer";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { X, ChevronDown, ChevronUp } from "lucide-react";
import {
  formatCurrency,
  formatDuration,
  formatDateTime,
  formatTokens,
  formatLatency,
  getStatusBadgeVariant,
} from "@/lib/utils/format";
import { useState } from "react";

interface ExecutionDrawerProps {
  executionId: string | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function ExecutionDrawer({ executionId, open, onOpenChange }: ExecutionDrawerProps) {
  const { data: execution, isLoading } = useExecutionDetail(executionId);
  const [expandedCalls, setExpandedCalls] = useState<Set<string>>(new Set());

  const toggleCallExpand = (callId: string) => {
    const newExpanded = new Set(expandedCalls);
    if (newExpanded.has(callId)) {
      newExpanded.delete(callId);
    } else {
      newExpanded.add(callId);
    }
    setExpandedCalls(newExpanded);
  };

  return (
    <Drawer open={open} onOpenChange={onOpenChange} direction="right">
      <DrawerContent>
        <DrawerHeader>
          <div className="flex items-start justify-between">
            <div className="flex-1">
              <DrawerTitle>Execution Detail</DrawerTitle>
              {execution && (
                <div className="mt-2 flex items-center gap-2">
                  <Badge variant={getStatusBadgeVariant(execution.status)}>
                    {execution.status.toUpperCase()}
                  </Badge>
                  <Badge variant="outline">{execution.environment}</Badge>
                </div>
              )}
            </div>
            <DrawerClose className="rounded-sm opacity-70 ring-offset-background transition-opacity hover:opacity-100">
              <X className="h-4 w-4" />
            </DrawerClose>
          </div>
        </DrawerHeader>

        <div className="flex-1 overflow-y-auto p-6">
          {isLoading ? (
            <div className="space-y-4">
              {[...Array(3)].map((_, i) => (
                <div key={i} className="h-24 animate-pulse rounded bg-muted" />
              ))}
            </div>
          ) : execution ? (
            <div className="space-y-6">
              {/* Execution Summary */}
              <div className="grid grid-cols-2 gap-4">
                <Card>
                  <CardContent className="pt-6">
                    <div className="text-sm text-muted-foreground">Start Time</div>
                    <div className="mt-1 font-medium">{formatDateTime(execution.startedAt)}</div>
                  </CardContent>
                </Card>
                <Card>
                  <CardContent className="pt-6">
                    <div className="text-sm text-muted-foreground">Duration</div>
                    <div className="mt-1 font-medium">{formatDuration(execution.durationMs)}</div>
                  </CardContent>
                </Card>
                <Card>
                  <CardContent className="pt-6">
                    <div className="text-sm text-muted-foreground">Total Calls</div>
                    <div className="mt-1 text-2xl font-bold">{execution.callsCount}</div>
                  </CardContent>
                </Card>
                <Card>
                  <CardContent className="pt-6">
                    <div className="text-sm text-muted-foreground">Total Tokens</div>
                    <div className="mt-1 text-2xl font-bold">{formatTokens(execution.totalTokens)}</div>
                  </CardContent>
                </Card>
                <Card className="col-span-2">
                  <CardContent className="pt-6">
                    <div className="text-sm text-muted-foreground">Total Cost</div>
                    <div className="mt-1 text-2xl font-bold">{formatCurrency(execution.totalCost)}</div>
                  </CardContent>
                </Card>
              </div>

              {/* Call Timeline */}
              <div>
                <h3 className="mb-4 text-lg font-semibold">Call Timeline</h3>
                <div className="space-y-3">
                  {execution.calls.map((call, index) => {
                    const isExpanded = expandedCalls.has(call.callId);
                    return (
                      <Card key={call.callId} className="overflow-hidden">
                        <div
                          className="cursor-pointer p-4 transition-colors hover:bg-accent"
                          onClick={() => toggleCallExpand(call.callId)}
                        >
                          <div className="flex items-center justify-between">
                            <div className="flex items-center gap-3">
                              <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary text-sm font-medium text-primary-foreground">
                                {index + 1}
                              </div>
                              <div>
                                <div className="flex items-center gap-2">
                                  <Badge variant="outline">{call.provider}</Badge>
                                  <span className="text-sm text-muted-foreground">{call.model}</span>
                                </div>
                                <div className="mt-1 flex items-center gap-4 text-xs text-muted-foreground">
                                  <span>{formatTokens(call.totalTokens)} tokens</span>
                                  <span>{formatLatency(call.latencyMs)}</span>
                                  <span>{formatCurrency(call.cost)}</span>
                                  {call.wasTruncated && (
                                    <Badge variant="secondary" className="text-xs">
                                      Truncated
                                    </Badge>
                                  )}
                                </div>
                              </div>
                            </div>
                            <div className="flex items-center gap-2">
                              <Badge variant={getStatusBadgeVariant(call.status)}>
                                {call.status.toUpperCase()}
                              </Badge>
                              {isExpanded ? (
                                <ChevronUp className="h-4 w-4" />
                              ) : (
                                <ChevronDown className="h-4 w-4" />
                              )}
                            </div>
                          </div>
                        </div>

                        {isExpanded && (
                          <div className="border-t border-border bg-muted/50 p-4">
                            {call.errorMessage && (
                              <div className="mb-4">
                                <div className="text-sm font-medium text-destructive">Error</div>
                                <div className="mt-1 text-sm text-muted-foreground">
                                  {call.errorType}: {call.errorMessage}
                                </div>
                              </div>
                            )}

                            {call.requestPreview && (
                              <div className="mb-4">
                                <div className="mb-1 text-sm font-medium">Request Preview</div>
                                <pre className="overflow-x-auto rounded bg-background p-3 text-xs">
                                  <code>{call.requestPreview}</code>
                                </pre>
                              </div>
                            )}

                            {call.responsePreview && (
                              <div>
                                <div className="mb-1 text-sm font-medium">Response Preview</div>
                                <pre className="overflow-x-auto rounded bg-background p-3 text-xs">
                                  <code>{call.responsePreview}</code>
                                </pre>
                              </div>
                            )}

                            <div className="mt-3 grid grid-cols-3 gap-2 text-xs">
                              <div>
                                <div className="text-muted-foreground">Prompt Tokens</div>
                                <div className="font-medium">{call.promptTokens}</div>
                              </div>
                              <div>
                                <div className="text-muted-foreground">Completion Tokens</div>
                                <div className="font-medium">{call.completionTokens}</div>
                              </div>
                              <div>
                                <div className="text-muted-foreground">Total Tokens</div>
                                <div className="font-medium">{call.totalTokens}</div>
                              </div>
                            </div>
                          </div>
                        )}
                      </Card>
                    );
                  })}
                </div>
              </div>
            </div>
          ) : (
            <div className="py-12 text-center text-muted-foreground">No execution data found</div>
          )}
        </div>
      </DrawerContent>
    </Drawer>
  );
}
