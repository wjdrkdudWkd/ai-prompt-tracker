import { get } from "./client";
import { ExecutionDetail } from "../types/api";

/**
 * Get execution detail with call timeline
 * GET /api/executions/{executionId}
 */
export async function getExecutionDetail(executionId: string): Promise<ExecutionDetail> {
  return get<ExecutionDetail>(`/api/executions/${encodeURIComponent(executionId)}`);
}
