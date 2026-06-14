import { useEffect } from "react";
import { Navigate, Outlet, useLocation, useParams } from "react-router-dom";

import {
  DEFAULT_LOCALE,
  SUPPORTED_LOCALES,
  persistLocale,
  type Locale,
} from "../i18n/index.ts";

function isSupportedLocale(value: string | undefined): value is Locale {
  return (
    value !== undefined &&
    (SUPPORTED_LOCALES as readonly string[]).includes(value)
  );
}

export function LocaleLayout() {
  const { locale: localeParam } = useParams<{ locale: string }>();
  const location = useLocation();

  useEffect(() => {
    if (isSupportedLocale(localeParam)) {
      persistLocale(localeParam);
    }
  }, [localeParam]);

  if (!isSupportedLocale(localeParam)) {
    const pathSuffix =
      location.pathname.replace(/^\/[^/]+/, "") || "/login";
    return (
      <Navigate to={`/${DEFAULT_LOCALE}${pathSuffix}`} replace />
    );
  }

  return <Outlet />;
}
