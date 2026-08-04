import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import type { PageParams } from "@/lib/types/api.ts";
import {
  createNotification,
  getMyNotifications,
  markNotificationAsRead,
} from "../api/notifications-api.ts";
import type { NotificationCreateRequest } from "../types/index.ts";
import { notificationKeys } from "./query-keys.ts";

export function useMyNotifications(params?: PageParams) {
  return useQuery({
    queryKey: notificationKeys.mine(params),
    queryFn: () => getMyNotifications(params),
  });
}

export function useMarkNotificationAsRead() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => markNotificationAsRead(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}

export function useCreateNotification() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: NotificationCreateRequest) => createNotification(body),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}
