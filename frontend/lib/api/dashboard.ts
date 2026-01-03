import { get } from "./client";
import { DashboardSummary, DashboardFilters } from "../types/api";
import { buildQueryString } from "../utils/filters";

/**
 * Get dashboard summary
 * GET /api/dashboard/summary?from=&to=&env=
 */
export async function getDashboardSummary(filters: DashboardFilters = {}): Promise<DashboardSummary> {
  const queryString = buildQueryString(filters);
  return get<DashboardSummary>(`/api/dashboard/summary${queryString}`);
}
