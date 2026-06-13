import { useTranslation } from "react-i18next";
import { useLocation, useNavigate, useParams } from "react-router-dom";

import { Button } from "@/components/ui/button.tsx";
import {
  SUPPORTED_LOCALES,
  persistLocale,
  type Locale,
} from "@/i18n/index.ts";
import { cn } from "@/lib/utils.ts";

function isLocale(value: string | undefined): value is Locale {
  return (
    value !== undefined &&
    (SUPPORTED_LOCALES as readonly string[]).includes(value)
  );
}

export function LocaleSwitcher({ className }: { className?: string }) {
  const { t } = useTranslation("common");
  const { locale: localeParam } = useParams<{ locale: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const activeLocale = isLocale(localeParam) ? localeParam : "en";

  function switchLocale(next: Locale) {
    if (next === activeLocale) {
      return;
    }
    persistLocale(next);
    const suffix = location.pathname.replace(/^\/[^/]+/, "") || "/login";
    navigate(`/${next}${suffix}${location.search}`, { replace: true });
  }

  return (
    <div
      role="group"
      aria-label={t("language")}
      className={cn("inline-flex items-center gap-1 rounded-lg border border-border bg-card/80 p-1", className)}
    >
      {SUPPORTED_LOCALES.map((code) => (
        <Button
          key={code}
          type="button"
          variant={activeLocale === code ? "default" : "ghost"}
          size="sm"
          className="min-w-9 px-2 uppercase"
          onClick={() => switchLocale(code)}
          aria-pressed={activeLocale === code}
        >
          {code}
        </Button>
      ))}
    </div>
  );
}
