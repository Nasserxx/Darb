import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import type { PageParams } from "@/lib/types/api.ts";
import { parentStudentsApi } from "../api/parent-students-api.ts";
import type {
  ParentStudentCreateRequest,
  ParentStudentUpdateRequest,
} from "../types/index.ts";
import { parentStudentKeys } from "./query-keys.ts";

export function useParentStudents(params: PageParams = {}) {
  return useQuery({
    queryKey: parentStudentKeys.list(params),
    queryFn: () => parentStudentsApi.list(params),
  });
}

export function useParentStudent(id: string, options?: { enabled?: boolean }) {
  return useQuery({
    queryKey: parentStudentKeys.detail(id),
    queryFn: () => parentStudentsApi.getById(id),
    enabled: options?.enabled ?? Boolean(id),
  });
}

export function useMyChildren() {
  return useQuery({
    queryKey: [...parentStudentKeys.all, "my-children"],
    queryFn: () => parentStudentsApi.getMyChildren(),
  });
}

export function useCreateParentStudent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: ParentStudentCreateRequest) =>
      parentStudentsApi.create(body),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: parentStudentKeys.lists(),
      });
    },
  });
}

export function useUpdateParentStudent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      id,
      body,
    }: {
      id: string;
      body: ParentStudentUpdateRequest;
    }) => parentStudentsApi.update(id, body),
    onSuccess: (_data, { id }) => {
      void queryClient.invalidateQueries({
        queryKey: parentStudentKeys.lists(),
      });
      void queryClient.invalidateQueries({
        queryKey: parentStudentKeys.detail(id),
      });
    },
  });
}

export function useDeleteParentStudent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => parentStudentsApi.delete(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: parentStudentKeys.lists(),
      });
    },
  });
}
