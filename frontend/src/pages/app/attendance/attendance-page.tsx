import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";

import { DataTable } from "@/components/shared/data-table.tsx";
import { PageHeader } from "@/components/shared/page-header.tsx";
import { Badge } from "@/components/ui/badge.tsx";
import { Button } from "@/components/ui/button.tsx";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import { useAttendanceByStudent } from "@/features/attendance/hooks/use-attendance.ts";
import { useCircles } from "@/features/circles/hooks/use-circles.ts";
import type { AttendanceResponse } from "@/features/attendance/types/index.ts";
import type { CircleResponse } from "@/features/circles/types/index.ts";
import { useWorkspace } from "@/features/workspace/context/workspace-provider.tsx";
import { DEFAULT_LOCALE } from "@/i18n/index.ts";
import { usePagination } from "@/lib/hooks/use-pagination.ts";
import { canMarkAttendance } from "@/lib/navigation/role-permissions.ts";
import { normalizeApiRole } from "@/lib/navigation/app-nav.ts";

import { ParentAttendanceView } from "./parent-attendance-view.tsx";

function StudentAttendanceView() {
  const { t } = useTranslation("app");
  const { profile } = useWorkspace();
  const { params, setPage } = usePagination();
  const studentId = profile?.studentId;

  const { data: attendanceData, isLoading: attendanceLoading } =
    useAttendanceByStudent(studentId, params);
  const { data: circlesData } = useCircles({ page: 0, size: 500 });

  const circleNames = useMemo(() => {
    const map = new Map<string, string>();
    for (const circle of circlesData?.content ?? []) {
      map.set(circle.id, circle.name);
    }
    return map;
  }, [circlesData?.content]);

  const columns = [
    {
      id: "sessionDate",
      header: t("attendance.sessionDate"),
      cell: (row: AttendanceResponse) => (
        <span className="font-medium">{row.sessionDate}</span>
      ),
    },
    {
      id: "circle",
      header: t("circles.title"),
      cell: (row: AttendanceResponse) => (
        <span>{circleNames.get(row.circleId) ?? row.circleId.slice(0, 8)}</span>
      ),
    },
    {
      id: "status",
      header: t("attendance.status"),
      cell: (row: AttendanceResponse) => (
        <Badge variant={row.status === "PRESENT" ? "default" : "outline"}>
          {t(`attendance.statuses.${row.status}`)}
        </Badge>
      ),
    },
    {
      id: "scheduledStart",
      header: t("circles.startTime"),
      cell: (row: AttendanceResponse) => (
        <span className="text-muted-foreground">
          {row.scheduledStart ? row.scheduledStart.slice(0, 5) : "—"}
        </span>
      ),
    },
    {
      id: "checkIn",
      header: t("attendance.checkIn"),
      cell: (row: AttendanceResponse) => (
        <span className="text-muted-foreground">
          {row.actualCheckIn ? row.actualCheckIn.slice(0, 5) : "—"}
        </span>
      ),
    },
  ];

  return (
    <DataTable
      columns={columns}
      data={attendanceData}
      isLoading={attendanceLoading}
      emptyMessage={t("attendance.studentEmpty")}
      onPageChange={setPage}
    />
  );
}

export function AttendancePage() {
  const { t } = useTranslation("app");
  const { locale } = useParams<{ locale: string }>();
  const localePrefix = locale ?? DEFAULT_LOCALE;
  const { user } = useAuth();
  const { mosqueId, profile } = useWorkspace();
  const { params, setPage } = usePagination();
  const role = user ? normalizeApiRole(user.role) : null;

  const isStudent = role === "STUDENT";
  const isParent = role === "PARENT";
  const canMark = canMarkAttendance(user?.role);

  // PARENT/STUDENT use student-scoped views — do not hit mosque pageForCaller.
  const { data, isLoading } = useCircles(params, {
    enabled: !isStudent && !isParent,
  });

  const circles = useMemo(() => {
    if (isStudent || isParent) return [];
    const all = data?.content ?? [];
    if (role === "SUPER_ADMIN") return all;
    if (role === "MOSQUE_ADMIN" && mosqueId) {
      return all.filter((circle) => circle.mosqueId === mosqueId);
    }
    if (role === "TEACHER" && profile?.teacherId) {
      return all.filter((circle) => circle.teacherId === profile.teacherId);
    }
    return all;
  }, [data?.content, isParent, isStudent, mosqueId, profile, role]);

  const filteredData = data
    ? { ...data, content: circles, totalElements: circles.length }
    : undefined;

  const columns = useMemo(() => {
    const base = [
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
    ];

    if (!canMark) {
      return base;
    }

    return [
      ...base,
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
  }, [canMark, localePrefix, t]);

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={t("attendance.title")}
        description={
          isStudent
            ? t("attendance.studentDescription")
            : isParent
              ? t("attendance.parentDescription")
              : t("attendance.description")
        }
      />
      {isStudent ? (
        <StudentAttendanceView />
      ) : isParent ? (
        <ParentAttendanceView />
      ) : (
        <DataTable
          columns={columns}
          data={filteredData}
          isLoading={isLoading}
          onPageChange={setPage}
        />
      )}
    </div>
  );
}
