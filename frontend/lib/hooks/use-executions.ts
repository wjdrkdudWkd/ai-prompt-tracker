import { useQuery } from "@tanstack/react-query";
import { getExecutionDetail } from "../api/executions";

export function useExecutionDetail(executionId: string | null) {
  return useQuery({
    queryKey: ["executions", executionId],
    queryFn: () => getExecutionDetail(executionId!),
    enabled: !!executionId,
  });
}
