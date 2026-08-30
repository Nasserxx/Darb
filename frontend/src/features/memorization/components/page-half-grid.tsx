import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";

import { Badge } from "@/components/ui/badge.tsx";
import { Skeleton } from "@/components/ui/skeleton.tsx";
import type { CoverageEntry } from "@/features/memorization/types/index.ts";
import { MushafPageImage } from "@/features/mushaf/index.ts";
import { cn } from "@/lib/utils";

interface PageHalfGridProps {
  studentId: string;
  localePrefix: string;
  circleId: string;
  pages: number[];
  coverage?: CoverageEntry[];
  isLoading?: boolean;
}

function halfStatus(
  page: number,
  half: "A" | "B",
  coverage: CoverageEntry[],
): CoverageEntry | undefined {
  const key = `${page}-${half}`;
  return coverage.find(
    (entry) => entry.key === key || (entry.page === page && entry.half === half),
  );
}

export function PageHalfGrid({
  studentId,
  localePrefix,
  circleId,
  pages,
  coverage = [],
  isLoading,
}: PageHalfGridProps) {
  const { t } = useTranslation("app");

  if (isLoading) {
    return (
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
        {Array.from({ length: 12 }, (_, i) => (
          <Skeleton key={i} className="aspect-[3/4] rounded-lg" />
        ))}
      </div>
    );
  }

  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
      {pages.map((page) => (
        <div
          key={page}
          className="flex flex-col overflow-hidden rounded-lg border bg-card"
        >
          <div className="relative aspect-[3/4] bg-muted">
            <MushafPageImage
              page={page}
              alt={t("memorization.pageImageAlt", { page })}
              className="size-full object-cover"
            />
            <div className="absolute inset-0 flex flex-col">
              <HalfLink
                studentId={studentId}
                localePrefix={localePrefix}
                circleId={circleId}
                page={page}
                half="A"
                entry={halfStatus(page, "A", coverage)}
                position="top"
                label={t("memorization.halfA")}
              />
              <HalfLink
                studentId={studentId}
                localePrefix={localePrefix}
                circleId={circleId}
                page={page}
                half="B"
                entry={halfStatus(page, "B", coverage)}
                position="bottom"
                label={t("memorization.halfB")}
              />
            </div>
            <span className="absolute start-2 top-2 rounded bg-background/80 px-1.5 py-0.5 text-xs font-medium tabular-nums">
              {page}
            </span>
          </div>
        </div>
      ))}
    </div>
  );
}

function HalfLink({
  studentId,
  localePrefix,
  circleId,
  page,
  half,
  entry,
  position,
  label,
}: {
  studentId: string;
  localePrefix: string;
  circleId: string;
  page: number;
  half: "A" | "B";
  entry?: CoverageEntry;
  position: "top" | "bottom";
  label: string;
}) {
  const { t } = useTranslation("app");
  const assessed = entry?.assessed ?? false;

  return (
    <Link
      to={`/${localePrefix}/memorization/student/${studentId}/page/${page}/half/${half}?circleId=${circleId}`}
      className={cn(
        "relative flex flex-1 items-end justify-end p-1 transition-colors hover:bg-primary/10",
        position === "top" ? "border-b border-dashed border-border/60" : "",
      )}
    >
      <Badge
        variant={assessed ? "default" : "secondary"}
        className="text-[10px]"
      >
        {label}
        {assessed && entry?.grade
          ? ` · ${t(`memorization.grades.${entry.grade}`)}`
          : assessed
            ? ` · ${t("memorization.assessed")}`
            : ""}
      </Badge>
    </Link>
  );
}
