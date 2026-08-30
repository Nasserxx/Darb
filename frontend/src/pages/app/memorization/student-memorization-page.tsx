import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Navigate, useParams, useSearchParams } from "react-router-dom";

import { PageHeader } from "@/components/shared/page-header.tsx";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select.tsx";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs.tsx";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import {
  useEnrollments,
  useStudentEnrollments,
} from "@/features/enrollments/hooks/use-enrollments.ts";
import { AssignLessonDialog } from "@/features/memorization/components/assign-lesson-dialog.tsx";
import { JuzGrid } from "@/features/memorization/components/juz-grid.tsx";
import { LegacySessionsTab } from "@/features/memorization/components/legacy-sessions-tab.tsx";
import { LessonCard } from "@/features/memorization/components/lesson-card.tsx";
import {
  useLessonAssignment,
  useMemorizationCoverage,
} from "@/features/memorization/hooks/use-mushaf-memorization.ts";
import { useStudent } from "@/features/students/hooks/use-students.ts";
import { useWorkspace } from "@/features/workspace/context/workspace-provider.tsx";
import { DEFAULT_LOCALE } from "@/i18n/index.ts";
import { formatShortId } from "@/lib/format/ids.ts";
import { normalizeApiRole } from "@/lib/navigation/app-nav.ts";
import { canManageMemorization } from "@/lib/navigation/role-permissions.ts";
import { canAccessStudentMemorization } from "@/features/memorization/lib/access.ts";

export function StudentMemorizationPage() {
  const { t } = useTranslation("app");
  const { studentId, locale } = useParams<{ studentId: string; locale: string }>();
  const [searchParams] = useSearchParams();
  const localePrefix = locale ?? DEFAULT_LOCALE;
  const { user } = useAuth();
  const { profile } = useWorkspace();

  const role = user ? normalizeApiRole(user.role) : null;
  const canManage = canManageMemorization(user?.role);
  // ponytail: STUDENT/PARENT can't list staff enrollments (403)
  const studentPath = role === "STUDENT" || role === "PARENT";
  const { data: student } = useStudent(studentId ?? "", {
    enabled: Boolean(studentId),
  });
  // ponytail: Former = WITHDRAWN; no isActive on StudentResponse
  const studentActive = Boolean(student && student.status !== "WITHDRAWN");

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
        .map((enrollment) => ({
          circleId: enrollment.circleId,
          circleName: enrollment.circleName,
        })),
    [enrollmentsPage?.content, studentId, studentPath],
  );

  const [selectedCircleId, setSelectedCircleId] = useState<string | null>(
    () => searchParams.get("circleId"),
  );

  const activeCircleId =
    selectedCircleId ?? enrolledCircles[0]?.circleId ?? "";

  const { data: lesson } = useLessonAssignment(studentId, activeCircleId);
  const { data: coverage, isLoading: coverageLoading } = useMemorizationCoverage(
    studentId,
    activeCircleId,
    "juz",
  );

  if (!studentId || !role || !canAccessStudentMemorization(role, studentId, profile)) {
    return <Navigate to={`/${localePrefix}/forbidden`} replace />;
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={t("memorization.title")}
        description={t("memorization.hubDescription")}
        actions={
          canManage && studentActive && enrolledCircles.length > 0 ? (
            <AssignLessonDialog
              studentId={studentId}
              circles={enrolledCircles}
              defaultCircleId={activeCircleId}
            />
          ) : null
        }
      />

      {enrolledCircles.length > 1 ? (
        <div className="max-w-xs">
          <Select value={activeCircleId} onValueChange={setSelectedCircleId}>
            <SelectTrigger>
              <SelectValue placeholder={t("memorization.circle")} />
            </SelectTrigger>
            <SelectContent>
              {enrolledCircles.map((circle) => (
                <SelectItem key={circle.circleId} value={circle.circleId}>
                  {circle.circleName ?? formatShortId(circle.circleId)}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      ) : null}

      <Tabs defaultValue="mushaf">
        <TabsList>
          <TabsTrigger value="mushaf">{t("memorization.tabMushafProgress")}</TabsTrigger>
          <TabsTrigger value="legacy">{t("memorization.tabLegacy")}</TabsTrigger>
        </TabsList>

        <TabsContent
          value="mushaf"
          keepMounted={false}
          className="flex flex-col gap-6 pt-4 data-hidden:hidden"
        >
          {lesson ? (
            <LessonCard
              lesson={lesson}
              studentId={studentId}
              localePrefix={localePrefix}
            />
          ) : null}

          <section className="flex flex-col gap-3">
            <h2 className="text-lg font-semibold">{t("memorization.juzGridTitle")}</h2>
            <JuzGrid
              studentId={studentId}
              localePrefix={localePrefix}
              circleId={activeCircleId}
              coverage={coverage?.entries}
              isLoading={coverageLoading && !!activeCircleId}
            />
          </section>
        </TabsContent>

        <TabsContent
          value="legacy"
          keepMounted={false}
          className="pt-4 data-hidden:hidden"
        >
          <LegacySessionsTab
            studentId={studentId}
            enrolledCircles={enrolledCircles}
          />
        </TabsContent>
      </Tabs>
    </div>
  );
}
