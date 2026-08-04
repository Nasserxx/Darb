import { useMemo } from "react";
import { useQueries } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";

import { DataTable } from "@/components/shared/data-table.tsx";
import { Badge } from "@/components/ui/badge.tsx";
import { PageHeader } from "@/components/shared/page-header.tsx";
import { circlesApi } from "@/features/circles/api/circles-api.ts";
import { circleKeys } from "@/features/circles/hooks/query-keys.ts";
import { enrollmentsApi } from "@/features/enrollments/api/enrollments-api.ts";
import { enrollmentKeys } from "@/features/enrollments/hooks/query-keys.ts";
import type { CircleResponse } from "@/features/circles/types/index.ts";
import type { PageResponse } from "@/lib/types/api.ts";
import { usePagination } from "@/lib/hooks/use-pagination.ts";

const ENROLLMENT_LIST_PARAMS = { page: 0, size: 500 } as const;

function formatSchedule(circle: CircleResponse): string {
  const days = circle.daysOfWeek ?? "—";
  const start = circle.startTime?.slice(0, 5) ?? "—";
  const end = circle.endTime?.slice(0, 5) ?? "—";
  return `${days} · ${start}–${end}`;
}

type ParentCirclesViewProps = {
  parentStudentIds: string[];
};

export function ParentCirclesView({ parentStudentIds }: ParentCirclesViewProps) {
  const { t } = useTranslation("app");
  const { page, size, setPage } = usePagination();

  const enrollmentQueries = useQueries({
    queries: parentStudentIds.map((studentId) => ({
      queryKey: enrollmentKeys.student(studentId, ENROLLMENT_LIST_PARAMS),
      queryFn: () =>
        enrollmentsApi.listByStudent(studentId, ENROLLMENT_LIST_PARAMS),
      enabled: Boolean(studentId),
    })),
  });

  const activeCircleIds = useMemo(() => {
    const ids = new Set<string>();
    for (const query of enrollmentQueries) {
      for (const enrollment of query.data?.content ?? []) {
        if (enrollment.status === "ACTIVE") {
          ids.add(enrollment.circleId);
        }
      }
    }
    return [...ids];
  }, [enrollmentQueries]);

  const circleQueries = useQueries({
    queries: activeCircleIds.map((circleId) => ({
      queryKey: circleKeys.detail(circleId),
      queryFn: () => circlesApi.getById(circleId),
      enabled: Boolean(circleId),
    })),
  });

  const circles = useMemo(
    () =>
      circleQueries
        .map((query) => query.data)
        .filter((circle): circle is CircleResponse => circle != null),
    [circleQueries],
  );

  const paginatedData = useMemo((): PageResponse<CircleResponse> | undefined => {
    if (circles.length === 0) return undefined;
    const start = page * size;
    const content = circles.slice(start, start + size);
    const totalPages = Math.max(1, Math.ceil(circles.length / size));
    return {
      content,
      totalElements: circles.length,
      totalPages,
      pageNumber: page,
      pageSize: size,
      last: page >= totalPages - 1,
    };
  }, [circles, page, size]);

  const columns = [
    {
      id: "name",
      header: t("circles.name"),
      cell: (row: CircleResponse) => (
        <span className="font-medium">{row.name}</span>
      ),
    },
    {
      id: "level",
      header: t("circles.level"),
      cell: (row: CircleResponse) => t(`enums.circleLevel.${row.level}`),
    },
    {
      id: "type",
      header: t("circles.type"),
      cell: (row: CircleResponse) => t(`enums.circleType.${row.type}`),
    },
    {
      id: "schedule",
      header: t("circles.schedule"),
      cell: (row: CircleResponse) => (
        <span className="text-muted-foreground">{formatSchedule(row)}</span>
      ),
    },
    {
      id: "status",
      header: t("circles.status"),
      cell: (row: CircleResponse) => (
        <Badge variant={row.status === "ACTIVE" ? "default" : "outline"}>
          {t(`enums.circleStatus.${row.status}`)}
        </Badge>
      ),
    },
  ];

  const enrollmentsLoading =
    parentStudentIds.length > 0 &&
    enrollmentQueries.some((query) => query.isLoading);
  const circlesLoading =
    activeCircleIds.length > 0 &&
    circleQueries.some((query) => query.isLoading);
  const isLoading = enrollmentsLoading || circlesLoading;

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={t("circles.title")}
        description={t("circles.description")}
      />
      <DataTable
        columns={columns}
        data={paginatedData}
        isLoading={isLoading}
        onPageChange={setPage}
      />
    </div>
  );
}
