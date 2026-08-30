import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";
import { toast } from "sonner";

import { ConfirmDeleteDialog } from "@/components/shared/confirm-delete-dialog.tsx";
import { DataTable } from "@/components/shared/data-table.tsx";
import { SuperAdminMosqueScopeBar } from "@/components/shared/super-admin-mosque-scope-bar.tsx";
import { Button } from "@/components/ui/button";
import { EnrollmentStatusBadge } from "@/features/enrollments/components/enrollment-status-badge.tsx";
import { StudentFormDialog } from "@/features/students/components/student-form-dialog.tsx";
import {
  useDeleteStudent,
  useStudents,
} from "@/features/students/hooks/use-students.ts";
import type { StudentResponse } from "@/features/students/types/index.ts";
import { DEFAULT_LOCALE } from "@/i18n/index.ts";
import { formatShortId } from "@/lib/format/ids.ts";
import { toMutationError } from "@/lib/errors/map-api-error.ts";
import { usePagination } from "@/lib/hooks/use-pagination.ts";

type StudentsTableProps = {
  canWrite: boolean;
};

export function StudentsTable({ canWrite }: StudentsTableProps) {
  const { t } = useTranslation("app");
  const { locale } = useParams<{ locale: string }>();
  const localePrefix = locale ?? DEFAULT_LOCALE;
  const { params, setPage, setSize, setFilter } = usePagination();
  const { data, isLoading } = useStudents(params);
  const deleteMutation = useDeleteStudent();
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<StudentResponse | null>(null);
  const [deleting, setDeleting] = useState<StudentResponse | null>(null);

  async function handleDelete() {
    if (!deleting) return;
    try {
      await deleteMutation.mutateAsync(deleting.id);
      toast.success(t("students.deleteSuccess"));
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
          label: t("superAdminScope.filterStudentName"),
          placeholder: t("superAdminScope.filterStudentNamePlaceholder"),
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
            id: "id",
            header: t("students.title"),
            cell: (row) => (
              <Link
                to={`/${localePrefix}/students/${row.id}`}
                className="font-medium text-primary hover:underline"
              >
                {row.fullName ?? formatShortId(row.id)}
              </Link>
            ),
          },
          {
            id: "memorizedJuz",
            header: t("students.memorizedJuz"),
            cell: (row) => row.memorizedJuz ?? "—",
          },
          {
            id: "status",
            header: t("students.status"),
            cell: (row) => <EnrollmentStatusBadge status={row.status} />,
          },
          {
            id: "absences",
            header: t("students.totalAbsences"),
            cell: (row) => row.totalAbsences,
          },
          ...(canWrite
            ? [
                {
                  id: "actions",
                  header: "",
                  className: "w-32 text-end",
                  cell: (row: StudentResponse) => (
                    <div className="flex justify-end gap-1">
                      <Button
                        variant="ghost"
                        size="sm"
                        // ponytail: Former = WITHDRAWN (no isActive on StudentResponse)
                        disabled={row.status === "WITHDRAWN"}
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
                        disabled={row.status === "WITHDRAWN"}
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
          <StudentFormDialog
            open={formOpen}
            onOpenChange={(open) => {
              setFormOpen(open);
              if (!open) setEditing(null);
            }}
            student={editing}
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