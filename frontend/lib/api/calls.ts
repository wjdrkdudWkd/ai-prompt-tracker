import { get } from "./client";
import { Call, CallsFilters, PaginatedResponse } from "../types/api";
import { buildQueryString } from "../utils/filters";

/**
 * Get calls list with pagination
 * GET /api/calls?provider=&model=&status=&from=&to=&page=&size=
 */
export async function getCalls(filters: CallsFilters = {}): Promise<PaginatedResponse<Call>> {
  const queryString = buildQueryString(filters);
  return get<PaginatedResponse<Call>>(`/api/calls${queryString}`);
}
