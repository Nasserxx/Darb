import { useState } from "react";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";

import { DataTable } from "@/components/shared/data-table.tsx";
import { SuperAdminAuditReasonDialog } from "@/components/shared/super-admin-audit-reason-dialog.tsx";
import { SuperAdminMosqueScopeBar } from "@/components/shared/super-admin-mosque-scope-bar.tsx";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import { ParentStudentFormDialog } from "@/features/parent-students/components/parent-student-form-dialog.tsx";
import {
  useDeleteParentStudent,
  useParentStudents,
} from "@/features/parent-students/hooks/use-parent-students.ts";
import type { ParentStudentResponse } from "@/features/parent-students/types/index.ts";
import { formatShortId } from "@/lib/format/ids.ts";
import { toMutationError } from "@/lib/errors/map-api-error.ts";
import { usePagination } from "@/lib/hooks/use-pagination.ts";
import { hasRole } from "@/lib/navigation/role-permissions.ts";

export function ParentStudentsTable() {
  const { t } = useTranslation("app");
  const { user } = useAuth();
  const isSuperAdmin = hasRole(user?.role, ["SUPER_ADMIN"]);
  const { params, setPage, setSize, setFilter } = usePagination();
  const { data, isLoading } = useParentStudents(params);
  const deleteMutation = useDeleteParentStudent();
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<ParentStudentResponse | null>(null);
  const [deleting, setDeleting] = useState<ParentStudentResponse | null>(null);

  async function handleDelete(auditReason?: string) {
    if (!deleting) return;
    try {
      await deleteMutation.mutateAsync({
        id: deleting.id,
        auditReason,
      });
      toast.success(t("parentStudents.deleteSuccess"));
      setDeleting(null);
    } catch (error) {
      toast.error(toMutationError(error, t).message);
    }
  }

  return (
    <>
      <SuperAdminMosqueScopeBar
        mosqueId={params.mosqueId}
        q={params.q}
        nameFilter={{
          label: t("superAdminScope.filterParentName"),
          placeholder: t("superAdminScope.filterParentNamePlaceholder"),
        }}
        onApply={({ mosqueId, q }) => setFilter({ mosqueId, q })}
      />
      <DataTable
        data={data}
        isLoading={isLoading}
        onPageChange={setPage}
        onSizeChange={setSize}
        pageSize={params.size}
        columns={[
          {
            id: "parent",
            header: t("parentStudents.parentUserId"),
            cell: (row) => row.parentName ?? formatShortId(row.parentUserId),
          },
          {
            id: "student",
            header: t("parentStudents.studentId"),
            cell: (row) => row.studentName ?? formatShortId(row.studentId),
          },
          {
            id: "relationship",
            header: t("parentStudents.relationship"),
            cell: (row) =>
              row.relationship
                ? t(`enums.parentRelationship.${row.relationship}`)
                : "—",
          },
          {
            id: "isPrimary",
            header: t("parentStudents.isPrimary"),
            cell: (row) =>
              row.isPrimary ? (
                <Badge>{t("parentStudents.primaryYes")}</Badge>
              ) : (
                "—"
              ),
          },
          {
            id: "notifications",
            header: t("parentStudents.receivesNotifications"),
            cell: (row) =>
              row.receivesNotifications
                ? t("parentStudents.notificationsYes")
                : t("parentStudents.notificationsNo"),
          },
          {
            id: "actions",
            header: "",
            className: "w-32 text-end",
            cell: (row) => (
              <div className="flex justify-end gap-1">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => {
                    setEditing(row);
                    setFormOpen(true);
                  }}
                >
                  {t("actions.edit")}
                </Button>
                {isSuperAdmin ? (
                  <Button
                    variant="ghost"
                    size="sm"
                    className="text-destructive"
                    onClick={() => setDeleting(row)}
                  >
                    {t("actions.delete")}
                  </Button>
                ) : null}
              </div>
            ),
          },
        ]}
      />
      <ParentStudentFormDialog
        open={formOpen}
        onOpenChange={(open) => {
          setFormOpen(open);
          if (!open) setEditing(null);
        }}
        link={editing}
      />
      {isSuperAdmin ? (
        <SuperAdminAuditReasonDialog
          open={Boolean(deleting)}
          onOpenChange={(open) => {
            if (!open) setDeleting(null);
          }}
          title={t("parentStudents.deleteTitle")}
          description={t("parentStudents.deletePermanentDescription")}
          confirmLabel={t("confirmDelete.confirm")}
          isPending={deleteMutation.isPending}
          onConfirm={(reason) => void handleDelete(reason)}
        />
      ) : null}
    </>
  );
}
