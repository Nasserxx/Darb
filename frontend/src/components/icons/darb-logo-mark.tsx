import { cn } from "@/lib/utils.ts";

const BRAND_TEAL = "#0c4a4e";
const BRAND_GOLD = "#b8952b";
const BRAND_PARCHMENT = "#f7f3eb";

type DarbLogoMarkProps = {
  className?: string;
  /** Light mark for dark (primary) backgrounds */
  inverted?: boolean;
};

/** Geometric path medallion — "Darb" (path) motif aligned with sanctuary brand. */
export function DarbLogoMark({ className, inverted = false }: DarbLogoMarkProps) {
  const ring = inverted ? BRAND_PARCHMENT : BRAND_TEAL;
  const pathStroke = BRAND_GOLD;
  const innerFill = inverted ? "color-mix(in srgb, #f7f3eb 12%, transparent)" : "color-mix(in srgb, #0c4a4e 8%, transparent)";

  return (
    <svg
      viewBox="0 0 32 32"
      preserveAspectRatio="xMidYMid meet"
      className={cn("block shrink-0", className)}
      aria-hidden
    >
      <circle cx="16" cy="16" r="15" fill={ring} />
      <circle cx="16" cy="16" r="13.5" fill="none" stroke={pathStroke} strokeWidth="0.75" opacity="0.55" />
      <circle cx="16" cy="16" r="11" fill={innerFill} />
      <path
        d="M9 21.5 C12 11.5 20 11.5 23 21.5"
        fill="none"
        stroke={pathStroke}
        strokeWidth="2"
        strokeLinecap="round"
      />
      <path
        d="M16 9 L17.8 12.2 L16 15.4 L14.2 12.2 Z"
        fill={pathStroke}
        opacity="0.95"
      />
    </svg>
  );
}
