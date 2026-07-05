import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import type { PageParams } from "@/lib/types/api.ts";
import {
  createReport,
  getMosqueReports,
  getReport,
} from "../api/reports-api.ts";
import type { ReportCreateRequest } from "../types/index.ts";
import { reportKeys } from "./query-keys.ts";

export function useReport(id: string | undefined) {
  return useQuery({
    queryKey: reportKeys.detail(id ?? ""),
    queryFn: () => getReport(id!),
    enabled: !!id,
  });
}

export function useMosqueReports(
  mosqueId: string | undefined,
  params?: PageParams,
) {
  return useQuery({
    queryKey: reportKeys.mosque(mosqueId ?? "", params),
    queryFn: () => getMosqueReports(mosqueId!, params),
    enabled: !!mosqueId,
  });
}

export function useCreateReport() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: ReportCreateRequest) => createReport(body),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: reportKeys.all });
    },
  });
}
