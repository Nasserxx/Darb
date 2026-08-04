import { useMutation, useQueries, useQuery } from "@tanstack/react-query";
import { CopyIcon } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";

import { DataTable } from "@/components/shared/data-table.tsx";
import { PageHeader } from "@/components/shared/page-header.tsx";
import { StatCard } from "@/components/shared/stat-card.tsx";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { stuckWorkApi } from "@/features/admin/api/stuck-work-api.ts";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import { mosqueAdminsApi } from "@/features/mosque-admins/api/mosque-admins-api.ts";
import { mosquesApi } from "@/features/mosques/api/mosques-api.ts";
import { enrollmentsApi } from "@/features/enrollments/api/enrollments-api.ts";
import { enrollmentKeys } from "@/features/enrollments/hooks/query-keys.ts";
import { useWorkspace } from "@/features/workspace/context/workspace-provider.tsx";
import { circleKeys } from "@/features/circles/hooks/query-keys.ts";
import { circlesApi } from "@/features/circles/api/circles-api.ts";
import { paymentKeys } from "@/features/payments/hooks/query-keys.ts";
import { getMosquePayments, getPayments } from "@/features/payments/api/payments-api.ts";
import { studentKeys } from "@/features/students/hooks/query-keys.ts";
import { studentsApi } from "@/features/students/api/students-api.ts";
import { normalizeApiRole } from "@/lib/navigation/app-nav.ts";
import { CircleDot, CreditCard, GraduationCap, Users } from "lucide-react";
import { formatShortId } from "@/lib/format/ids.ts";

export function AdminDashboardPage() {
  const { t } = useTranslation("app");
  const { user } = useAuth();
  const { locale } = useParams<{ locale: string }>();
  const localePrefix = locale ?? "en";
  const { mosqueId } = useWorkspace();
  const role = user ? normalizeApiRole(user.role) : null;
  const isSuperAdmin = role === "SUPER_ADMIN";

  const stuckWorkQuery = useQuery({
    queryKey: ["admin", "stuck-work"],
    queryFn: () => stuckWorkApi.list(),
    enabled: isSuperAdmin,
  });

  const joinRequestsQuery = useQuery({
    queryKey: ["mosque-admins", "join-requests"],
    queryFn: () => mosqueAdminsApi.listJoinRequests(),
    enabled: !isSuperAdmin,
  });

  const inviteCodesQuery = useQuery({
    queryKey: ["mosques", "invite-codes"],
    queryFn: () => mosquesApi.getInviteCodes(),
  });

  const approveJoinRequest = useMutation({
    mutationFn: (id: string) => mosqueAdminsApi.approveJoinRequest(id),
    onSuccess: () => {
      toast.success(t("admin.joinRequests.approveSuccess"));
      void joinRequestsQuery.refetch();
    },
  });

  const rejectJoinRequest = useMutation({
    mutationFn: (id: string) => mosqueAdminsApi.rejectJoinRequest(id),
    onSuccess: () => {
      toast.success(t("admin.joinRequests.rejectSuccess"));
      void joinRequestsQuery.refetch();
    },
  });

  async function copyCode(code: string | undefined) {
    if (!code) return;
    try {
      await navigator.clipboard.writeText(code);
      toast.success(t("admin.inviteCodes.copied"));
    } catch {
      toast.error(t("onboarding.error"));
    }
  }

  const [studentsQuery, circlesQuery, enrollmentsQuery, paymentsQuery] =
    useQueries({
      queries: [
        {
          queryKey: studentKeys.list({ page: 0, size: 1 }),
          queryFn: () => studentsApi.list({ page: 0, size: 1 }),
        },
        {
          queryKey: circleKeys.list({ page: 0, size: 1 }),
          queryFn: () => circlesApi.list({ page: 0, size: 1 }),
        },
        {
          queryKey: enrollmentKeys.list({ page: 0, size: 5 }),
          queryFn: () => enrollmentsApi.list({ page: 0, size: 5 }),
        },
        {
          queryKey: paymentKeys.list({ page: 0, size: 100 }),
          queryFn: () =>
            mosqueId
              ? getMosquePayments(mosqueId, { page: 0, size: 100 })
              : getPayments({ page: 0, size: 100 }),
          enabled: true,
        },
      ],
    });

  const pendingEnrollments =
    enrollmentsQuery.data?.content.filter((e) => e.status === "PENDING") ?? [];
  const overdueCount =
    paymentsQuery.data?.content.filter((p) => p.status === "OVERDUE").length ??
    0;

  return (
    <div className="auth-stagger flex flex-col gap-8">
      <PageHeader
        title={t(isSuperAdmin ? "admin.fleetTitle" : "admin.title")}
        description={t(isSuperAdmin ? "admin.fleetDescription" : "admin.description")}
        actions={
          <div className="flex flex-wrap items-center gap-2">
            <Button size="sm" asChild>
              <Link to={`/${localePrefix}/students`}>
                {t("admin.quickActions.addStudent")}
              </Link>
            </Button>
            <Button size="sm" variant="outline" asChild>
              <Link to={`/${localePrefix}/circles`}>
                {t("admin.quickActions.createCircle")}
              </Link>
            </Button>
            <Button size="sm" variant="outline" asChild>
              <Link to={`/${localePrefix}/payments`}>
                {t("admin.quickActions.recordPayment")}
              </Link>
            </Button>
          </div>
        }
      />

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          title={t("admin.stats.students")}
          value={studentsQuery.data?.totalElements ?? "—"}
          icon={Users}
        />
        <StatCard
          title={t("admin.stats.circles")}
          value={circlesQuery.data?.totalElements ?? "—"}
          icon={CircleDot}
        />
        <StatCard
          title={t("admin.stats.pendingEnrollments")}
          value={pendingEnrollments.length}
          icon={GraduationCap}
        />
        <StatCard
          title={t("admin.stats.overduePayments")}
          value={overdueCount}
          icon={CreditCard}
          accent="gold"
        />
      </div>

      {isSuperAdmin ? (
        <Card>
          <CardHeader>
            <CardTitle>{t("admin.sections.fleetStuckWork")}</CardTitle>
          </CardHeader>
          <CardContent>
            <DataTable
              isLoading={stuckWorkQuery.isLoading}
              data={
                stuckWorkQuery.data
                  ? {
                      content: stuckWorkQuery.data,
                      pageNumber: 0,
                      pageSize: stuckWorkQuery.data.length,
                      totalElements: stuckWorkQuery.data.length,
                      totalPages: 1,
                      last: true,
                    }
                  : undefined
              }
              emptyMessage={t("admin.stuckWork.empty")}
              columns={[
                {
                  id: "kind",
                  header: t("admin.stuckWork.kind"),
                  cell: (row) => (
                    <Badge variant="secondary">{row.kind.replace(/_/g, " ")}</Badge>
                  ),
                },
                {
                  id: "mosque",
                  header: t("mosques.name"),
                  cell: (row) => row.mosqueName,
                },
                {
                  id: "summary",
                  header: t("admin.stuckWork.summary"),
                  cell: (row) => row.summary,
                },
                {
                  id: "density",
                  header: t("admin.stuckWork.density"),
                  cell: (row) => row.densityScore,
                },
                {
                  id: "actions",
                  header: t("actions.view"),
                  cell: () => (
                    <Button size="sm" variant="outline" asChild>
                      <Link to={`/${localePrefix}/mosques`}>
                        {t("admin.stuckWork.openMosqueDesk")}
                      </Link>
                    </Button>
                  ),
                },
              ]}
            />
          </CardContent>
        </Card>
      ) : null}

      <Card>
        <CardHeader>
          <CardTitle>{t("admin.sections.pendingEnrollments")}</CardTitle>
        </CardHeader>
        <CardContent>
          <DataTable
            isLoading={enrollmentsQuery.isLoading}
            data={
              enrollmentsQuery.data
                ? {
                    ...enrollmentsQuery.data,
                    content: pendingEnrollments,
                  }
                : undefined
            }
            emptyMessage={t("table.empty")}
            columns={[
              {
                id: "student",
                header: t("students.title"),
                cell: (row) => (
                  <span className="font-mono text-xs">{row.studentName ?? formatShortId(row.studentId)}</span>
                ),
              },
              {
                id: "circle",
                header: t("circles.title"),
                cell: (row) => (
                  <span className="font-mono text-xs">{row.circleName ?? formatShortId(row.circleId)}</span>
                ),
              },
              {
                id: "status",
                header: t("enrollments.status"),
                cell: (row) => <Badge variant="secondary">{row.status}</Badge>,
              },
            ]}
          />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>{t("admin.sections.inviteCodes")}</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          {inviteCodesQuery.isLoading ? (
            <p className="text-sm text-muted-foreground">{t("common:loading")}</p>
          ) : (
            ["adminInviteCode", "teacherInviteCode", "studentInviteCode"].map((key) => {
              const labelKey =
                key === "adminInviteCode"
                  ? "admin"
                  : key === "teacherInviteCode"
                    ? "teacher"
                    : "student";
              const code = inviteCodesQuery.data?.[key as keyof typeof inviteCodesQuery.data];
              return (
                <div
                  key={key}
                  className="flex flex-col gap-2 rounded-lg border border-border/70 p-4 sm:flex-row sm:items-center sm:justify-between"
                >
                  <div className="flex flex-col gap-1">
                    <p className="text-sm font-medium">
                      {t(`admin.inviteCodes.${labelKey}`)}
                    </p>
                    <p className="font-mono text-sm">{code ?? "—"}</p>
                  </div>
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={!code}
                    onClick={() => void copyCode(code as string | undefined)}
                  >
                    <CopyIcon data-icon="inline-start" />
                    {t("admin.inviteCodes.copy")}
                  </Button>
                </div>
              );
            })
          )}
        </CardContent>
      </Card>

      {!isSuperAdmin ? (
      <Card>
        <CardHeader>
          <CardTitle>{t("admin.sections.pendingJoinRequests")}</CardTitle>
        </CardHeader>
        <CardContent>
          <DataTable
            isLoading={joinRequestsQuery.isLoading}
            data={
              joinRequestsQuery.data
                ? {
                    content: joinRequestsQuery.data,
                    pageNumber: 0,
                    pageSize: joinRequestsQuery.data.length,
                    totalElements: joinRequestsQuery.data.length,
                    totalPages: 1,
                    last: true,
                  }
                : undefined
            }
            emptyMessage={t("admin.joinRequests.empty")}
            columns={[
              {
                id: "name",
                header: t("mosques.name"),
                cell: (row) => row.userFullName,
              },
              {
                id: "role",
                header: t("auth:register.role"),
                cell: (row) => <Badge variant="secondary">{row.requestedRole}</Badge>,
              },
              {
                id: "actions",
                header: t("actions.view"),
                cell: (row) => (
                  <div className="flex flex-wrap gap-2">
                    <Button
                      size="sm"
                      disabled={approveJoinRequest.isPending}
                      onClick={() => approveJoinRequest.mutate(row.id)}
                    >
                      {t("actions.approve")}
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      disabled={rejectJoinRequest.isPending}
                      onClick={() => rejectJoinRequest.mutate(row.id)}
                    >
                      {t("actions.reject")}
                    </Button>
                  </div>
                ),
              },
            ]}
          />
        </CardContent>
      </Card>
      ) : null}
    </div>
  );
}

