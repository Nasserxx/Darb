import { PencilIcon, PlusIcon, Trash2Icon } from "lucide-react";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";

import { ConfirmDeleteDialog } from "@/components/shared/confirm-delete-dialog.tsx";
import { DataTable } from "@/components/shared/data-table.tsx";
import { PageHeader } from "@/components/shared/page-header.tsx";
import { Badge } from "@/components/ui/badge.tsx";
import { Button } from "@/components/ui/button.tsx";
import { usePagination } from "@/lib/hooks/use-pagination.ts";
import { toMutationError } from "@/lib/errors/map-api-error.ts";

import { MosqueAdminFormDialog } from "./mosque-admin-form-dialog.tsx";
import {
  useDeleteMosqueAdmin,
  useMosqueAdmins,
} from "../hooks/use-mosque-admins.ts";
import type { MosqueAdminResponse } from "../types/index.ts";

export function MosqueAdminsList() {
  const { t } = useTranslation("app");
  const { params, setPage } = usePagination();
  const { data, isLoading } = useMosqueAdmins(params);
  const deleteAdmin = useDeleteMosqueAdmin();

  const [formOpen, setFormOpen] = useState(false);
  const [editingAdmin, setEditingAdmin] = useState<MosqueAdminResponse | null>(null);
  const [deletingAdmin, setDeletingAdmin] = useState<MosqueAdminResponse | null>(null);

  function openCreate() {
    setEditingAdmin(null);
    setFormOpen(true);
  }

  function openEdit(admin: MosqueAdminResponse) {
    setEditingAdmin(admin);
    setFormOpen(true);
  }

  async function handleDelete() {
    if (!deletingAdmin) return;
    try {
      await deleteAdmin.mutateAsync(deletingAdmin.id);
      toast.success(t("mosqueAdmins.deleteSuccess"));
      setDeletingAdmin(null);
    } catch (error) {
      toast.error(toMutationError(error, t).message);
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={t("mosqueAdmins.title")}
        description={t("mosqueAdmins.description")}
        actions={
          <Button onClick={openCreate}>
            <PlusIcon data-icon="inline-start" />
            {t("mosqueAdmins.create")}
          </Button>
        }
      />

      <DataTable
        columns={[
          {
            id: "userId",
            header: t("mosqueAdmins.userId"),
            cell: (row) => (
              <span className="font-mono text-xs">{row.userId.slice(0, 8)}…</span>
            ),
          },
          {
            id: "mosqueId",
            header: t("mosqueAdmins.mosqueId"),
            cell: (row) => (
              <span className="font-mono text-xs">{row.mosqueId.slice(0, 8)}…</span>
            ),
          },
          {
            id: "permission",
            header: t("mosqueAdmins.permission"),
            cell: (row) => t(`mosqueAdmins.permissions.${row.permission}`),
          },
          {
            id: "primary",
            header: t("mosqueAdmins.isPrimaryAdmin"),
            cell: (row) =>
              row.isPrimaryAdmin ? (
                <Badge>{t("mosqueAdmins.primaryYes")}</Badge>
              ) : (
                "—"
              ),
          },
          {
            id: "actions",
            header: t("mosqueAdmins.actions"),
            className: "w-28 text-end",
            cell: (row) => (
              <div className="flex justify-end gap-1">
                <Button
                  variant="ghost"
                  size="icon-sm"
                  onClick={() => openEdit(row)}
                  aria-label={t("actions.edit")}
                >
                  <PencilIcon />
                </Button>
                <Button
                  variant="ghost"
                  size="icon-sm"
                  onClick={() => setDeletingAdmin(row)}
                  aria-label={t("actions.delete")}
                >
                  <Trash2Icon />
                </Button>
              </div>
            ),
          },
        ]}
        data={data}
        isLoading={isLoading}
        onPageChange={setPage}
      />

      <MosqueAdminFormDialog
        open={formOpen}
        onOpenChange={setFormOpen}
        mosqueAdmin={editingAdmin}
      />

      <ConfirmDeleteDialog
        open={Boolean(deletingAdmin)}
        onOpenChange={(open) => {
          if (!open) setDeletingAdmin(null);
        }}
        onConfirm={() => void handleDelete()}
        isPending={deleteAdmin.isPending}
      />
    </div>
  );
}
