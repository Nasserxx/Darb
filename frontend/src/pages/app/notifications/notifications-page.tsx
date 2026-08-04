import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";

import { DataTable } from "@/components/shared/data-table.tsx";
import { PageHeader } from "@/components/shared/page-header.tsx";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  useMarkNotificationAsRead,
  useMyNotifications,
} from "@/features/notifications/hooks/use-notifications.ts";
import type { NotificationResponse } from "@/features/notifications/types/index.ts";
import { toMutationError } from "@/lib/errors/map-api-error.ts";
import { usePagination } from "@/lib/hooks/use-pagination.ts";

function formatInstant(value?: string | null): string {
  if (!value) return "—";
  return new Date(value).toLocaleString();
}

export function NotificationsPage() {
  const { t } = useTranslation("app");
  const { params, setPage } = usePagination();
  const { data, isLoading } = useMyNotifications(params);
  const markRead = useMarkNotificationAsRead();

  const columns = useMemo(
    () => [
      {
        id: "title",
        header: t("notifications.columnTitle"),
        cell: (row: NotificationResponse) => (
          <span className="font-medium">{row.title}</span>
        ),
      },
      {
        id: "body",
        header: t("notifications.columnBody"),
        cell: (row: NotificationResponse) => (
          <span className="line-clamp-2 max-w-md text-sm text-muted-foreground">
            {row.body}
          </span>
        ),
      },
      {
        id: "channel",
        header: t("notifications.channel"),
        cell: (row: NotificationResponse) => (
          <Badge variant="outline">{row.channel}</Badge>
        ),
      },
      {
        id: "status",
        header: t("notifications.status"),
        cell: (row: NotificationResponse) => (
          <Badge variant={row.status === "READ" ? "secondary" : "default"}>
            {row.status}
          </Badge>
        ),
      },
      {
        id: "sentAt",
        header: t("notifications.sentAt"),
        cell: (row: NotificationResponse) => (
          <span className="text-sm text-muted-foreground">
            {formatInstant(row.sentAt)}
          </span>
        ),
      },
      {
        id: "actions",
        header: "",
        className: "w-32 text-right",
        cell: (row: NotificationResponse) =>
          row.status !== "READ" ? (
            <Button
              variant="ghost"
              size="sm"
              disabled={markRead.isPending}
              onClick={() => void handleMarkRead(row.id)}
            >
              {t("actions.markRead")}
            </Button>
          ) : null,
      },
    ],
    [markRead.isPending, t],
  );

  async function handleMarkRead(id: string) {
    try {
      await markRead.mutateAsync(id);
      toast.success(t("notifications.markReadSuccess"));
    } catch (error) {
      toast.error(toMutationError(error, t).message);
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={t("notifications.title")}
        description={t("notifications.description")}
      />
      <DataTable
        columns={columns}
        data={data}
        isLoading={isLoading}
        emptyMessage={t("notifications.empty")}
        onPageChange={setPage}
      />
    </div>
  );
}
