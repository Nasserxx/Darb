import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, Navigate, useParams, useSearchParams } from "react-router-dom";
import { ChevronLeftIcon } from "lucide-react";

import { PageHeader } from "@/components/shared/page-header.tsx";
import { Button } from "@/components/ui/button.tsx";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select.tsx";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import {
  useEnrollments,
  useStudentEnrollments,
} from "@/features/enrollments/hooks/use-enrollments.ts";
import { PageHalfGrid } from "@/features/memorization/components/page-half-grid.tsx";
import {
  useJuzMetadata,
  useMemorizationCoverage,
} from "@/features/memorization/hooks/use-mushaf-memorization.ts";
import { canAccessStudentMemorization } from "@/features/memorization/lib/access.ts";
import { getPagesInJuz } from "@/features/mushaf/index.ts";
import { useWorkspace } from "@/features/workspace/context/workspace-provider.tsx";
import { DEFAULT_LOCALE } from "@/i18n/index.ts";
import { formatShortId } from "@/lib/format/ids.ts";
import { normalizeApiRole } from "@/lib/navigation/app-nav.ts";

export function JuzMemorizationPage() {
  const { t } = useTranslation("app");
  const { studentId, juz: juzParam, locale } = useParams<{
    studentId: string;
    juz: string;
    locale: string;
  }>();
  const [searchParams] = useSearchParams();
  const localePrefix = locale ?? DEFAULT_LOCALE;
  const juz = Number(juzParam);
  const { user } = useAuth();
  const { profile } = useWorkspace();

  const role = user ? normalizeApiRole(user.role) : null;
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
        .map((enrollment) => ({
          circleId: enrollment.circleId,
          circleName: enrollment.circleName,
        })),
    [enrollmentsPage?.content, studentId, studentPath],
  );

  const circleFromQuery = searchParams.get("circleId") ?? "";
  const [circleId, setCircleId] = useState(
    circleFromQuery || enrolledCircles[0]?.circleId || "",
  );
  const activeCircleId = circleId || enrolledCircles[0]?.circleId || "";

  useJuzMetadata(Number.isFinite(juz) ? juz : undefined);
  const pages = useMemo(
    () => (Number.isFinite(juz) && juz >= 1 && juz <= 30 ? getPagesInJuz(juz) : []),
    [juz],
  );

  const { data: coverage, isLoading } = useMemorizationCoverage(
    studentId,
    activeCircleId,
    "half",
  );

  if (
    !studentId ||
    !role ||
    !canAccessStudentMemorization(role, studentId, profile) ||
    !Number.isFinite(juz) ||
    juz < 1 ||
    juz > 30
  ) {
    return <Navigate to={`/${localePrefix}/forbidden`} replace />;
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={t("memorization.juzTitle", {
          juz,
          name: t(`memorization.juzNames.${juz}`),
        })}
        description={t("memorization.juzDescription")}
        actions={
          <Button variant="outline" asChild>
            <Link to={`/${localePrefix}/memorization/student/${studentId}`}>
              <ChevronLeftIcon className="size-4" aria-hidden />
              {t("memorization.back")}
            </Link>
          </Button>
        }
      />

      {enrolledCircles.length > 1 ? (
        <div className="max-w-xs">
          <Select value={activeCircleId} onValueChange={setCircleId}>
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

      <PageHalfGrid
        studentId={studentId}
        localePrefix={localePrefix}
        circleId={activeCircleId}
        pages={pages}
        coverage={coverage?.entries}
        isLoading={isLoading}
      />
    </div>
  );
}
