import { XIcon } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";

import { Badge } from "@/components/ui/badge.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Combobox } from "@/components/ui/combobox.tsx";
import { Field, FieldLabel } from "@/components/ui/field.tsx";
import { Input } from "@/components/ui/input.tsx";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import { CitySelect } from "@/features/mosques/components/city-select.tsx";
import { useMosque, useMosques } from "@/features/mosques/hooks/use-mosques.ts";
import { getCountryOptions } from "@/lib/countries.ts";
import { normalizeApiRole } from "@/lib/navigation/app-nav.ts";
import {
  clearLastMosque,
  readLastMosque,
  writeLastMosque,
} from "@/lib/super-admin/last-mosque-storage.ts";

const MOSQUE_PICKER_PAGE_SIZE = 100;

export type SuperAdminScopeFilters = {
  mosqueId?: string;
  q?: string;
};

export type SuperAdminMosqueScopeBarProps = {
  mosqueId?: string;
  q?: string;
  nameFilter?: {
    label: string;
    placeholder: string;
  };
  onApply: (filters: SuperAdminScopeFilters) => void;
  /** When set, overrides role check. Default: visible only for SUPER_ADMIN. */
  enabled?: boolean;
};

export function SuperAdminMosqueScopeBar({
  mosqueId,
  q,
  nameFilter,
  onApply,
  enabled,
}: SuperAdminMosqueScopeBarProps) {
  const { t, i18n } = useTranslation("app");
  const { user } = useAuth();
  const role = user ? normalizeApiRole(user.role) : null;
  const isEnabled = enabled ?? role === "SUPER_ADMIN";

  const [draftMosqueId, setDraftMosqueId] = useState<string | undefined>(
    mosqueId,
  );
  const [draftQ, setDraftQ] = useState(q ?? "");
  const [country, setCountry] = useState<string | undefined>();
  const [city, setCity] = useState<string | null>(null);
  const [suggestionDismissed, setSuggestionDismissed] = useState(false);

  // Keep draft in sync with applied URL scope (D23).
  useEffect(() => {
    setDraftMosqueId(mosqueId);
  }, [mosqueId]);

  useEffect(() => {
    setDraftQ(q ?? "");
  }, [q]);

  const countryOptions = useMemo(
    () => getCountryOptions(i18n.language),
    [i18n.language],
  );

  const listParams = useMemo(
    () => ({
      page: 0,
      size: MOSQUE_PICKER_PAGE_SIZE,
      country: country || undefined,
      city: city?.trim() || undefined,
    }),
    [country, city],
  );

  const { data: mosquesPage } = useMosques(listParams);
  // Resolve applied/draft mosque even when it is outside the picker page (D23).
  const mosqueToResolve = draftMosqueId ?? mosqueId;
  const { data: selectedMosque } = useMosque(mosqueToResolve ?? "", {
    enabled: Boolean(mosqueToResolve),
  });

  const mosqueOptions = useMemo(() => {
    const byId = new Map<string, string>();
    for (const mosque of mosquesPage?.content ?? []) {
      byId.set(mosque.id, mosque.name);
    }
    if (selectedMosque) {
      byId.set(selectedMosque.id, selectedMosque.name);
    }
    // Show a label immediately while useMosque loads (last-mosque session hint).
    if (mosqueToResolve && !byId.has(mosqueToResolve)) {
      const last = readLastMosque();
      if (last?.id === mosqueToResolve) {
        byId.set(mosqueToResolve, last.name);
      }
    }
    return [...byId.entries()].map(([value, label]) => ({ value, label }));
  }, [mosquesPage?.content, selectedMosque, mosqueToResolve]);

  const lastMosque =
    !mosqueId && !suggestionDismissed ? readLastMosque() : null;

  if (!isEnabled) {
    return null;
  }

  function handleSearch() {
    // Empty mosque + Search = no-op; never clear applied mosqueId (D6).
    if (!draftMosqueId) {
      return;
    }
    const trimmedQ = draftQ.trim();
    const name =
      mosqueOptions.find((option) => option.value === draftMosqueId)?.label ??
      selectedMosque?.name ??
      draftMosqueId;
    writeLastMosque({ id: draftMosqueId, name });
    onApply({
      mosqueId: draftMosqueId,
      q: trimmedQ || undefined,
    });
  }

  function clearAll() {
    setDraftMosqueId(undefined);
    setDraftQ("");
    setCountry(undefined);
    setCity(null);
    clearLastMosque();
    setSuggestionDismissed(false);
    onApply({ mosqueId: undefined, q: undefined });
  }

  function applySuggestion() {
    if (!lastMosque) return;
    setDraftMosqueId(lastMosque.id);
  }

  const hasFilters =
    Boolean(mosqueId) ||
    Boolean(q) ||
    Boolean(draftMosqueId) ||
    Boolean(draftQ) ||
    Boolean(country) ||
    Boolean(city);
  const appliedQ = q ?? "";
  const searchDisabled =
    !draftMosqueId ||
    (draftMosqueId === mosqueId && draftQ.trim() === appliedQ.trim());
  const showNameFilter = Boolean(nameFilter && draftMosqueId);

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap items-end gap-3">
        <Field className="w-48">
          <FieldLabel>{t("superAdminScope.country")}</FieldLabel>
          <Combobox
            options={countryOptions}
            value={country ?? null}
            onValueChange={(value) => {
              // ponytail: country/city only narrow picker options; keep draft while URL scoped
              setCountry(value ?? undefined);
              setCity(null);
            }}
            placeholder={t("superAdminScope.allCountries")}
          />
        </Field>
        <Field className="w-48">
          <FieldLabel htmlFor="sa-scope-city">
            {t("superAdminScope.city")}
          </FieldLabel>
          <CitySelect
            id="sa-scope-city"
            country={country}
            value={city}
            onValueChange={setCity}
            mode="filter"
            activeOnly={false}
            placeholder={t("superAdminScope.filterCity")}
          />
        </Field>
        <Field className="w-64">
          <FieldLabel>{t("superAdminScope.mosque")}</FieldLabel>
          <Combobox
            options={mosqueOptions}
            value={draftMosqueId ?? null}
            onValueChange={(value) => {
              setDraftMosqueId(value ?? undefined);
              if (!value) {
                setDraftQ("");
              }
            }}
            placeholder={t("superAdminScope.mosquePlaceholder")}
            searchPlaceholder={t("superAdminScope.mosqueSearch")}
          />
        </Field>
        {showNameFilter ? (
          <Field className="w-56">
            <FieldLabel htmlFor="sa-scope-name">{nameFilter!.label}</FieldLabel>
            <Input
              id="sa-scope-name"
              dir="auto"
              value={draftQ}
              onChange={(event) => setDraftQ(event.target.value)}
              placeholder={nameFilter!.placeholder}
            />
          </Field>
        ) : null}
        <Button
          type="button"
          variant="default"
          disabled={searchDisabled}
          onClick={handleSearch}
        >
          {t("superAdminScope.search")}
        </Button>
        {hasFilters ? (
          <Button type="button" variant="outline" onClick={clearAll}>
            {t("superAdminScope.clearAll")}
          </Button>
        ) : null}
      </div>

      {!mosqueId ? (
        <div className="flex flex-wrap items-center gap-2">
          <Badge variant="secondary">{t("superAdminScope.allMosques")}</Badge>
          {lastMosque ? (
            <>
              <Button
                type="button"
                variant="secondary"
                size="sm"
                onClick={applySuggestion}
              >
                {t("superAdminScope.suggestFilter", { name: lastMosque.name })}
              </Button>
              <Button
                type="button"
                variant="ghost"
                size="icon-sm"
                onClick={() => setSuggestionDismissed(true)}
                aria-label={t("superAdminScope.dismissSuggestion")}
              >
                <XIcon />
              </Button>
            </>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}
