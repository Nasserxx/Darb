import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";

import { Progress } from "@/components/ui/progress.tsx";
import { Skeleton } from "@/components/ui/skeleton.tsx";
import type { CoverageEntry } from "@/features/memorization/types/index.ts";
import { cn } from "@/lib/utils";

interface JuzGridProps {
  studentId: string;
  localePrefix: string;
  circleId?: string;
  coverage?: CoverageEntry[];
  isLoading?: boolean;
}

export function JuzGrid({
  studentId,
  localePrefix,
  circleId,
  coverage = [],
  isLoading,
}: JuzGridProps) {
  const { t } = useTranslation("app");

  const coverageByJuz = new Map<number, CoverageEntry>();
  for (const entry of coverage) {
    if (entry.juz !== undefined) {
      coverageByJuz.set(entry.juz, entry);
    }
  }

  if (isLoading) {
    return (
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-5 lg:grid-cols-6">
        {Array.from({ length: 30 }, (_, i) => (
          <Skeleton key={i} className="h-24 rounded-lg" />
        ))}
      </div>
    );
  }

  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-5 lg:grid-cols-6">
      {Array.from({ length: 30 }, (_, index) => {
        const juz = index + 1;
        const entry = coverageByJuz.get(juz);
        const progress = entry?.progressPercent ?? 0;

        const query = circleId ? `?circleId=${circleId}` : "";

        return (
          <Link
            key={juz}
            to={`/${localePrefix}/memorization/student/${studentId}/juz/${juz}${query}`}
            className={cn(
              "flex flex-col gap-2 rounded-lg border p-3 transition-colors hover:bg-muted/50",
              "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
            )}
          >
            <div className="flex items-baseline justify-between gap-1">
              <span className="text-lg font-semibold tabular-nums">{juz}</span>
              <span className="text-xs text-muted-foreground tabular-nums">
                {Math.round(progress)}%
              </span>
            </div>
            <p className="line-clamp-2 text-sm leading-snug">
              {t(`memorization.juzNames.${juz}`)}
            </p>
            <Progress value={progress} className="h-1.5" />
          </Link>
        );
      })}
    </div>
  );
}
