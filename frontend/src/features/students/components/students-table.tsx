import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";
import { toast } from "sonner";

import { ConfirmDeleteDialog } from "@/components/shared/confirm-delete-dialog.tsx";
import { DataTable } from "@/components/shared/data-table.tsx";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
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
  const { params, setPage } = usePagination();
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
      <DataTable
        data={data}
        isLoading={isLoading}
        onPageChange={setPage}
        columns={[
          {
            id: "id",
            header: "ID",
            cell: (row) => (
              <Link
                to={`/${localePrefix}/students/${row.id}`}
                className="font-medium text-primary hover:underline"
              >
                {formatShortId(row.id)}
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
            cell: (row) => (
              <Badge variant="outline">
                {t(`enums.enrollmentStatus.${row.status}`)}
              </Badge>
            ),
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