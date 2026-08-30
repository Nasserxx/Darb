import { useTranslation } from "react-i18next";

import type { MemorizationStamp } from "@/features/memorization/types/index.ts";
import { stampKey } from "@/features/memorization/hooks/use-stamp-session.ts";
import { cn } from "@/lib/utils";

interface AyahStampListProps {
  ayahs: Array<{ surah: number; ayah: number }>;
  stamps: MemorizationStamp[];
  readonly?: boolean;
  onAyahClick?: (surah: number, ayah: number) => void;
}

export function AyahStampList({
  ayahs,
  stamps,
  readonly,
  onAyahClick,
}: AyahStampListProps) {
  const { t } = useTranslation("app");

  const stampsByAyah = new Map<string, MemorizationStamp[]>();
  for (const stamp of stamps) {
    const key = stampKey(stamp);
    const list = stampsByAyah.get(key) ?? [];
    list.push(stamp);
    stampsByAyah.set(key, list);
  }

  return (
    <ul className="flex flex-col gap-1 overflow-y-auto text-sm leading-relaxed">
      {ayahs.map(({ surah, ayah }) => {
        const key = stampKey({ surah, ayah });
        const ayahStamps = stampsByAyah.get(key) ?? [];
        const hasTajweed = ayahStamps.some((s) => s.type === "TAJWEED");
        const hasHifz = ayahStamps.some((s) => s.type === "HIFZ");

        return (
          <li key={key}>
            <button
              type="button"
              disabled={readonly && !onAyahClick}
              onClick={() => onAyahClick?.(surah, ayah)}
              className={cn(
                "flex w-full items-start gap-2 rounded-md px-2 py-1.5 text-start transition-colors",
                !readonly && onAyahClick ? "hover:bg-muted cursor-pointer" : "cursor-default",
                (hasTajweed || hasHifz) && "bg-muted/50",
              )}
            >
              <span className="shrink-0 font-medium tabular-nums text-muted-foreground">
                {surah}:{ayah}
              </span>
              <span className="font-arabic flex-1">
                {t("memorization.ayahPlaceholder")}
              </span>
              <span className="flex shrink-0 gap-1">
                {hasTajweed ? (
                  <span
                    className="size-2 rounded-full bg-amber-500"
                    title={t("memorization.stampTajweed")}
                  />
                ) : null}
                {hasHifz ? (
                  <span
                    className="size-2 rounded-full bg-red-500"
                    title={t("memorization.stampHifz")}
                  />
                ) : null}
              </span>
            </button>
          </li>
        );
      })}
    </ul>
  );
}
