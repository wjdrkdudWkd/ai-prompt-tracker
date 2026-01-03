import { useQuery } from "@tanstack/react-query";
import { getDashboardSummary } from "../api/dashboard";
import { DashboardFilters } from "../types/api";

export function useDashboardSummary(filters: DashboardFilters = {}) {
  return useQuery({
    queryKey: ["dashboard", "summary", filters],
    queryFn: () => getDashboardSummary(filters),
  });
}
