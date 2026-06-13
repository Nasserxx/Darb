import type { ReactNode } from "react";
import { useTranslation } from "react-i18next";

import { LocaleSwitcher } from "@/components/locale-switcher.tsx";
import { cn } from "@/lib/utils.ts";

type AuthShellProps = {
  title: string;
  subtitle?: string;
  children: ReactNode;
  footer?: ReactNode;
  className?: string;
};

export function AuthShell({
  title,
  subtitle,
  children,
  footer,
  className,
}: AuthShellProps) {
  const { t } = useTranslation("common");

  return (
    <div
      className={cn(
        "grid min-h-svh lg:grid-cols-[minmax(0,1.05fr)_minmax(0,1fr)]",
        className,
      )}
    >
      <aside
        className="brand-panel-pattern relative hidden flex-col justify-between overflow-hidden bg-primary px-10 py-12 text-primary-foreground lg:flex"
        aria-hidden={false}
      >
        <div className="flex items-start justify-between gap-4">
          <span className="font-serif text-3xl font-semibold tracking-tight">
            {t("appName")}
          </span>
          <LocaleSwitcher />
        </div>

        <div className="flex max-w-md flex-col gap-4">
          <p className="font-serif text-4xl leading-tight font-semibold tracking-tight">
            {t("tagline")}
          </p>
          <div
            className="h-1 w-16 rounded-full bg-accent"
            aria-hidden
          />
          <p className="text-sm leading-relaxed text-primary-foreground/85">
            {subtitle ?? t("tagline")}
          </p>
        </div>

        <p className="text-xs text-primary-foreground/60">
          © {new Date().getFullYear()} {t("appName")}
        </p>
      </aside>

      <div className="flex flex-col">
        <header className="flex items-center justify-between gap-4 border-b border-border bg-background/80 px-6 py-4 backdrop-blur-sm lg:hidden">
          <span className="font-serif text-2xl font-semibold text-foreground">
            {t("appName")}
          </span>
          <LocaleSwitcher />
        </header>

        <main className="flex flex-1 flex-col items-center justify-center px-6 py-10 sm:px-10">
          <div className="auth-stagger flex w-full max-w-md flex-col gap-8">
            <div className="flex flex-col gap-2 text-start">
              <h1 className="font-serif text-3xl font-semibold tracking-tight text-foreground sm:text-4xl">
                {title}
              </h1>
              {subtitle ? (
                <p className="text-sm text-muted-foreground">{subtitle}</p>
              ) : null}
            </div>

            {children}

            {footer ? (
              <div className="border-t border-border pt-6 text-center text-sm text-muted-foreground">
                {footer}
              </div>
            ) : null}
          </div>
        </main>
      </div>
    </div>
  );
}
