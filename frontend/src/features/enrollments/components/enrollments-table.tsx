import { useTranslation } from "react-i18next";
import { toast } from "sonner";

import { DataTable } from "@/components/shared/data-table.tsx";
import { Button } from "@/components/ui/button";
import { EnrollmentStatusBadge } from "@/features/enrollments/components/enrollment-status-badge.tsx";
import {
  useEnrollments,
  useUpdateEnrollment,
} from "@/features/enrollments/hooks/use-enrollments.ts";
import type { EnrollmentResponse } from "@/features/enrollments/types/index.ts";
import { formatShortId } from "@/lib/format/ids.ts";
import { toMutationError } from "@/lib/errors/map-api-error.ts";
import { usePagination } from "@/lib/hooks/use-pagination.ts";

type EnrollmentsTableProps = {
  canManage: boolean;
};

export function EnrollmentsTable({ canManage }: EnrollmentsTableProps) {
  const { t } = useTranslation("app");
  const { params, setPage } = usePagination();
  const { data, isLoading } = useEnrollments(params);
  const updateMutation = useUpdateEnrollment();

  async function updateStatus(
    enrollment: EnrollmentResponse,
    status: "ACTIVE" | "REJECTED",
  ) {
    try {
      await updateMutation.mutateAsync({
        id: enrollment.id,
        body: { status },
      });
      toast.success(
        status === "ACTIVE"
          ? t("enrollments.approveSuccess")
          : t("enrollments.rejectSuccess"),
      );
    } catch (error) {
      toast.error(toMutationError(error, t).message);
    }
  }

  return (
    <DataTable
      data={data}
      isLoading={isLoading}
      onPageChange={setPage}
      columns={[
        {
          id: "student",
          header: t("enrollments.studentId"),
          cell: (row) => formatShortId(row.studentId),
        },
        {
          id: "circle",
          header: t("enrollments.circleId"),
          cell: (row) => formatShortId(row.circleId),
        },
        {
          id: "status",
          header: t("enrollments.status"),
          cell: (row) => <EnrollmentStatusBadge status={row.status} />,
        },
        {
          id: "enrolledDate",
          header: t("enrollments.enrolledDate"),
          cell: (row) => row.enrolledDate ?? "—",
        },
        {
          id: "notes",
          header: t("enrollments.notes"),
          cell: (row) => (
            <span className="line-clamp-1 max-w-48 text-muted-foreground">
              {row.notes ?? "—"}
            </span>
          ),
        },
        ...(canManage
          ? [
              {
                id: "actions",
                header: "",
                className: "w-40 text-end",
                cell: (row: EnrollmentResponse) =>
                  row.status === "PENDING" ? (
                    <div className="flex justify-end gap-1">
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={updateMutation.isPending}
                        onClick={() => void updateStatus(row, "ACTIVE")}
                      >
                        {t("actions.approve")}
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="text-destructive"
                        disabled={updateMutation.isPending}
                        onClick={() => void updateStatus(row, "REJECTED")}
                      >
                        {t("actions.reject")}
                      </Button>
                    </div>
                  ) : null,
              },
            ]
          : []),
      ]}
    />
  );
}
