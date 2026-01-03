// TypeScript types mapped from backend DTOs

export interface DashboardSummary {
  totalCost: number;
  totalExecutions: number;
  totalCalls: number;
  avgLatencyMs: number;
  avgCallsPerExecution: number;
  executionErrorRate: number;
  callErrorRate: number;
}

export interface FunctionAggregate {
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

export interface ProviderBreakdown {
  provider: string;
  calls: number;
  cost: number;
}

export interface ModelBreakdown {
  model: string;
  calls: number;
  cost: number;
}

export interface FunctionDetail {
  functionName: string;
  category: string;
  tags: string[];
  totalExecutions: number;
  totalCalls: number;
  totalCost: number;
  avgExecutionTimeMs: number;
  errorRate: number;
  topProviders: ProviderBreakdown[];
  topModels: ModelBreakdown[];
}

export interface CallDetail {
  callId: string;
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
  createdAt: string; // ISO timestamp
}

export interface ExecutionDetail {
  executionId: string;
  functionName: string;
  category: string;
  tags: string[];
  environment: string;
  startedAt: string; // ISO timestamp
  finishedAt: string;
  durationMs: number;
  status: string;
  errorMessage?: string;
  callsCount: number;
  totalTokens: number;
  totalCost: number;
  calls: CallDetail[];
}

export interface ExecutionSummary {
  executionId: string;
  functionName: string;
  environment: string;
  startedAt: string;
  durationMs: number;
  status: string;
  callsCount: number;
  totalCost: number;
}

export interface Call {
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

// Pagination response wrapper
export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number; // current page (0-indexed)
}

// Filter types
export interface DashboardFilters {
  from?: string;
  to?: string;
  env?: string;
}

export interface FunctionsFilters extends DashboardFilters {
  category?: string;
  status?: string;
  q?: string; // search query
  page?: number;
  size?: number;
}

export interface CallsFilters {
  provider?: string;
  model?: string;
  status?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

export interface ExecutionsFilters {
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}
