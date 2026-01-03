import { useQuery } from "@tanstack/react-query";
import { getFunctions, getFunctionDetail, getFunctionExecutions } from "../api/functions";
import { FunctionsFilters, ExecutionsFilters } from "../types/api";

export function useFunctions(filters: FunctionsFilters = {}) {
  return useQuery({
    queryKey: ["functions", filters],
    queryFn: () => getFunctions(filters),
  });
}

export function useFunctionDetail(functionName: string | null) {
  return useQuery({
    queryKey: ["functions", functionName],
    queryFn: () => getFunctionDetail(functionName!),
    enabled: !!functionName,
  });
}

export function useFunctionExecutions(functionName: string | null, filters: ExecutionsFilters = {}) {
  return useQuery({
    queryKey: ["functions", functionName, "executions", filters],
    queryFn: () => getFunctionExecutions(functionName!, filters),
    enabled: !!functionName,
  });
}
