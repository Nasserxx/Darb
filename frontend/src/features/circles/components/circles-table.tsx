import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";

import { ConfirmDeleteDialog } from "@/components/shared/confirm-delete-dialog.tsx";
import { DataTable } from "@/components/shared/data-table.tsx";
import { SuperAdminMosqueScopeBar } from "@/components/shared/super-admin-mosque-scope-bar.tsx";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { CircleFormDialog } from "@/features/circles/components/circle-form-dialog.tsx";
import {
  useCircles,
  useDeleteCircle,
} from "@/features/circles/hooks/use-circles.ts";
import type { CircleResponse } from "@/features/circles/types/index.ts";
import { toMutationError } from "@/lib/errors/map-api-error.ts";
import { usePagination } from "@/lib/hooks/use-pagination.ts";

function formatSchedule(circle: CircleResponse): string {
  const days = circle.daysOfWeek ?? "—";
  const start = circle.startTime?.slice(0, 5) ?? "—";
  const end = circle.endTime?.slice(0, 5) ?? "—";
  return `${days} · ${start}–${end}`;
}

type CirclesTableProps = {
  canWrite: boolean;
  /** Optional set of circle IDs to filter by (e.g. for student enrollment filtering) */
  circleIds?: Set<string>;
};

export function CirclesTable({ canWrite, circleIds }: CirclesTableProps) {
  const { t } = useTranslation("app");
  const { params, setPage, setSize, setFilter } = usePagination();
  const { data, isLoading } = useCircles(circleIds ? { page: 0, size: 500 } : params);
  const deleteMutation = useDeleteCircle();
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<CircleResponse | null>(null);
  const [deleting, setDeleting] = useState<CircleResponse | null>(null);

  const filteredData = useMemo(() => {
    if (!data || !circleIds) return data;
    const filtered = data.content.filter((c) => circleIds.has(c.id));
    return { ...data, content: filtered, totalElements: filtered.length };
  }, [data, circleIds]);

  async function handleDelete() {
    if (!deleting) return;
    try {
      await deleteMutation.mutateAsync(deleting.id);
      toast.success(t("circles.deleteSuccess"));
      setDeleting(null);
    } catch (error) {
      toast.error(toMutationError(error, t).message);
    }
  }

  return (
    <>
      {!circleIds ? (
        <SuperAdminMosqueScopeBar
          mosqueId={params.mosqueId}
          q={params.q}
          nameFilter={{
            label: t("superAdminScope.filterHalaqaName"),
            placeholder: t("superAdminScope.filterHalaqaNamePlaceholder"),
          }}
          onApply={({ mosqueId, q }) => setFilter({ mosqueId, q })}
        />
      ) : null}
      <DataTable
        data={filteredData}
        isLoading={isLoading}
        onPageChange={setPage}
        onSizeChange={setSize}
        pageSize={params.size}
        columns={[
          {
            id: "name",
            header: t("circles.name"),
            cell: (row) => <span className="font-medium">{row.name}</span>,
          },
          {
            id: "level",
            header: t("circles.level"),
            cell: (row) => t(`enums.circleLevel.${row.level}`),
          },
          {
            id: "type",
            header: t("circles.type"),
            cell: (row) => t(`enums.circleType.${row.type}`),
          },
          {
            id: "schedule",
            header: t("circles.schedule"),
            cell: (row) => (
              <span className="text-muted-foreground">{formatSchedule(row)}</span>
            ),
          },
          {
            id: "capacity",
            header: t("circles.capacity"),
            cell: (row) => row.capacity ?? "—",
          },
          {
            id: "status",
            header: t("circles.status"),
            cell: (row) => (
              <Badge variant={row.status === "ACTIVE" ? "default" : "outline"}>
                {t(`enums.circleStatus.${row.status}`)}
              </Badge>
            ),
          },
          ...(canWrite
            ? [
                {
                  id: "actions",
                  header: "",
                  className: "w-32 text-end",
                  cell: (row: CircleResponse) => (
                    <div className="flex justify-end gap-1">
                      <Button
                        variant="ghost"
                        size="sm"
                        disabled={row.status === "ENDED"}
                        onClick={() => {
                          setEditing(row);
                          setFormOpen(true);
                        }}
                      >
                        {t("actions.edit")}
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="text-destructive"
                        disabled={row.status === "ENDED"}
                        onClick={() => setDeleting(row)}
                      >
                        {t("actions.delete")}
                      </Button>
                    </div>
                  ),
                },
              ]
            : []),
        ]}
      />
      {canWrite ? (
        <>
          <CircleFormDialog
            open={formOpen}
            onOpenChange={(open) => {
              setFormOpen(open);
              if (!open) setEditing(null);
            }}
            circle={editing}
          />
          <ConfirmDeleteDialog
            open={Boolean(deleting)}
            onOpenChange={(open) => {
              if (!open) setDeleting(null);
            }}
            onConfirm={() => void handleDelete()}
            isPending={deleteMutation.isPending}
          />
        </>
      ) : null}
    </>
  );
}
