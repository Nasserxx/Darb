import { useMemo, useState } from "react";
import { Navigate, useParams, useSearchParams } from "react-router-dom";

import { HalfPageViewer } from "@/features/memorization/components/half-page-viewer.tsx";
import { canAccessStudentMemorization } from "@/features/memorization/lib/access.ts";
import type { PageHalf } from "@/features/mushaf/types/index.ts";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import {
  useEnrollments,
  useStudentEnrollments,
} from "@/features/enrollments/hooks/use-enrollments.ts";
import { useWorkspace } from "@/features/workspace/context/workspace-provider.tsx";
import { DEFAULT_LOCALE } from "@/i18n/index.ts";
import { normalizeApiRole } from "@/lib/navigation/app-nav.ts";
import { canManageMemorization } from "@/lib/navigation/role-permissions.ts";

export function HalfPageViewerPage() {
  const {
    studentId,
    page: pageParam,
    half: halfParam,
    locale,
  } = useParams<{
    studentId: string;
    page: string;
    half: string;
    locale: string;
  }>();
  const [searchParams] = useSearchParams();
  const localePrefix = locale ?? DEFAULT_LOCALE;
  const page = Number(pageParam);
  const half = halfParam === "B" ? "B" : "A";
  const { user } = useAuth();
  const { profile } = useWorkspace();

  const role = user ? normalizeApiRole(user.role) : null;
  const readonly = !canManageMemorization(user?.role);
  // ponytail: STUDENT/PARENT can't list staff enrollments (403)
  const studentPath = role === "STUDENT" || role === "PARENT";

  const { data: studentEnrollmentsPage } = useStudentEnrollments(
    studentId,
    { page: 0, size: 500 },
    { enabled: studentPath && Boolean(studentId) },
  );
  const { data: staffEnrollmentsPage } = useEnrollments(
    { page: 0, size: 500 },
    { enabled: !studentPath },
  );
  const enrollmentsPage = studentPath
    ? studentEnrollmentsPage
    : staffEnrollmentsPage;

  const enrolledCircles = useMemo(
    () =>
      (enrollmentsPage?.content ?? [])
        .filter(
          (enrollment) =>
            enrollment.status === "ACTIVE" &&
            (studentPath || enrollment.studentId === studentId),
        )
        .map((enrollment) => enrollment.circleId),
    [enrollmentsPage?.content, studentId, studentPath],
  );

  const circleFromQuery = searchParams.get("circleId") ?? "";
  const [circleId] = useState(circleFromQuery || enrolledCircles[0] || "");
  const activeCircleId = circleId || enrolledCircles[0] || "";

  if (
    !studentId ||
    !role ||
    !canAccessStudentMemorization(role, studentId, profile) ||
    !Number.isFinite(page) ||
    page < 1 ||
    page > 604 ||
    !activeCircleId
  ) {
    return <Navigate to={`/${localePrefix}/forbidden`} replace />;
  }

  return (
    <HalfPageViewer
      studentId={studentId}
      localePrefix={localePrefix}
      circleId={activeCircleId}
      page={page}
      half={half as PageHalf}
      readonly={readonly}
    />
  );
}
