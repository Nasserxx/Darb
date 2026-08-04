import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import type { PageParams } from "@/lib/types/api.ts";
import {
  createMessage,
  getCircleMessages,
  getMyMessages,
  markMessageAsRead,
} from "../api/messages-api.ts";
import type { MessageCreateRequest } from "../types/index.ts";
import { messageKeys } from "./query-keys.ts";

export function useMyMessages(params?: PageParams) {
  return useQuery({
    queryKey: messageKeys.mine(params),
    queryFn: () => getMyMessages(params),
  });
}

export function useCircleMessages(
  circleId: string | undefined,
  params?: PageParams,
) {
  return useQuery({
    queryKey: messageKeys.circle(circleId ?? "", params),
    queryFn: () => getCircleMessages(circleId!, params),
    enabled: !!circleId,
  });
}

export function useCreateMessage() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: MessageCreateRequest) => createMessage(body),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: messageKeys.all });
    },
  });
}

export function useMarkMessageAsRead() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => markMessageAsRead(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: messageKeys.all });
    },
  });
}
