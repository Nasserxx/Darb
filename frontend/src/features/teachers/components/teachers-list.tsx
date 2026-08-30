import { PencilIcon, PlusIcon, Trash2Icon } from "lucide-react";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";

import { ConfirmDeleteDialog } from "@/components/shared/confirm-delete-dialog.tsx";
import { DataTable } from "@/components/shared/data-table.tsx";
import { PageHeader } from "@/components/shared/page-header.tsx";
import { SuperAdminMosqueScopeBar } from "@/components/shared/super-admin-mosque-scope-bar.tsx";
import { FormerMemberBadge } from "@/components/shared/former-member-badge.tsx";
import { Badge } from "@/components/ui/badge.tsx";
import { Button } from "@/components/ui/button.tsx";
import { usePagination } from "@/lib/hooks/use-pagination.ts";
import { toMutationError } from "@/lib/errors/map-api-error.ts";
import { formatShortId } from "@/lib/format/ids.ts";

import { TeacherFormDialog } from "./teacher-form-dialog.tsx";
import { useDeleteTeacher, useTeachers } from "../hooks/use-teachers.ts";
import type { TeacherResponse } from "../types/index.ts";

export function TeachersList() {
  const { t } = useTranslation("app");
  const { params, setPage, setSize, setFilter } = usePagination();
  const { data, isLoading } = useTeachers(params);
  const deleteTeacher = useDeleteTeacher();

  const [formOpen, setFormOpen] = useState(false);
  const [editingTeacher, setEditingTeacher] = useState<TeacherResponse | null>(null);
  const [deletingTeacher, setDeletingTeacher] = useState<TeacherResponse | null>(null);

  function openCreate() {
    setEditingTeacher(null);
    setFormOpen(true);
  }

  function openEdit(teacher: TeacherResponse) {
    setEditingTeacher(teacher);
    setFormOpen(true);
  }

  async function handleDelete() {
    if (!deletingTeacher) return;
    try {
      await deleteTeacher.mutateAsync(deletingTeacher.id);
      toast.success(t("teachers.deleteSuccess"));
      setDeletingTeacher(null);
    } catch (error) {
      toast.error(toMutationError(error, t).message);
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={t("teachers.title")}
        description={t("teachers.description")}
        actions={
          <Button onClick={openCreate}>
            <PlusIcon data-icon="inline-start" />
            {t("teachers.create")}
          </Button>
        }
      />

      <SuperAdminMosqueScopeBar
        mosqueId={params.mosqueId}
        q={params.q}
        nameFilter={{
          label: t("superAdminScope.filterTeacherName"),
          placeholder: t("superAdminScope.filterTeacherNamePlaceholder"),
        }}
        onApply={({ mosqueId, q }) => setFilter({ mosqueId, q })}
      />

      <DataTable
        columns={[
          {
            id: "userId",
            header: t("teachers.userId"),
            cell: (row) => row.userName ?? formatShortId(row.userId),
          },
          {
            id: "specialization",
            header: t("teachers.specialization"),
            cell: (row) => row.specialization ?? "—",
          },
          {
            id: "yearsExperience",
            header: t("teachers.yearsExperience"),
            cell: (row) => row.yearsExperience ?? "—",
          },
          {
            id: "availability",
            header: t("teachers.isAvailable"),
            cell: (row) => (
              <Badge variant={row.isAvailable ? "default" : "secondary"}>
                {row.isAvailable ? t("teachers.available") : t("teachers.unavailable")}
              </Badge>
            ),
          },
          {
            id: "status",
            header: t("teachers.status"),
            cell: (row) =>
              row.isActive ? (
                <Badge variant="default">{t("teachers.active")}</Badge>
              ) : (
                <FormerMemberBadge />
              ),
          },
          {
            id: "actions",
            header: t("teachers.actions"),
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
                  onClick={() => setDeletingTeacher(row)}
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

      <TeacherFormDialog
        open={formOpen}
        onOpenChange={setFormOpen}
        teacher={editingTeacher}
      />

      <ConfirmDeleteDialog
        open={Boolean(deletingTeacher)}
        onOpenChange={(open) => {
          if (!open) setDeletingTeacher(null);
        }}
        onConfirm={() => void handleDelete()}
        isPending={deleteTeacher.isPending}
      />
    </div>
  );
}
