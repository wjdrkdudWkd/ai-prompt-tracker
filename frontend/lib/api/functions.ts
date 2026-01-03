import { get } from "./client";
import {
  FunctionAggregate,
  FunctionDetail,
  FunctionsFilters,
  ExecutionsFilters,
  PaginatedResponse,
  ExecutionSummary,
} from "../types/api";
import { buildQueryString } from "../utils/filters";

/**
 * Get functions list with pagination
 * GET /api/functions?from=&to=&env=&category=&status=&q=&page=&size=
 */
export async function getFunctions(
  filters: FunctionsFilters = {}
): Promise<PaginatedResponse<FunctionAggregate>> {
  const queryString = buildQueryString(filters);
  return get<PaginatedResponse<FunctionAggregate>>(`/api/functions${queryString}`);
}

/**
 * Get function detail
 * GET /api/functions/{functionName}
 */
export async function getFunctionDetail(functionName: string): Promise<FunctionDetail> {
  return get<FunctionDetail>(`/api/functions/${encodeURIComponent(functionName)}`);
}

/**
 * Get function executions
 * GET /api/functions/{functionName}/executions?from=&to=&page=&size=
 */
export async function getFunctionExecutions(
  functionName: string,
  filters: ExecutionsFilters = {}
): Promise<PaginatedResponse<ExecutionSummary>> {
  const queryString = buildQueryString(filters);
  return get<PaginatedResponse<ExecutionSummary>>(
    `/api/functions/${encodeURIComponent(functionName)}/executions${queryString}`
  );
}
