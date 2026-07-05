import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import type { PageParams } from "@/lib/types/api.ts";
import { studentsApi } from "../api/students-api.ts";
import type {
  StudentCreateRequest,
  StudentUpdateRequest,
} from "../types/index.ts";
import { studentKeys } from "./query-keys.ts";

export function useStudents(params: PageParams = {}) {
  return useQuery({
    queryKey: studentKeys.list(params),
    queryFn: () => studentsApi.list(params),
  });
}

export function useStudent(id: string, options?: { enabled?: boolean }) {
  return useQuery({
    queryKey: studentKeys.detail(id),
    queryFn: () => studentsApi.getById(id),
    enabled: options?.enabled ?? Boolean(id),
  });
}

export function useCreateStudent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: StudentCreateRequest) => studentsApi.create(body),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: studentKeys.lists() });
    },
  });
}

export function useUpdateStudent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, body }: { id: string; body: StudentUpdateRequest }) =>
      studentsApi.update(id, body),
    onSuccess: (_data, { id }) => {
      void queryClient.invalidateQueries({ queryKey: studentKeys.lists() });
      void queryClient.invalidateQueries({ queryKey: studentKeys.detail(id) });
    },
  });
}

export function useDeleteStudent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => studentsApi.delete(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: studentKeys.lists() });
    },
  });
}
