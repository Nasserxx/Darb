import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import type { PageParams } from "@/lib/types/api.ts";

import { teachersApi } from "../api/teachers-api.ts";
import type {
  TeacherCreateRequest,
  TeacherUpdateRequest,
} from "../types/index.ts";
import { teacherKeys } from "./query-keys.ts";

export function useTeachers(params: PageParams = {}) {
  return useQuery({
    queryKey: teacherKeys.list(params),
    queryFn: () => teachersApi.list(params),
  });
}

export function useTeacher(id: string, options?: { enabled?: boolean }) {
  return useQuery({
    queryKey: teacherKeys.detail(id),
    queryFn: () => teachersApi.getById(id),
    enabled: options?.enabled ?? Boolean(id),
  });
}

export function useCreateTeacher() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: TeacherCreateRequest) => teachersApi.create(body),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: teacherKeys.lists() });
    },
  });
}

export function useUpdateTeacher() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, body }: { id: string; body: TeacherUpdateRequest }) =>
      teachersApi.update(id, body),
    onSuccess: (_data, { id }) => {
      void queryClient.invalidateQueries({ queryKey: teacherKeys.lists() });
      void queryClient.invalidateQueries({ queryKey: teacherKeys.detail(id) });
    },
  });
}

export function useDeleteTeacher() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => teachersApi.delete(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: teacherKeys.lists() });
    },
  });
}
