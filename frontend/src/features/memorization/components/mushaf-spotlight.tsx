import { useTranslation } from "react-i18next";

import type { PageHalf } from "@/features/mushaf/types/index.ts";
import type { MemorizationStamp } from "@/features/memorization/types/index.ts";
import { MushafPageImage } from "@/features/mushaf/index.ts";
import { cn } from "@/lib/utils";

interface MushafSpotlightProps {
  page: number;
  activeHalf: PageHalf;
  stamps: MemorizationStamp[];
  readonly?: boolean;
  onImageClick?: (x: number, y: number) => void;
  className?: string;
}

export function MushafSpotlight({
  page,
  activeHalf,
  stamps,
  readonly,
  onImageClick,
  className,
}: MushafSpotlightProps) {
  const { t } = useTranslation("app");

  function handleClick(event: React.MouseEvent<HTMLDivElement>) {
    if (readonly || !onImageClick) return;
    const rect = event.currentTarget.getBoundingClientRect();
    const x = (event.clientX - rect.left) / rect.width;
    const y = (event.clientY - rect.top) / rect.height;
    onImageClick(x, y);
  }

  return (
    <div className={cn("relative overflow-hidden rounded-lg border bg-muted", className)}>
      <div className="relative aspect-[2/3] w-full">
        <MushafPageImage
          page={page}
          half={activeHalf}
          alt={t("memorization.pageImageAlt", { page })}
          className="size-full object-contain"
        />
        <div
          role={readonly ? undefined : "button"}
          tabIndex={readonly ? undefined : 0}
          onClick={handleClick}
          onKeyDown={(event) => {
            if (readonly || !onImageClick) return;
            if (event.key === "Enter" || event.key === " ") {
              event.preventDefault();
              onImageClick(0.5, activeHalf === "A" ? 0.25 : 0.75);
            }
          }}
          className={cn(
            "absolute inset-0",
            !readonly && onImageClick && "cursor-crosshair",
          )}
        >
          <div
            className={cn(
              "absolute inset-x-0 top-0 h-1/2 transition-opacity",
              activeHalf === "A" ? "opacity-100" : "opacity-40",
            )}
            style={{
              background: activeHalf !== "A" ? "rgba(0,0,0,0.35)" : undefined,
            }}
          />
          <div
            className={cn(
              "absolute inset-x-0 bottom-0 h-1/2 transition-opacity",
              activeHalf === "B" ? "opacity-100" : "opacity-40",
            )}
            style={{
              background: activeHalf !== "B" ? "rgba(0,0,0,0.35)" : undefined,
            }}
          />
          {stamps.map((stamp, index) => {
            if (stamp.x === undefined || stamp.y === undefined) return null;
            const inActiveHalf =
              (activeHalf === "A" && stamp.y <= 0.5) ||
              (activeHalf === "B" && stamp.y > 0.5);
            if (!inActiveHalf) return null;
            return (
              <span
                key={`${stamp.surah}-${stamp.ayah}-${index}`}
                className={cn(
                  "absolute size-3 -translate-x-1/2 -translate-y-1/2 rounded-full border-2 border-background shadow-sm",
                  stamp.type === "TAJWEED" ? "bg-amber-500" : "bg-red-500",
                )}
                style={{
                  left: `${stamp.x * 100}%`,
                  top: `${stamp.y * 100}%`,
                }}
                title={`${stamp.surah}:${stamp.ayah}`}
              />
            );
          })}
        </div>
      </div>
    </div>
  );
}
