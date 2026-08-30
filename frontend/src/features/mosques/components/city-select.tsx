import { useMemo } from "react";
import { useTranslation } from "react-i18next";

import { Combobox, type ComboboxOption } from "@/components/ui/combobox.tsx";

import { useMosqueCities } from "../hooks/use-mosques.ts";

type CitySelectProps = {
  country: string | undefined | null;
  value: string | null;
  onValueChange: (value: string | null) => void;
  mode: "filter" | "creatable";
  activeOnly?: boolean;
  allowCustomValue?: string | null;
  disabled?: boolean;
  id?: string;
  placeholder?: string;
  error?: boolean;
};

export function CitySelect({
  country,
  value,
  onValueChange,
  mode,
  activeOnly = false,
  allowCustomValue,
  disabled,
  id,
  placeholder,
  error,
}: CitySelectProps) {
  const { t } = useTranslation("app");
  const countryCode = country?.trim() || undefined;
  const { data: cities = [] } = useMosqueCities(countryCode, { activeOnly });

  const options = useMemo<ComboboxOption[]>(() => {
    const base = cities.map((city) => ({ value: city, label: city }));
    if (
      allowCustomValue &&
      !base.some((option) => option.value === allowCustomValue)
    ) {
      return [{ value: allowCustomValue, label: allowCustomValue }, ...base];
    }
    return base;
  }, [cities, allowCustomValue]);

  const countryMissing = !countryCode;
  const isDisabled = Boolean(disabled) || countryMissing;

  return (
    <Combobox
      id={id}
      options={options}
      value={value}
      onValueChange={onValueChange}
      disabled={isDisabled}
      placeholder={
        countryMissing
          ? t("mosques.selectCountryFirst")
          : (placeholder ?? t("mosques.city"))
      }
      searchPlaceholder={t("mosques.citySearchPlaceholder")}
      emptyLabel={t("mosques.noCities")}
      onCreateValue={
        mode === "creatable" ? (next) => onValueChange(next) : undefined
      }
      createLabel={(query) => t("mosques.useCustomCity", { city: query })}
      className={error ? "border-destructive" : undefined}
    />
  );
}
