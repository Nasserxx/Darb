import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import type { PageParams } from "@/lib/types/api.ts";
import { circlesApi } from "../api/circles-api.ts";
import type {
  CircleCreateRequest,
  CircleUpdateRequest,
} from "../types/index.ts";
import { circleKeys } from "./query-keys.ts";

export function useCircles(
  params: PageParams = {},
  options?: { enabled?: boolean },
) {
  return useQuery({
    queryKey: circleKeys.list(params),
    queryFn: () => circlesApi.list(params),
    enabled: options?.enabled ?? true,
  });
}

export function useCircle(id: string, options?: { enabled?: boolean }) {
  return useQuery({
    queryKey: circleKeys.detail(id),
    queryFn: () => circlesApi.getById(id),
    enabled: options?.enabled ?? Boolean(id),
  });
}

export function useCreateCircle() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: CircleCreateRequest) => circlesApi.create(body),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: circleKeys.lists() });
    },
  });
}

export function useUpdateCircle() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, body }: { id: string; body: CircleUpdateRequest }) =>
      circlesApi.update(id, body),
    onSuccess: (_data, { id }) => {
      void queryClient.invalidateQueries({ queryKey: circleKeys.lists() });
      void queryClient.invalidateQueries({ queryKey: circleKeys.detail(id) });
    },
  });
}

export function useDeleteCircle() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => circlesApi.delete(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: circleKeys.lists() });
    },
  });
}
