import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";

import { DataTable } from "@/components/shared/data-table.tsx";
import { PageHeader } from "@/components/shared/page-header.tsx";
import { Badge } from "@/components/ui/badge.tsx";
import { Button } from "@/components/ui/button.tsx";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import { useCircles } from "@/features/circles/hooks/use-circles.ts";
import type { CircleResponse } from "@/features/circles/types/index.ts";
import { useWorkspace } from "@/features/workspace/context/workspace-provider.tsx";
import { DEFAULT_LOCALE } from "@/i18n/index.ts";
import { usePagination } from "@/lib/hooks/use-pagination.ts";
import { normalizeApiRole } from "@/lib/navigation/app-nav.ts";

export function AttendancePage() {
  const { t } = useTranslation("app");
  const { locale } = useParams<{ locale: string }>();
  const localePrefix = locale ?? DEFAULT_LOCALE;
  const { user } = useAuth();
  const { mosqueId, profile } = useWorkspace();
  const { params, setPage } = usePagination();
  const { data, isLoading } = useCircles(params);
  const role = user ? normalizeApiRole(user.role) : null;

  const circles = useMemo(() => {
    const all = data?.content ?? [];
    if (role === "SUPER_ADMIN") return all;
    if (role === "MOSQUE_ADMIN" && mosqueId) {
      return all.filter((circle) => circle.mosqueId === mosqueId);
    }
    if (role === "TEACHER" && profile?.teacherId) {
      return all.filter((circle) => circle.teacherId === profile.teacherId);
    }
    return all;
  }, [data?.content, mosqueId, profile?.teacherId, role]);

  const filteredData = data
    ? { ...data, content: circles, totalElements: circles.length }
    : undefined;

  const columns = [
    {
      id: "name",
      header: t("circles.title"),
      cell: (row: CircleResponse) => (
        <span className="font-medium">{row.name}</span>
      ),
    },
    {
      id: "level",
      header: t("circles.level"),
      cell: (row: CircleResponse) => (
        <Badge variant="secondary">{row.level.replace(/_/g, " ")}</Badge>
      ),
    },
    {
      id: "status",
      header: t("students.status"),
      cell: (row: CircleResponse) => (
        <Badge variant="outline">{row.status.replace(/_/g, " ")}</Badge>
      ),
    },
    {
      id: "actions",
      header: "",
      className: "text-right",
      cell: (row: CircleResponse) => (
        <Button variant="outline" size="sm" asChild>
          <Link to={`/${localePrefix}/attendance/circle/${row.id}`}>
            {t("attendance.markAttendance")}
          </Link>
        </Button>
      ),
    },
  ];

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={t("attendance.title")}
        description={t("attendance.description")}
      />
      <DataTable
        columns={columns}
        data={filteredData}
        isLoading={isLoading}
        onPageChange={setPage}
      />
    </div>
  );
}
