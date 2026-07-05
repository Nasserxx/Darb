import { PencilIcon, PlusIcon, Trash2Icon } from "lucide-react";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";

import { ConfirmDeleteDialog } from "@/components/shared/confirm-delete-dialog.tsx";
import { DataTable } from "@/components/shared/data-table.tsx";
import { PageHeader } from "@/components/shared/page-header.tsx";
import { Badge } from "@/components/ui/badge.tsx";
import { Button } from "@/components/ui/button.tsx";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import { useWorkspace } from "@/features/workspace/context/workspace-provider.tsx";
import { normalizeApiRole } from "@/lib/navigation/app-nav.ts";
import { usePagination } from "@/lib/hooks/use-pagination.ts";
import { toMutationError } from "@/lib/errors/map-api-error.ts";

import { MosqueFormDialog } from "./mosque-form-dialog.tsx";
import { useDeleteMosque, useMosques } from "../hooks/use-mosques.ts";
import type { MosqueResponse } from "../types/index.ts";

export function MosquesList() {
  const { t } = useTranslation("app");
  const { user } = useAuth();
  const { mosqueId } = useWorkspace();
  const role = user ? normalizeApiRole(user.role) : null;
  const canCreate = role === "SUPER_ADMIN";
  const canDelete = role === "SUPER_ADMIN";
  const canEditRow = (mosque: MosqueResponse) =>
    role === "SUPER_ADMIN" || (role === "MOSQUE_ADMIN" && mosque.id === mosqueId);
  const { params, setPage } = usePagination();
  const { data, isLoading } = useMosques(params);
  const deleteMosque = useDeleteMosque();

  const [formOpen, setFormOpen] = useState(false);
  const [editingMosque, setEditingMosque] = useState<MosqueResponse | null>(null);
  const [deletingMosque, setDeletingMosque] = useState<MosqueResponse | null>(null);

  function openCreate() {
    setEditingMosque(null);
    setFormOpen(true);
  }

  function openEdit(mosque: MosqueResponse) {
    setEditingMosque(mosque);
    setFormOpen(true);
  }

  async function handleDelete() {
    if (!deletingMosque) return;
    try {
      await deleteMosque.mutateAsync(deletingMosque.id);
      toast.success(t("mosques.deleteSuccess"));
      setDeletingMosque(null);
    } catch (error) {
      toast.error(toMutationError(error, t).message);
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={t("mosques.title")}
        description={t("mosques.description")}
        actions={
          canCreate ? (
            <Button onClick={openCreate}>
              <PlusIcon data-icon="inline-start" />
              {t("mosques.create")}
            </Button>
          ) : null
        }
      />

      <DataTable
        columns={[
          {
            id: "name",
            header: t("mosques.name"),
            cell: (row) => row.name,
          },
          {
            id: "city",
            header: t("mosques.city"),
            cell: (row) => row.city ?? "—",
          },
          {
            id: "phone",
            header: t("mosques.phone"),
            cell: (row) => row.phone ?? "—",
          },
          {
            id: "status",
            header: t("mosques.status"),
            cell: (row) => (
              <Badge variant={row.isActive ? "default" : "secondary"}>
                {row.isActive ? t("mosques.active") : t("mosques.inactive")}
              </Badge>
            ),
          },
          ...(role === "SUPER_ADMIN" || role === "MOSQUE_ADMIN"
            ? [
                {
                  id: "actions",
                  header: t("mosques.actions"),
                  className: "w-28 text-end",
                  cell: (row: MosqueResponse) => (
                    <div className="flex justify-end gap-1">
                      {canEditRow(row) ? (
                        <Button
                          variant="ghost"
                          size="icon-sm"
                          onClick={() => openEdit(row)}
                          aria-label={t("actions.edit")}
                        >
                          <PencilIcon />
                        </Button>
                      ) : null}
                      {canDelete ? (
                        <Button
                          variant="ghost"
                          size="icon-sm"
                          onClick={() => setDeletingMosque(row)}
                          aria-label={t("actions.delete")}
                        >
                          <Trash2Icon />
                        </Button>
                      ) : null}
                    </div>
                  ),
                },
              ]
            : []),
        ]}
        data={data}
        isLoading={isLoading}
        onPageChange={setPage}
      />

      <MosqueFormDialog
        open={formOpen}
        onOpenChange={setFormOpen}
        mosque={editingMosque}
      />

      <ConfirmDeleteDialog
        open={Boolean(deletingMosque)}
        onOpenChange={(open) => {
          if (!open) setDeletingMosque(null);
        }}
        onConfirm={() => void handleDelete()}
        isPending={deleteMosque.isPending}
      />
    </div>
  );
}
