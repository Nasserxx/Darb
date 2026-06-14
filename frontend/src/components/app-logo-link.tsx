import { cva, type VariantProps } from "class-variance-authority";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";

import { DarbLogoMark } from "@/components/icons/darb-logo-mark.tsx";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import { DEFAULT_LOCALE } from "@/i18n/index.ts";
import { cn } from "@/lib/utils.ts";

const logoLinkVariants = cva(
  [
    "group inline-flex min-w-0 items-center gap-2.5 rounded-lg outline-none",
    "transition-[opacity,transform,box-shadow] duration-200",
    "focus-visible:ring-2 focus-visible:ring-offset-2",
    "active:scale-[0.98]",
  ],
  {
    variants: {
      variant: {
        default:
          "text-primary hover:opacity-90 focus-visible:ring-ring focus-visible:ring-offset-background",
        onPrimary:
          "text-primary-foreground hover:opacity-90 focus-visible:ring-primary-foreground/50 focus-visible:ring-offset-primary",
      },
      size: {
        sm: "[&_[data-logo-mark]]:size-8 [&_[data-logo-name]]:text-xl",
        md: "[&_[data-logo-mark]]:size-9 [&_[data-logo-name]]:text-2xl",
        lg: "[&_[data-logo-mark]]:size-10 [&_[data-logo-name]]:text-3xl",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "md",
    },
  },
);

type AppLogoLinkProps = VariantProps<typeof logoLinkVariants> & {
  className?: string;
};

export function AppLogoLink({ variant, size, className }: AppLogoLinkProps) {
  const { t } = useTranslation("common");
  const { isAuthenticated } = useAuth();
  const { locale } = useParams<{ locale: string }>();
  const localePrefix = locale ?? DEFAULT_LOCALE;
  const homePath = isAuthenticated
    ? `/${localePrefix}/dashboard`
    : `/${localePrefix}/login`;

  return (
    <Link
      to={homePath}
      className={cn(logoLinkVariants({ variant, size }), className)}
      aria-label={t("goToHome")}
    >
      <span
        data-logo-mark
        className="relative flex shrink-0 items-center justify-center rounded-full shadow-[0_0_0_1px_color-mix(in_srgb,var(--accent)_35%,transparent),0_2px_8px_color-mix(in_srgb,#000_12%,transparent)] transition-shadow group-hover:shadow-[0_0_0_1px_color-mix(in_srgb,var(--accent)_55%,transparent),0_4px_14px_color-mix(in_srgb,#000_16%,transparent)]"
      >
        <DarbLogoMark className="size-full rounded-full" inverted={variant === "onPrimary"} />
      </span>
      <span
        data-logo-name
        className="truncate font-serif font-semibold tracking-tight"
      >
        {t("appName")}
      </span>
    </Link>
  );
}
