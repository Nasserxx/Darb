import { PencilIcon, PlusIcon, Trash2Icon } from "lucide-react";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";

import { ConfirmDeleteDialog } from "@/components/shared/confirm-delete-dialog.tsx";
import { DataTable } from "@/components/shared/data-table.tsx";
import { PageHeader } from "@/components/shared/page-header.tsx";
import { SuperAdminAuditReasonDialog } from "@/components/shared/super-admin-audit-reason-dialog.tsx";
import { SuperAdminMosqueScopeBar } from "@/components/shared/super-admin-mosque-scope-bar.tsx";
import { FormerMemberBadge } from "@/components/shared/former-member-badge.tsx";
import { Badge } from "@/components/ui/badge.tsx";
import { Button } from "@/components/ui/button.tsx";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import { usePagination } from "@/lib/hooks/use-pagination.ts";
import { toMutationError } from "@/lib/errors/map-api-error.ts";
import { formatShortId } from "@/lib/format/ids.ts";
import { hasRole } from "@/lib/navigation/role-permissions.ts";

import { MosqueAdminFormDialog } from "./mosque-admin-form-dialog.tsx";
import {
  useDeleteMosqueAdmin,
  useMosqueAdmins,
} from "../hooks/use-mosque-admins.ts";
import type { MosqueAdminResponse } from "../types/index.ts";

export function MosqueAdminsList() {
  const { t } = useTranslation("app");
  const { user } = useAuth();
  const isSuperAdmin = hasRole(user?.role, ["SUPER_ADMIN"]);
  const { params, setPage, setSize, setFilter } = usePagination();
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

  async function handleDelete(auditReason?: string) {
    if (!deletingAdmin) return;
    try {
      await deleteAdmin.mutateAsync({
        id: deletingAdmin.id,
        auditReason,
      });
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

      <SuperAdminMosqueScopeBar
        mosqueId={params.mosqueId}
        q={params.q}
        nameFilter={{
          label: t("superAdminScope.filterAdminName"),
          placeholder: t("superAdminScope.filterAdminNamePlaceholder"),
        }}
        onApply={({ mosqueId, q }) => setFilter({ mosqueId, q })}
      />

      <DataTable
        columns={[
          {
            id: "userId",
            header: t("mosqueAdmins.userId"),
            cell: (row) => row.userName ?? formatShortId(row.userId),
          },
          {
            id: "mosqueId",
            header: t("mosqueAdmins.mosqueId"),
            cell: (row) => row.mosqueName ?? formatShortId(row.mosqueId),
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
            id: "status",
            header: t("mosqueAdmins.status"),
            cell: (row) =>
              row.isActive ? (
                <Badge variant="default">{t("mosqueAdmins.active")}</Badge>
              ) : (
                <FormerMemberBadge />
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
                  disabled={!row.isActive}
                  onClick={() => openEdit(row)}
                  aria-label={t("actions.edit")}
                >
                  <PencilIcon />
                </Button>
                <Button
                  variant="ghost"
                  size="icon-sm"
                  disabled={!row.isActive}
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
        onSizeChange={setSize}
        pageSize={params.size}
      />

      <MosqueAdminFormDialog
        open={formOpen}
        onOpenChange={setFormOpen}
        mosqueAdmin={editingAdmin}
      />

      {isSuperAdmin ? (
        <SuperAdminAuditReasonDialog
          open={Boolean(deletingAdmin)}
          onOpenChange={(open) => {
            if (!open) setDeletingAdmin(null);
          }}
          title={t("mosqueAdmins.deleteTitle")}
          description={t("mosqueAdmins.deleteSoftLeaveDescription")}
          confirmLabel={t("confirmDelete.confirm")}
          isPending={deleteAdmin.isPending}
          onConfirm={(reason) => void handleDelete(reason)}
        />
      ) : (
        <ConfirmDeleteDialog
          open={Boolean(deletingAdmin)}
          onOpenChange={(open) => {
            if (!open) setDeletingAdmin(null);
          }}
          title={t("mosqueAdmins.deleteTitle")}
          description={t("mosqueAdmins.deleteSoftLeaveDescription")}
          onConfirm={() => void handleDelete()}
          isPending={deleteAdmin.isPending}
        />
      )}
    </div>
  );
}
