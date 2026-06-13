import { useTranslation } from "react-i18next";
import { Link, useNavigate, useParams } from "react-router-dom";

import { Button } from "@/components/ui/button.tsx";
import { LocaleSwitcher } from "@/components/locale-switcher.tsx";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import { DEFAULT_LOCALE, translateRole } from "@/i18n/index.ts";

export function DashboardPage() {
  const { t } = useTranslation(["common", "auth"]);
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const { locale } = useParams<{ locale: string }>();
  const localePrefix = locale ?? DEFAULT_LOCALE;

  function handleLogout() {
    logout();
    navigate(`/${localePrefix}/login`, { replace: true });
  }

  return (
    <div className="min-h-svh bg-background">
      <header className="flex items-center justify-between border-b border-border px-6 py-4">
        <span className="font-serif text-xl font-semibold text-primary">
          {t("common:appName")}
        </span>
        <div className="flex items-center gap-3">
          <LocaleSwitcher />
          <Button variant="outline" size="sm" onClick={handleLogout}>
            {t("auth:dashboard.signOut")}
          </Button>
        </div>
      </header>
      <main className="mx-auto flex max-w-2xl flex-col gap-6 p-8">
        <div>
          <h1 className="font-serif text-3xl font-semibold text-foreground">
            {t("auth:dashboard.title")}
          </h1>
          {user ? (
            <p className="mt-2 text-muted-foreground">
              {t("auth:dashboard.welcome", {
                name: user.fullName,
                role: translateRole(user.role),
              })}
            </p>
          ) : null}
        </div>
        <nav className="flex flex-col gap-3">
          <Link
            to={`/${localePrefix}/settings/change-password`}
            className="rounded-lg border border-border bg-card px-4 py-3 text-sm font-medium text-foreground shadow-xs transition-colors hover:bg-muted"
          >
            {t("auth:changePassword.title")}
          </Link>
        </nav>
      </main>
    </div>
  );
}
