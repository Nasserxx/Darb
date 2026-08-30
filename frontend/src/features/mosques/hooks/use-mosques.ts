import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import type { PageParams } from "@/lib/types/api.ts";
import { mosqueAdminKeys } from "@/features/mosque-admins/hooks/query-keys.ts";
import { notificationKeys } from "@/features/notifications/hooks/query-keys.ts";

import { mosquesApi } from "../api/mosques-api.ts";
import type {
  MosqueCreateRequest,
  MosqueUpdateRequest,
} from "../types/index.ts";
import type { MosqueJoinRequest } from "../types/onboard.ts";
import { mosqueKeys } from "./query-keys.ts";

export function useMosques(
  params: PageParams = {},
  options?: { enabled?: boolean },
) {
  return useQuery({
    queryKey: mosqueKeys.list(params),
    queryFn: () => mosquesApi.list(params),
    enabled: options?.enabled ?? true,
  });
}

export function useMosqueCities(
  country: string | undefined,
  options?: { activeOnly?: boolean },
) {
  const trimmed = country?.trim() ?? "";
  const activeOnly = options?.activeOnly ?? false;
  return useQuery({
    queryKey: mosqueKeys.cities(trimmed, activeOnly),
    queryFn: () => mosquesApi.listCities(trimmed, { activeOnly }),
    enabled: Boolean(trimmed),
  });
}

export function useMosque(id: string, options?: { enabled?: boolean }) {
  return useQuery({
    queryKey: mosqueKeys.detail(id),
    queryFn: () => mosquesApi.getById(id),
    enabled: options?.enabled ?? Boolean(id),
  });
}

export function useJoinPreview(code: string, options?: { enabled?: boolean }) {
  const trimmed = code.trim();
  return useQuery({
    queryKey: mosqueKeys.joinPreview(trimmed),
    queryFn: () => mosquesApi.previewJoin(trimmed),
    enabled: (options?.enabled ?? true) && trimmed.length >= 8,
    staleTime: 0,
  });
}

export function useOnboardMosque() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: MosqueCreateRequest) => mosquesApi.onboard(body),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: mosqueKeys.lists() });
      void queryClient.invalidateQueries({ queryKey: mosqueAdminKeys.lists() });
    },
  });
}

export function useJoinMosque() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: MosqueJoinRequest) => mosquesApi.join(body),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: mosqueKeys.lists() });
      void queryClient.invalidateQueries({ queryKey: mosqueAdminKeys.lists() });
    },
  });
}

export function useCreateMosque() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: MosqueCreateRequest) => mosquesApi.create(body),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: mosqueKeys.lists() });
    },
  });
}

export function useUpdateMosque() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, body }: { id: string; body: MosqueUpdateRequest }) =>
      mosquesApi.update(id, body),
    onSuccess: (_data, { id }) => {
      void queryClient.invalidateQueries({ queryKey: mosqueKeys.lists() });
      void queryClient.invalidateQueries({ queryKey: mosqueKeys.detail(id) });
    },
  });
}

export function useDeleteMosque() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => mosquesApi.delete(id),
    onSuccess: (_data, id) => {
      void queryClient.invalidateQueries({ queryKey: mosqueKeys.lists() });
      void queryClient.invalidateQueries({ queryKey: mosqueKeys.detail(id) });
    },
  });
}

export function useMosqueInviteCodes(
  id: string,
  options?: { enabled?: boolean },
) {
  return useQuery({
    queryKey: mosqueKeys.inviteCodes(id),
    queryFn: () => mosquesApi.getInviteCodesForMosque(id),
    enabled: (options?.enabled ?? true) && Boolean(id),
  });
}

export function useRotateInviteCodes(id: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => mosquesApi.rotateInviteCodes(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: mosqueKeys.inviteCodes(id),
      });
    },
  });
}

export function useReactivateMosque() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => mosquesApi.reactivate(id),
    onSuccess: (_data, id) => {
      void queryClient.invalidateQueries({ queryKey: mosqueKeys.lists() });
      void queryClient.invalidateQueries({ queryKey: mosqueKeys.detail(id) });
    },
  });
}

export function useMyInvitations(options?: { enabled?: boolean }) {
  return useQuery({
    queryKey: mosqueKeys.myInvitations(),
    queryFn: () => mosquesApi.listMyInvitations(),
    enabled: options?.enabled ?? true,
  });
}

export function useAcceptJoinRequest() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => mosquesApi.acceptJoinRequest(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: mosqueKeys.myInvitations() });
      void queryClient.invalidateQueries({ queryKey: notificationKeys.all });
      void queryClient.invalidateQueries({ queryKey: mosqueKeys.lists() });
    },
  });
}

export function useRefuseJoinRequest() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => mosquesApi.refuseJoinRequest(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: mosqueKeys.myInvitations() });
      void queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}
