import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { BookOpenIcon } from "lucide-react";

import { Button } from "@/components/ui/button.tsx";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card.tsx";
import { parseHalfPageId } from "@/features/mushaf/index.ts";
import type { LessonAssignmentResponse } from "@/features/memorization/types/index.ts";
import { DEFAULT_LOCALE } from "@/i18n/index.ts";

interface LessonCardProps {
  lesson: LessonAssignmentResponse;
  studentId: string;
  localePrefix: string;
}

export function LessonCard({ lesson, studentId, localePrefix }: LessonCardProps) {
  const { t } = useTranslation("app");
  const primaryHalfId = lesson.halfPageIds[0];
  if (!primaryHalfId) return null;

  let parsed: { page: number; half: "A" | "B" };
  try {
    parsed = parseHalfPageId(primaryHalfId);
  } catch {
    return null;
  }
  const { page, half } = parsed;

  const halfLabel =
    half === "A" ? t("memorization.halfA") : t("memorization.halfB");

  return (
    <Card className="border-primary/30 bg-primary/5">
      <CardHeader className="pb-2">
        <CardTitle className="flex items-center gap-2 text-lg">
          <BookOpenIcon className="size-5" aria-hidden />
          {t("memorization.currentLesson")}
        </CardTitle>
        <CardDescription>{t("memorization.lessonDescription")}</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-col gap-1 text-sm">
          <span>
            {t("memorization.pageLabel", { page })} · {halfLabel}
          </span>
          {lesson.note ? (
            <p className="text-muted-foreground">{lesson.note}</p>
          ) : null}
          {lesson.halfPageIds.length > 1 ? (
            <span className="text-muted-foreground">
              {t("memorization.halfPagesCount", {
                count: lesson.halfPageIds.length,
              })}
            </span>
          ) : null}
        </div>
        <Button asChild>
          <Link
            to={`/${localePrefix ?? DEFAULT_LOCALE}/memorization/student/${studentId}/page/${page}/half/${half}?circleId=${lesson.circleId}`}
          >
            {t("memorization.openLesson")}
          </Link>
        </Button>
      </CardContent>
    </Card>
  );
}
