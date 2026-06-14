import { cva, type VariantProps } from "class-variance-authority";
import { ChevronDownIcon } from "lucide-react";
import { useTranslation } from "react-i18next";
import { useLocation, useNavigate, useParams } from "react-router-dom";

import { FlagMedallion, LocaleFlag } from "@/components/icons/locale-flag.tsx";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuLabel,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu.tsx";
import {
  isRtlLocale,
  persistLocale,
  SUPPORTED_LOCALES,
  type Locale,
} from "@/i18n/index.ts";
import { cn } from "@/lib/utils.ts";

/** Native endonyms — stable labels regardless of active UI locale (avoids flicker on switch). */
const LOCALE_NATIVE_NAMES: Record<Locale, string> = {
  en: "English",
  ar: "العربية",
  de: "Deutsch",
};

const triggerVariants = cva(
  [
    "group inline-flex h-11 min-w-[10.5rem] items-center justify-between gap-2.5 rounded-full border px-3.5 text-sm shadow-xs",
    "transition-[color,box-shadow,background-color,ring] outline-none",
    "focus-visible:ring-2 focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 sm:h-10",
    "data-[state=open]:ring-2 data-[state=open]:ring-accent/50",
    "data-[state=open]:shadow-[0_0_20px_color-mix(in_srgb,var(--accent)_42%,transparent),inset_0_1px_0_color-mix(in_srgb,var(--accent)_25%,transparent)]",
  ],
  {
    variants: {
      variant: {
        default:
          "border-border/80 bg-card/90 text-foreground backdrop-blur-sm hover:bg-card focus-visible:ring-ring focus-visible:ring-offset-background",
        onPrimary:
          "border-primary-foreground/25 bg-primary-foreground/10 text-primary-foreground backdrop-blur-md hover:bg-primary-foreground/15 focus-visible:ring-primary-foreground/40 focus-visible:ring-offset-primary data-[state=open]:ring-primary-foreground/40 data-[state=open]:shadow-[0_0_22px_color-mix(in_srgb,var(--accent)_55%,transparent),0_0_8px_color-mix(in_srgb,var(--primary-foreground)_18%,transparent)]",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  },
);

const triggerMedallionVariants = cva(
  "shrink-0 transition-[box-shadow,transform] duration-300 group-data-[state=open]:scale-105",
  {
    variants: {
      variant: {
        default:
          "group-data-[state=open]:shadow-[0_0_0_2px_color-mix(in_srgb,var(--accent)_30%,transparent),0_0_14px_color-mix(in_srgb,var(--accent)_48%,transparent)]",
        onPrimary:
          "opacity-95 group-data-[state=open]:opacity-100 group-data-[state=open]:shadow-[0_0_0_2px_color-mix(in_srgb,var(--accent)_35%,transparent),0_0_16px_color-mix(in_srgb,var(--accent)_52%,transparent)]",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  },
);

const rowMedallionVariants = cva(
  "shrink-0 transition-transform duration-200 group-hover/locale-row:scale-[1.04]",
  {
    variants: {
      variant: {
        default: "",
        onPrimary: "",
      },
      active: {
        true: "ring-accent/60 shadow-[0_0_10px_color-mix(in_srgb,var(--accent)_38%,transparent)]",
        false: "opacity-90 group-hover/locale-row:opacity-100",
      },
    },
    defaultVariants: {
      variant: "default",
      active: false,
    },
  },
);

function isLocale(value: string | undefined): value is Locale {
  return (
    value !== undefined &&
    (SUPPORTED_LOCALES as readonly string[]).includes(value)
  );
}

function buildLocalePath(
  nextLocale: Locale,
  pathname: string,
  search: string,
  hash: string,
): string {
  const suffix = pathname.replace(/^\/[^/]+/, "") || "/login";
  return `/${nextLocale}${suffix}${search}${hash}`;
}

type LocaleSwitcherProps = {
  className?: string;
} & VariantProps<typeof triggerVariants>;

export function LocaleSwitcher({
  className,
  variant = "default",
}: LocaleSwitcherProps) {
  const { t, i18n } = useTranslation("common");
  const { locale: localeParam } = useParams<{ locale: string }>();
  const navigate = useNavigate();
  const location = useLocation();

  const activeLocale: Locale = isLocale(i18n.language)
    ? i18n.language
    : isLocale(localeParam)
      ? localeParam
      : "en";
  const onPrimary = variant === "onPrimary";

  function switchLocale(next: Locale) {
    if (next === activeLocale) {
      return;
    }
    persistLocale(next);
    navigate(
      buildLocalePath(next, location.pathname, location.search, location.hash),
      { replace: true },
    );
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        className={cn(triggerVariants({ variant }), className)}
        aria-label={`${t("language")}: ${LOCALE_NATIVE_NAMES[activeLocale]}`}
      >
        <span className="flex min-w-0 items-center gap-2.5">
          <FlagMedallion
            key={activeLocale}
            locale={activeLocale}
            size="sm"
            active
            elevated
            className={triggerMedallionVariants({ variant })}
          />
          <span
            className={cn(
              "truncate text-[0.9375rem] font-medium tracking-tight",
              isRtlLocale(activeLocale) ? "font-sans" : "font-serif",
            )}
            dir={isRtlLocale(activeLocale) ? "rtl" : "ltr"}
          >
            {LOCALE_NATIVE_NAMES[activeLocale]}
          </span>
        </span>
        <ChevronDownIcon
          className={cn(
            "size-4 shrink-0 opacity-60 transition-transform duration-200 group-data-[state=open]:rotate-180",
            onPrimary ? "text-primary-foreground" : "text-muted-foreground",
          )}
          aria-hidden
        />
      </DropdownMenuTrigger>

      <DropdownMenuContent
        align="end"
        sideOffset={10}
        className="w-[17rem] overflow-hidden border-border/60 bg-[radial-gradient(ellipse_120%_80%_at_50%_-20%,#fffdf8_0%,#f7f3eb_42%,#e8dfd0_100%)] p-0 shadow-xl"
      >
        <div
          className="relative overflow-hidden border-b border-border/40 px-4 pt-3.5 pb-3"
          aria-hidden
        >
          <div
            className="pointer-events-none absolute -inset-x-8 -top-12 h-24 bg-[radial-gradient(ellipse_at_center,color-mix(in_srgb,var(--accent)_22%,transparent)_0%,transparent_70%)]"
            aria-hidden
          />
          <div className="absolute inset-x-6 top-0 h-px bg-linear-to-r from-transparent via-accent to-transparent opacity-80" />

          <div className="relative mb-3 flex items-center justify-center gap-2">
            <span className="h-px w-8 bg-linear-to-r from-transparent to-accent/60" />
            <span className="relative flex size-3 items-center justify-center">
              <span className="absolute inset-0 rotate-45 border border-accent/60 bg-accent/20" />
              <span className="absolute inset-[2px] rotate-45 border border-accent/40" />
            </span>
            <span className="h-px w-8 bg-linear-to-l from-transparent to-accent/60" />
          </div>

          <div className="relative flex items-end justify-center gap-2">
            {SUPPORTED_LOCALES.map((code, index) => (
              <span
                key={code}
                className={cn(
                  "transition-opacity",
                  code === activeLocale ? "opacity-100" : "opacity-40",
                )}
                style={{
                  transform: `translateY(${index === 1 ? "-2px" : "0"})`,
                }}
              >
                <FlagMedallion
                  locale={code}
                  size="sm"
                  active={code === activeLocale}
                  className={cn(
                    code === activeLocale && "scale-110",
                    "transition-transform",
                  )}
                />
              </span>
            ))}
          </div>
        </div>

        <div className="flex flex-col gap-0.5 p-1.5">
          <DropdownMenuLabel
            id="locale-switcher-label"
            className="px-2.5 py-1 font-serif text-xs tracking-[0.16em] text-muted-foreground uppercase"
          >
            {t("language")}
          </DropdownMenuLabel>

          <DropdownMenuRadioGroup
            value={activeLocale}
            aria-labelledby="locale-switcher-label"
            onValueChange={(value) => switchLocale(value as Locale)}
          >
            {SUPPORTED_LOCALES.map((code) => {
              const isActive = code === activeLocale;
              const isRtl = isRtlLocale(code);

              return (
                <DropdownMenuRadioItem
                  key={code}
                  value={code}
                  className={cn(
                    "group/locale-row relative cursor-pointer gap-3.5 rounded-lg px-2.5 py-3 pe-9 focus:bg-muted/45",
                    isActive && "bg-primary/10 focus:bg-primary/12",
                  )}
                >
                  {isActive ? (
                    <span
                      className="absolute inset-y-2 start-0 w-[3px] rounded-full bg-accent shadow-[0_0_8px_color-mix(in_srgb,var(--accent)_55%,transparent)]"
                      aria-hidden
                    />
                  ) : null}

                  <FlagMedallion
                    locale={code}
                    size="md"
                    active={isActive}
                    elevated={isActive}
                    className={rowMedallionVariants({
                      variant,
                      active: isActive,
                    })}
                  />

                  <span className="flex min-w-0 flex-1 flex-col gap-0.5">
                    <span
                      className={cn(
                        "truncate text-sm leading-tight font-medium",
                        isRtl ? "font-sans" : "font-serif",
                        isActive ? "text-foreground" : "text-foreground/88",
                      )}
                      dir={isRtl ? "rtl" : "ltr"}
                    >
                      {LOCALE_NATIVE_NAMES[code]}
                    </span>
                    <span
                      className="flex items-center gap-1.5 font-sans text-[0.625rem] font-medium tracking-[0.12em] text-muted-foreground uppercase"
                      aria-hidden
                    >
                      <LocaleFlag locale={code} className="size-2.5 rounded-[1px]" />
                      {code}
                    </span>
                  </span>
                </DropdownMenuRadioItem>
              );
            })}
          </DropdownMenuRadioGroup>
        </div>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
