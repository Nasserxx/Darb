import { useMemo } from "react";

import { Combobox } from "@/components/ui/combobox.tsx";
import { getTimeZoneOptions, type TimeZoneOption } from "@/lib/timezones.ts";

type TimeZoneSelectProps = {
  value: string | null;
  onValueChange: (value: string | null) => void;
  error?: boolean;
  id?: string;
  placeholder?: string;
  allowCustomValue?: string | null;
};

export function TimeZoneSelect({
  value,
  onValueChange,
  error,
  id,
  placeholder,
  allowCustomValue,
}: TimeZoneSelectProps) {
  const options = useMemo<TimeZoneOption[]>(() => {
    const base = getTimeZoneOptions();
    if (allowCustomValue && !base.some((o) => o.value === allowCustomValue)) {
      return [{ value: allowCustomValue, label: allowCustomValue }, ...base];
    }
    return base;
  }, [allowCustomValue]);

  return (
    <Combobox
      id={id}
      options={options}
      value={value}
      onValueChange={onValueChange}
      placeholder={placeholder}
      className={error ? "aria-invalid:border-destructive" : undefined}
    />
  );
}
