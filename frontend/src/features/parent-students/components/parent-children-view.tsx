import { Link, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/shared/empty-state.tsx";
import { useMyChildren } from "@/features/parent-students/hooks/use-parent-students.ts";
import { formatShortId } from "@/lib/format/ids.ts";
import { DEFAULT_LOCALE } from "@/i18n/index.ts";
import { BookOpen, CircleDot, ExternalLink, GraduationCap, Users } from "lucide-react";

export function ParentChildrenView() {
  const { t } = useTranslation("app");
  const { locale } = useParams<{ locale: string }>();
  const localePrefix = locale ?? DEFAULT_LOCALE;
  const { data: children, isLoading } = useMyChildren();

  if (isLoading) {
    return (
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {Array.from({ length: 3 }).map((_, i) => (
          <Card key={i}>
            <CardHeader>
              <Skeleton className="h-5 w-32" />
              <Skeleton className="h-4 w-24" />
            </CardHeader>
            <CardContent>
              <div className="flex flex-col gap-2">
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-4 w-3/4" />
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    );
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

  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {children.map((child) => (
        <Link
          key={child.id}
          to={`/${localePrefix}/students/${child.id}`}
          className="transition-colors hover:opacity-80"
        >
          <Card className="h-full">
            <CardHeader>
              <div className="flex items-start justify-between gap-2">
                <div className="flex flex-col gap-1">
                  <CardTitle className="text-lg">
                    {t("students.detail")} #{child.fullName ?? formatShortId(child.id)}
                  </CardTitle>
                  <CardDescription>
                    {t("students.mosqueId")}:{" "}
                    {child.mosqueName ?? formatShortId(child.mosqueId)}
                  </CardDescription>
                </div>
                <Badge variant="outline">
                  {child.status === "WITHDRAWN"
                    ? t("membership.formerMember")
                    : t(`enums.enrollmentStatus.${child.status}`)}
                </Badge>
              </div>
            </CardHeader>
            <CardContent>
              <div className="flex flex-wrap gap-4">
                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                  <BookOpen className="size-4" />
                  <span>
                    {t("students.memorizedJuz")}: {child.memorizedJuz ?? "—"}
                  </span>
                </div>
                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                  <GraduationCap className="size-4" />
                  <span>
                    {t("students.totalAbsences")}: {child.totalAbsences}
                  </span>
                </div>
              </div>
              <div className="mt-4 flex items-center gap-3">
                <Button
                  variant="outline"
                  size="sm"
                  asChild
                  onClick={(e) => e.stopPropagation()}
                >
                  <Link
                    to={`/${localePrefix}/memorization/student/${child.id}`}
                  >
                    <BookOpen className="mr-1 size-3.5" />
                    {t("memorization.title")}
                  </Link>
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  asChild
                  onClick={(e) => e.stopPropagation()}
                >
                  <Link to={`/${localePrefix}/goals/student/${child.id}`}>
                    <CircleDot className="mr-1 size-3.5" />
                    {t("goals.title")}
                  </Link>
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  asChild
                  onClick={(e) => e.stopPropagation()}
                >
                  <Link to={`/${localePrefix}/students/${child.id}`}>
                    <ExternalLink className="mr-1 size-3.5" />
                    {t("actions.view")}
                  </Link>
                </Button>
              </div>
            </CardContent>
          </Card>
        </Link>
      ))}
    </div>
  );
}

