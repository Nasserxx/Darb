import type { ComponentType } from "react";
import { cva, type VariantProps } from "class-variance-authority";

import type { Locale } from "@/i18n/index.ts";
import { cn } from "@/lib/utils.ts";

const FLAG_BLUE = "#012169";
const FLAG_WHITE = "#ffffff";
const FLAG_RED = "#c8102e";
const FLAG_SAUDI_GREEN = "#006c35";
const FLAG_GERMANY_BLACK = "#000000";
const FLAG_GERMANY_RED = "#dd0000";
const FLAG_GERMANY_GOLD = "#ffce00";
/** Representative flags for locale codes (not nationality). Always pair with native endonyms in UI. */
function EnFlag({ className }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 32 32"
      preserveAspectRatio="xMidYMid slice"
      className={className}
      aria-hidden
    >
      <rect width="32" height="32" fill={FLAG_BLUE} />
      <path fill={FLAG_WHITE} d="M0 0 32 32 28 32 0 4Z" />
      <path fill={FLAG_WHITE} d="M32 0 0 32 4 32 32 4Z" />
      <path fill={FLAG_RED} d="M1 0 32 31 29 32 0 2Z" />
      <path fill={FLAG_RED} d="M31 0 0 31 3 32 32 2Z" />
      <rect x="13" width="6" height="32" fill={FLAG_WHITE} />
      <rect y="13" width="32" height="6" fill={FLAG_WHITE} />
      <rect x="14" width="4" height="32" fill={FLAG_RED} />
      <rect y="14" width="32" height="4" fill={FLAG_RED} />
    </svg>
  );
}

function ArFlag({ className }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 32 32"
      preserveAspectRatio="xMidYMid slice"
      className={className}
      aria-hidden
    >
      <rect width="32" height="32" fill={FLAG_SAUDI_GREEN} />
      <rect x="5" y="8" width="22" height="3.5" rx="0.5" fill={FLAG_WHITE} />
      <rect x="6" y="13" width="20" height="2.5" rx="0.5" fill={FLAG_WHITE} />
      <rect x="7" y="21" width="14" height="2" fill={FLAG_WHITE} />
      <path fill={FLAG_WHITE} d="M20 19.5 27 22.5 20 25.5Z" />
    </svg>
  );
}

function DeFlag({ className }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 32 32"
      preserveAspectRatio="xMidYMid slice"
      className={className}
      aria-hidden
    >
      <rect width="32" height="10.67" fill={FLAG_GERMANY_BLACK} />
      <rect y="10.67" width="32" height="10.67" fill={FLAG_GERMANY_RED} />
      <rect y="21.34" width="32" height="10.66" fill={FLAG_GERMANY_GOLD} />
    </svg>
  );
}

const FLAG_COMPONENTS: Record<
  Locale,
  ComponentType<{ className?: string }>
> = {
  en: EnFlag,
  ar: ArFlag,
  de: DeFlag,
};

type LocaleFlagProps = {
  locale: Locale;
  className?: string;
};

export function LocaleFlag({ locale, className }: LocaleFlagProps) {
  const Flag = FLAG_COMPONENTS[locale];
  return (
    <span dir="ltr" aria-hidden className="inline-flex shrink-0">
      <Flag className={cn("block", className)} />
    </span>
  );
}

const medallionVariants = cva(
  "inline-flex shrink-0 items-center justify-center overflow-hidden rounded-full border-2 border-accent bg-card shadow-xs",
  {
    variants: {
      size: {
        sm: "size-7",
        md: "size-9",
        lg: "size-11",
      },
      active: {
        true: "ring-2 ring-accent/50 shadow-[0_0_10px_color-mix(in_srgb,var(--accent)_35%,transparent)]",
        false: "",
      },
      elevated: {
        true: "shadow-md",
        false: "shadow-sm",
      },
    },
    defaultVariants: {
      size: "md",
      active: false,
      elevated: false,
    },
  },
);

type FlagMedallionProps = {
  locale: Locale;
  className?: string;
} & VariantProps<typeof medallionVariants>;

export function FlagMedallion({
  locale,
  size,
  active,
  elevated,
  className,
}: FlagMedallionProps) {
  const Flag = FLAG_COMPONENTS[locale];

  return (
    <span
      dir="ltr"
      aria-hidden
      className={cn(
        medallionVariants({ size, active, elevated }),
        className,
      )}
    >
      <Flag className="block size-full" />
    </span>
  );
}
