import { useQuery } from "@tanstack/react-query";
import { getCalls } from "../api/calls";
import { CallsFilters } from "../types/api";

export function useCalls(filters: CallsFilters = {}) {
  return useQuery({
    queryKey: ["calls", filters],
    queryFn: () => getCalls(filters),
  });
}
