import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";
import { toast } from "sonner";

import { DataTable } from "@/components/shared/data-table.tsx";
import { PageHeader } from "@/components/shared/page-header.tsx";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import {
  useMarkMessageAsRead,
  useMyMessages,
} from "@/features/messages/hooks/use-messages.ts";
import type { MessageResponse } from "@/features/messages/types/index.ts";
import { DEFAULT_LOCALE } from "@/i18n/index.ts";
import { usePagination } from "@/lib/hooks/use-pagination.ts";
import { toMutationError } from "@/lib/errors/map-api-error.ts";

function shortId(value: string): string {
  return `${value.slice(0, 8)}…`;
}

function formatInstant(value: string): string {
  return new Date(value).toLocaleString();
}

export function MessagesPage() {
  const { t } = useTranslation("app");
  const { user } = useAuth();
  const { locale } = useParams<{ locale: string }>();
  const localePrefix = locale ?? DEFAULT_LOCALE;
  const { params, setPage } = usePagination();
  const { data, isLoading } = useMyMessages(params);
  const markRead = useMarkMessageAsRead();

  const columns = useMemo(
    () => [
      {
        id: "content",
        header: t("messages.content"),
        cell: (row: MessageResponse) => (
          <span className="line-clamp-2 max-w-md text-sm">{row.content}</span>
        ),
      },
      {
        id: "circle",
        header: t("messages.circle"),
        cell: (row: MessageResponse) => (
          <Link
            to={`/${localePrefix}/messages/circle/${row.circleId}`}
            className="text-sm font-medium text-primary hover:underline"
          >
            {shortId(row.circleId)}
          </Link>
        ),
      },
      {
        id: "status",
        header: t("messages.status"),
        cell: (row: MessageResponse) => (
          <Badge variant={row.status === "READ" ? "secondary" : "default"}>
            {row.status}
          </Badge>
        ),
      },
      {
        id: "sentAt",
        header: t("messages.sentAt"),
        cell: (row: MessageResponse) => (
          <span className="text-sm text-muted-foreground">
            {formatInstant(row.sentAt)}
          </span>
        ),
      },
      {
        id: "actions",
        header: "",
        className: "w-40 text-right",
        cell: (row: MessageResponse) => {
          const isReceiver = user?.userId === row.receiverId;
          const canMarkRead = isReceiver && row.status !== "READ";

          return (
            <div className="flex justify-end gap-2">
              <Button variant="outline" size="sm" asChild>
                <Link to={`/${localePrefix}/messages/circle/${row.circleId}`}>
                  {t("actions.view")}
                </Link>
              </Button>
              {canMarkRead ? (
                <Button
                  variant="ghost"
                  size="sm"
                  disabled={markRead.isPending}
                  onClick={() => void handleMarkRead(row.id)}
                >
                  {t("actions.markRead")}
                </Button>
              ) : null}
            </div>
          );
        },
      },
    ],
    [localePrefix, markRead.isPending, t, user?.userId],
  );

  async function handleMarkRead(id: string) {
    try {
      await markRead.mutateAsync(id);
      toast.success(t("messages.markReadSuccess"));
    } catch (error) {
      toast.error(toMutationError(error, t).message);
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={t("messages.title")}
        description={t("messages.description")}
      />
      <DataTable
        columns={columns}
        data={data}
        isLoading={isLoading}
        emptyMessage={t("messages.emptyInbox")}
        onPageChange={setPage}
      />
    </div>
  );
}
