import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";

import type { PageParams } from "@/lib/types/api.ts";

import { usersApi } from "../api/users-api.ts";
import type { UserUpdateRequest } from "../types/index.ts";
import { userKeys } from "./query-keys.ts";

const USER_SEARCH_DEBOUNCE_MS = 300;
const USER_SEARCH_PAGE_SIZE = 20;

export function useUsers(params: PageParams = {}) {
  return useQuery({
    queryKey: userKeys.list(params),
    queryFn: () => usersApi.list(params),
  });
}

/**
 * Mosque-scoped user search with a 300ms debounce. Returns the page of
 * matching users; the query is disabled until at least one character is typed.
 */
export function useUserSearch(query: string, options?: { enabled?: boolean }) {
  const [debouncedQuery, setDebouncedQuery] = useState(query);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setDebouncedQuery(query);
    }, USER_SEARCH_DEBOUNCE_MS);
    return () => window.clearTimeout(timer);
  }, [query]);

  const trimmed = debouncedQuery.trim();

  return useQuery({
    queryKey: userKeys.search(trimmed),
    queryFn: () =>
      usersApi.search({ q: trimmed, page: 0, size: USER_SEARCH_PAGE_SIZE }),
    enabled: (options?.enabled ?? true) && trimmed.length > 0,
    staleTime: 30_000,
  });
}

export function useUser(id: string, options?: { enabled?: boolean }) {
  return useQuery({
    queryKey: userKeys.detail(id),
    queryFn: () => usersApi.getById(id),
    enabled: options?.enabled ?? Boolean(id),
  });
}

export function useCurrentUser() {
  return useQuery({
    queryKey: userKeys.me(),
    queryFn: () => usersApi.getMe(),
  });
}

export function useUpdateCurrentUser() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: UserUpdateRequest) => usersApi.updateMe(body),
    onSuccess: (data) => {
      queryClient.setQueryData(userKeys.me(), data);
      void queryClient.invalidateQueries({ queryKey: userKeys.lists() });
    },
  });
}

export function useUpdateUser() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, body }: { id: string; body: UserUpdateRequest }) =>
      usersApi.update(id, body),
    onSuccess: (_data, { id }) => {
      void queryClient.invalidateQueries({ queryKey: userKeys.lists() });
      void queryClient.invalidateQueries({ queryKey: userKeys.detail(id) });
    },
  });
}

export function useDeleteUser() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => usersApi.delete(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: userKeys.lists() });
    },
  });
}
