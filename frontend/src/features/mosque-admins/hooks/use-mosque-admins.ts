import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import type { PageParams } from "@/lib/types/api.ts";

import { mosqueAdminsApi } from "../api/mosque-admins-api.ts";
import type {
  MosqueAdminCreateRequest,
  MosqueAdminUpdateRequest,
} from "../types/index.ts";
import { mosqueAdminKeys } from "./query-keys.ts";

export function useMosqueAdmins(params: PageParams = {}) {
  return useQuery({
    queryKey: mosqueAdminKeys.list(params),
    queryFn: () => mosqueAdminsApi.list(params),
  });
}

export function useMosqueAdmin(id: string, options?: { enabled?: boolean }) {
  return useQuery({
    queryKey: mosqueAdminKeys.detail(id),
    queryFn: () => mosqueAdminsApi.getById(id),
    enabled: options?.enabled ?? Boolean(id),
  });
}

export function useCreateMosqueAdmin() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      body,
      auditReason,
    }: {
      body: MosqueAdminCreateRequest;
      auditReason?: string;
    }) => mosqueAdminsApi.create(body, auditReason),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: mosqueAdminKeys.lists() });
    },
  });
}

export function useUpdateMosqueAdmin() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      id,
      body,
      auditReason,
    }: {
      id: string;
      body: MosqueAdminUpdateRequest;
      auditReason?: string;
    }) => mosqueAdminsApi.update(id, body, auditReason),
    onSuccess: (_data, { id }) => {
      void queryClient.invalidateQueries({ queryKey: mosqueAdminKeys.lists() });
      void queryClient.invalidateQueries({
        queryKey: mosqueAdminKeys.detail(id),
      });
    },
  });
}

export function useDeleteMosqueAdmin() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      id,
      auditReason,
    }: {
      id: string;
      auditReason?: string;
    }) => mosqueAdminsApi.delete(id, auditReason),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: mosqueAdminKeys.lists() });
    },
  });
}
