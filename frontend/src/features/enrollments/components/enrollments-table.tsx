import { useState } from "react";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";

import { DataTable } from "@/components/shared/data-table.tsx";
import { SuperAdminAuditReasonDialog } from "@/components/shared/super-admin-audit-reason-dialog.tsx";
import { SuperAdminMosqueScopeBar } from "@/components/shared/super-admin-mosque-scope-bar.tsx";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import { EnrollmentStatusBadge } from "@/features/enrollments/components/enrollment-status-badge.tsx";
import {
  useEnrollments,
  useUpdateEnrollment,
} from "@/features/enrollments/hooks/use-enrollments.ts";
import type { EnrollmentResponse } from "@/features/enrollments/types/index.ts";
import { formatShortId } from "@/lib/format/ids.ts";
import { toMutationError } from "@/lib/errors/map-api-error.ts";
import { usePagination } from "@/lib/hooks/use-pagination.ts";
import { hasRole } from "@/lib/navigation/role-permissions.ts";

type EnrollmentsTableProps = {
  canManage: boolean;
};

type PendingStatus = {
  enrollment: EnrollmentResponse;
  status: "ACTIVE" | "REJECTED";
};

export function EnrollmentsTable({ canManage }: EnrollmentsTableProps) {
  const { t } = useTranslation("app");
  const { user } = useAuth();
  const isSuperAdmin = hasRole(user?.role, ["SUPER_ADMIN"]);
  const { params, setPage, setSize, setFilter } = usePagination();
  const { data, isLoading } = useEnrollments(params);
  const updateMutation = useUpdateEnrollment();
  const [pending, setPending] = useState<PendingStatus | null>(null);

  async function updateStatus(
    enrollment: EnrollmentResponse,
    status: "ACTIVE" | "REJECTED",
    auditReason?: string,
  ) {
    try {
      await updateMutation.mutateAsync({
        id: enrollment.id,
        body: { status },
        auditReason,
      });
      toast.success(
        status === "ACTIVE"
          ? t("enrollments.approveSuccess")
          : t("enrollments.rejectSuccess"),
      );
      setPending(null);
    } catch (error) {
      toast.error(toMutationError(error, t).message);
    }
  }

  function requestStatusChange(
    enrollment: EnrollmentResponse,
    status: "ACTIVE" | "REJECTED",
  ) {
    if (isSuperAdmin) {
      setPending({ enrollment, status });
      return;
    }
    void updateStatus(enrollment, status);
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
          id: "student",
          header: t("enrollments.studentId"),
          cell: (row) => row.studentName ?? formatShortId(row.studentId),
        },
        {
          id: "circle",
          header: t("enrollments.circleId"),
          cell: (row) => row.circleName ?? formatShortId(row.circleId),
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
                        onClick={() => requestStatusChange(row, "ACTIVE")}
                      >
                        {t("actions.approve")}
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="text-destructive"
                        disabled={updateMutation.isPending}
                        onClick={() => requestStatusChange(row, "REJECTED")}
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
      <SuperAdminAuditReasonDialog
        open={Boolean(pending)}
        onOpenChange={(open) => {
          if (!open) setPending(null);
        }}
        title={t("superAdmin.auditReason.title")}
        confirmLabel={t("superAdmin.auditReason.confirm")}
        isPending={updateMutation.isPending}
        onConfirm={(reason) => {
          if (!pending) return;
          return updateStatus(pending.enrollment, pending.status, reason);
        }}
      />
    </>
  );
}
