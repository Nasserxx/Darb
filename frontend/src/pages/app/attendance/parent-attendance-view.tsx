import { useMemo } from "react";
import { useQueries } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import { Users } from "lucide-react";

import { DataTable } from "@/components/shared/data-table.tsx";
import { EmptyState } from "@/components/shared/empty-state.tsx";
import { Badge } from "@/components/ui/badge.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Skeleton } from "@/components/ui/skeleton.tsx";
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from "@/components/ui/tabs.tsx";
import { attendanceApi } from "@/features/attendance/api/attendance-api.ts";
import { attendanceKeys } from "@/features/attendance/hooks/attendance-keys.ts";
import { useSubmitExcuse } from "@/features/attendance/hooks/use-attendance.ts";
import type { AttendanceResponse } from "@/features/attendance/types/index.ts";
import { useMyChildren } from "@/features/parent-students/hooks/use-parent-students.ts";
import type { StudentResponse } from "@/features/students/types/index.ts";
import { formatShortId } from "@/lib/format/ids.ts";
import type { PageResponse } from "@/lib/types/api.ts";

const ATTENDANCE_PAGE = { page: 0, size: 500 } as const;

const EXCUSE_REASONS = ["SICK", "FAMILY", "TRAVEL", "PERSONAL", "OTHER"] as const;

function childLabel(child: StudentResponse): string {
  return child.fullName ?? formatShortId(child.id);
}

type ChildAttendanceTableProps = {
  isLoading: boolean;
  attendanceData: PageResponse<AttendanceResponse> | undefined;
  onSubmitExcuse: (attendanceId: string, absenceReason: string) => void;
  isExcusing: boolean;
};

function ChildAttendanceTable({
  isLoading,
  attendanceData,
  onSubmitExcuse,
  isExcusing,
}: ChildAttendanceTableProps) {
  const { t } = useTranslation("app");

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
        <span>{row.circleName ?? formatShortId(row.circleId)}</span>
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
    {
      id: "excuse",
      header: "",
      className: "text-right",
      cell: (row: AttendanceResponse) => {
        if (row.status === "EXCUSED") {
          return (
            <span className="text-muted-foreground text-sm">
              {row.absenceReason
                ? t(`attendance.absenceReasons.${row.absenceReason}`)
                : t("attendance.statuses.EXCUSED")}
            </span>
          );
        }
        return (
          <Button
            size="sm"
            variant="outline"
            disabled={isExcusing}
            onClick={() => {
              const reason = window.prompt(
                t("attendance.excusePrompt"),
                EXCUSE_REASONS[0],
              );
              if (!reason) return;
              const normalized = reason.trim().toUpperCase();
              if (
                !EXCUSE_REASONS.includes(
                  normalized as (typeof EXCUSE_REASONS)[number],
                )
              ) {
                toast.error(t("attendance.excuseInvalidReason"));
                return;
              }
              onSubmitExcuse(row.id, normalized);
            }}
          >
            {t("attendance.submitExcuse")}
          </Button>
        );
      },
    },
  ];

  return (
    <DataTable
      columns={columns}
      data={attendanceData}
      isLoading={isLoading}
      emptyMessage={t("attendance.studentEmpty")}
    />
  );
}

export function ParentAttendanceView() {
  const { t } = useTranslation("app");
  const { data: children, isLoading: childrenLoading } = useMyChildren();
  const submitExcuse = useSubmitExcuse({
    onSuccess: () => toast.success(t("attendance.excuseSuccess")),
    onError: () => toast.error(t("attendance.excuseError")),
  });

  const studentIds = useMemo(
    () => (children ?? []).map((child) => child.id),
    [children],
  );

  const attendanceQueries = useQueries({
    queries: studentIds.map((studentId) => ({
      queryKey: attendanceKeys.student(studentId, ATTENDANCE_PAGE),
      queryFn: () => attendanceApi.listByStudent(studentId, ATTENDANCE_PAGE),
      enabled: Boolean(studentId),
    })),
  });

  const attendanceLoading = attendanceQueries.some((query) => query.isLoading);
  const isLoading = childrenLoading || attendanceLoading;

  function handleSubmitExcuse(attendanceId: string, absenceReason: string) {
    submitExcuse.mutate({ id: attendanceId, body: { absenceReason } });
  }

  if (childrenLoading) {
    return <Skeleton className="h-48 w-full" />;
  }

  if (!children || children.length === 0) {
    return (
      <EmptyState
        icon={<Users className="text-muted-foreground" />}
        title={t("students.empty")}
        description={t("students.empty")}
      />
    );
  }

  if (children.length === 1) {
    return (
      <ChildAttendanceTable
        isLoading={isLoading}
        attendanceData={attendanceQueries[0]?.data}
        onSubmitExcuse={handleSubmitExcuse}
        isExcusing={submitExcuse.isPending}
      />
    );
  }

  const defaultTab = studentIds[0] ?? "";

  return (
    <Tabs defaultValue={defaultTab}>
      <TabsList>
        {children.map((child) => (
          <TabsTrigger key={child.id} value={child.id}>
            {childLabel(child)}
          </TabsTrigger>
        ))}
      </TabsList>
      {children.map((child, index) => (
        <TabsContent key={child.id} value={child.id}>
          <ChildAttendanceTable
            isLoading={isLoading}
            attendanceData={attendanceQueries[index]?.data}
            onSubmitExcuse={handleSubmitExcuse}
            isExcusing={submitExcuse.isPending}
          />
        </TabsContent>
      ))}
    </Tabs>
  );
}
