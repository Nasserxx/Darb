import { Link, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";

import { PageHeader } from "@/components/shared/page-header.tsx";
import { Button } from "@/components/ui/button";
import { DEFAULT_LOCALE } from "@/i18n/index.ts";
import { getDefaultLandingPath } from "@/lib/navigation/app-nav.ts";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";

export function ForbiddenPage() {
  const { t } = useTranslation("app");
  const { locale } = useParams<{ locale: string }>();
  const { user } = useAuth();
  const localePrefix = locale ?? DEFAULT_LOCALE;
  const homePath = user
    ? `/${localePrefix}${getDefaultLandingPath(user.role)}`
    : `/${localePrefix}/login`;

  return (
    <div className="mx-auto flex max-w-lg flex-col gap-6 py-16">
      <PageHeader
        title={t("forbidden.title")}
        description={t("forbidden.description")}
      />
      <Button asChild>
        <Link to={homePath}>{t("forbidden.back")}</Link>
      </Button>
    </div>
  );
}
