import { useId, useMemo, useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";

import { Button } from "@/components/ui/button.tsx";
import { Combobox, type ComboboxOption } from "@/components/ui/combobox.tsx";
import {
  Field,
  FieldDescription,
  FieldLabel,
} from "@/components/ui/field.tsx";
import { Input } from "@/components/ui/input.tsx";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet.tsx";
import {
  usePickerCities,
  usePickerStates,
  useUserPicker,
} from "@/features/users/hooks/use-users.ts";
import type {
  UserPickerOccupancyRole,
  UserPickerParams,
  UserPickerResponse,
} from "@/features/users/types/index.ts";
import { getCountryOptions, localizeCountry } from "@/lib/countries.ts";
import { formatShortId } from "@/lib/format/ids.ts";
import { cn } from "@/lib/utils.ts";

const PICKER_PAGE_SIZE = 20;

type PickerFilters = {
  q: string;
  country: string | null;
  state: string | null;
  city: string | null;
  dateOfBirth: string;
};

const EMPTY_FILTERS: PickerFilters = {
  q: "",
  country: null,
  state: null,
  city: null,
  dateOfBirth: "",
};

function visiblePageNumbers(currentPage: number, totalPages: number): number[] {
  if (totalPages <= 0) return [];
  if (totalPages <= 7) {
    return Array.from({ length: totalPages }, (_, i) => i + 1);
  }
  const current = currentPage + 1;
  const pages = new Set<number>([1, totalPages]);
  for (let i = current - 1; i <= current + 1; i++) {
    if (i >= 1 && i <= totalPages) pages.add(i);
  }
  return [...pages].sort((a, b) => a - b);
}

function locationLine(row: UserPickerResponse): string {
  return [row.city, row.addressState, row.addressCountry]
    .filter((part) => part && part.trim())
    .join(", ");
}

function hasValidCriterion(filters: PickerFilters): boolean {
  return (
    filters.q.trim().length >= 2 ||
    Boolean(filters.country) ||
    Boolean(filters.dateOfBirth)
  );
}

function filtersEqual(a: PickerFilters, b: PickerFilters): boolean {
  return (
    a.q === b.q &&
    a.country === b.country &&
    a.state === b.state &&
    a.city === b.city &&
    a.dateOfBirth === b.dateOfBirth
  );
}

function committedToParams(
  committed: PickerFilters | null,
  page: number,
  occupancyMosqueId?: string,
  occupancyRole?: UserPickerOccupancyRole,
): UserPickerParams {
  const occupancy =
    occupancyMosqueId && occupancyRole
      ? { mosqueId: occupancyMosqueId, role: occupancyRole }
      : {};
  if (!committed) {
    return {
      page,
      size: PICKER_PAGE_SIZE,
      ...occupancy,
    };
  }
  const q = committed.q.trim();
  return {
    q: q.length >= 2 ? q : undefined,
    country: committed.country ?? undefined,
    state: committed.state ?? undefined,
    city: committed.city ?? undefined,
    dateOfBirth: committed.dateOfBirth || undefined,
    page,
    size: PICKER_PAGE_SIZE,
    ...occupancy,
  };
}

export function UserSearchSelect({
  value,
  onValueChange,
  placeholder,
  className,
  occupancyMosqueId,
  occupancyRole,
}: {
  value: string;
  onValueChange: (userId: string) => void;
  placeholder?: string;
  className?: string;
  occupancyMosqueId?: string;
  occupancyRole?: UserPickerOccupancyRole;
}) {
  const { t, i18n } = useTranslation("app");
  const fieldId = useId();
  const nameId = `${fieldId}-name`;
  const dobId = `${fieldId}-dob`;
  const countryId = `${fieldId}-country`;
  const stateId = `${fieldId}-state`;
  const cityId = `${fieldId}-city`;
  const sheetSide = i18n.dir() === "rtl" ? "left" : "right";
  const [open, setOpen] = useState(false);
  const [draft, setDraft] = useState<PickerFilters>(EMPTY_FILTERS);
  const [committed, setCommitted] = useState<PickerFilters | null>(null);
  const [hasSearched, setHasSearched] = useState(false);
  const [showNeedCriterion, setShowNeedCriterion] = useState(false);
  const [page, setPage] = useState(0);
  const [picked, setPicked] = useState<UserPickerResponse | null>(null);

  const countryOptions = useMemo(
    () => getCountryOptions(i18n.language),
    [i18n.language],
  );

  const {
    data: states = [],
    isPending: statesPending,
    isFetching: statesFetching,
    isFetched: statesFetched,
  } = usePickerStates(draft.country ?? undefined);
  const {
    data: cities = [],
    isPending: citiesPending,
    isFetching: citiesFetching,
    isFetched: citiesFetched,
  } = usePickerCities(draft.country ?? undefined, draft.state ?? undefined);

  const countryLabel =
    localizeCountry(draft.country, i18n.language) ?? draft.country ?? "";

  const pickerParams = committedToParams(
    committed,
    page,
    occupancyMosqueId,
    occupancyRole,
  );
  const { data: pageData, isFetching } = useUserPicker(pickerParams, {
    enabled: open && hasSearched,
  });

  const stateOptions: ComboboxOption[] = states.map((item) => ({
    value: item,
    label: item,
  }));
  const cityOptions: ComboboxOption[] = cities.map((item) => ({
    value: item,
    label: item,
  }));

  const selectedLabel =
    picked && picked.id === value
      ? picked.fullName
      : value
        ? formatShortId(value)
        : null;

  const draftDirty =
    hasSearched && committed !== null && !filtersEqual(draft, committed);

  function handleCountryChange(next: string | null) {
    setDraft((current) => ({
      ...current,
      country: next,
      state: null,
      city: null,
    }));
  }

  function handleStateChange(next: string | null) {
    setDraft((current) => ({ ...current, state: next, city: null }));
  }

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!hasValidCriterion(draft)) {
      setShowNeedCriterion(true);
      return;
    }
    setShowNeedCriterion(false);
    setCommitted({ ...draft });
    setPage(0);
    setHasSearched(true);
  }

  function handleClearAll() {
    setDraft(EMPTY_FILTERS);
    setCommitted(null);
    setHasSearched(false);
    setShowNeedCriterion(false);
    setPage(0);
  }

  function handlePick(row: UserPickerResponse) {
    setPicked(row);
    onValueChange(row.id);
    setOpen(false);
  }

  const rows = pageData?.content ?? [];
  const totalPages = pageData?.totalPages ?? 0;
  const pages = visiblePageNumbers(pageData?.pageNumber ?? 0, totalPages);
  const clearValue = t("users.picker.clearValue");

  return (
    <div className={cn("flex flex-col gap-2", className)}>
      <Button
        type="button"
        variant="outline"
        className="w-full justify-start font-normal"
        onClick={() => setOpen(true)}
      >
        {selectedLabel ?? placeholder ?? t("users.picker.choose")}
      </Button>

      <Sheet open={open} onOpenChange={setOpen}>
        <SheetContent
          side={sheetSide}
          className="flex w-full flex-col sm:max-w-lg"
        >
          <SheetHeader>
            <SheetTitle>{t("users.picker.title")}</SheetTitle>
            <SheetDescription>{t("users.picker.description")}</SheetDescription>
          </SheetHeader>

          <form
            onSubmit={handleSearch}
            className="flex min-h-0 flex-1 flex-col gap-3 overflow-y-auto px-4 pb-4"
          >
            <Field>
              <FieldLabel htmlFor={nameId}>
                {t("users.picker.nameLabel")}
              </FieldLabel>
              <Input
                id={nameId}
                value={draft.q}
                onChange={(event) =>
                  setDraft((current) => ({ ...current, q: event.target.value }))
                }
                placeholder={t("users.picker.namePlaceholder")}
              />
              <FieldDescription>{t("users.picker.nameHint")}</FieldDescription>
            </Field>

            <Field>
              <FieldLabel htmlFor={dobId}>
                {t("users.picker.dateOfBirth")}
              </FieldLabel>
              <div className="flex gap-2">
                <Input
                  id={dobId}
                  type="date"
                  value={draft.dateOfBirth}
                  onChange={(event) =>
                    setDraft((current) => ({
                      ...current,
                      dateOfBirth: event.target.value,
                    }))
                  }
                />
                <Button
                  type="button"
                  variant="ghost"
                  onClick={() =>
                    setDraft((current) => ({ ...current, dateOfBirth: "" }))
                  }
                >
                  {clearValue}
                </Button>
              </div>
              <FieldDescription>
                {t("users.picker.dateOfBirthHint")}
              </FieldDescription>
            </Field>

            <Field>
              <FieldLabel htmlFor={countryId}>
                {t("users.picker.country")}
              </FieldLabel>
              <Combobox
                id={countryId}
                options={countryOptions}
                value={draft.country}
                onValueChange={handleCountryChange}
                placeholder={t("users.picker.countrySearch")}
                searchPlaceholder={t("users.picker.countrySearch")}
                emptyLabel={t("users.noResults")}
                clearAriaLabel={clearValue}
              />
              <FieldDescription>
                {t("users.picker.countryHint")}
              </FieldDescription>
            </Field>

            <Field>
              <FieldLabel htmlFor={stateId}>
                {t("users.picker.state")}
              </FieldLabel>
              <Combobox
                id={stateId}
                options={stateOptions}
                value={draft.state}
                onValueChange={handleStateChange}
                placeholder={t("users.picker.stateSearch")}
                disabled={!draft.country}
                searchPlaceholder={t("users.picker.stateSearch")}
                emptyLabel={
                  draft.country
                    ? t("users.picker.noStates", { country: countryLabel })
                    : t("users.picker.stateNeedCountry")
                }
                clearAriaLabel={clearValue}
              />
              {!draft.country ? (
                <FieldDescription>
                  {t("users.picker.stateNeedCountry")}
                </FieldDescription>
              ) : statesFetched &&
                !statesPending &&
                !statesFetching &&
                states.length === 0 ? (
                <FieldDescription>
                  {t("users.picker.noStates", { country: countryLabel })}
                </FieldDescription>
              ) : null}
            </Field>

            <Field>
              <FieldLabel htmlFor={cityId}>
                {t("users.picker.city")}
              </FieldLabel>
              <Combobox
                id={cityId}
                options={cityOptions}
                value={draft.city}
                onValueChange={(next) =>
                  setDraft((current) => ({ ...current, city: next }))
                }
                placeholder={t("users.picker.citySearch")}
                disabled={!draft.country}
                searchPlaceholder={t("users.picker.citySearch")}
                emptyLabel={
                  draft.country
                    ? draft.state
                      ? t("users.picker.noCitiesInState", {
                          country: countryLabel,
                        })
                      : t("users.picker.noCities", { country: countryLabel })
                    : t("users.picker.cityNeedCountry")
                }
                clearAriaLabel={clearValue}
              />
              {!draft.country ? (
                <FieldDescription>
                  {t("users.picker.cityNeedCountry")}
                </FieldDescription>
              ) : citiesFetched &&
                !citiesPending &&
                !citiesFetching &&
                cities.length === 0 ? (
                <FieldDescription>
                  {draft.state
                    ? t("users.picker.noCitiesInState", {
                        country: countryLabel,
                      })
                    : t("users.picker.noCities", { country: countryLabel })}
                </FieldDescription>
              ) : null}
            </Field>

            <div className="flex flex-wrap gap-2">
              <Button type="submit" variant="default">
                {t("users.picker.search")}
              </Button>
              <Button type="button" variant="ghost" onClick={handleClearAll}>
                {t("users.picker.clearFilters")}
              </Button>
            </div>

            {showNeedCriterion ? (
              <p className="text-sm text-destructive">
                {t("users.picker.needCriterion")}
              </p>
            ) : null}
            {draftDirty ? (
              <p className="text-sm text-muted-foreground">
                {t("users.picker.resultsStale")}
              </p>
            ) : null}
            {isFetching && hasSearched ? (
              <p className="text-sm text-muted-foreground">
                {t("users.searching")}
              </p>
            ) : null}
            {!hasSearched && !isFetching && !showNeedCriterion ? (
              <p className="text-sm text-muted-foreground">
                {t("users.picker.idle")}
              </p>
            ) : null}
            {hasSearched && !isFetching && rows.length === 0 ? (
              <p className="text-sm text-muted-foreground">
                {t("users.picker.empty")}
              </p>
            ) : null}

            <ul className="flex flex-col gap-1">
              {rows.map((row) => (
                <li key={row.id}>
                  <button
                    type="button"
                    className="w-full rounded-lg border border-border px-3 py-2 text-start hover:bg-muted"
                    onClick={() => handlePick(row)}
                  >
                    <span className="block font-medium">{row.fullName}</span>
                    <span className="block text-xs text-muted-foreground">
                      {[locationLine(row), row.dateOfBirth]
                        .filter(Boolean)
                        .join(" · ")}
                    </span>
                  </button>
                </li>
              ))}
            </ul>

            {totalPages > 1 && pageData ? (
              <div className="flex flex-wrap items-center justify-between gap-2">
                <p className="text-xs text-muted-foreground">
                  {t("table.pageInfo", {
                    page: pageData.pageNumber + 1,
                    total: totalPages,
                    count: pageData.totalElements,
                  })}
                </p>
                <div className="flex flex-wrap items-center gap-1">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    disabled={draftDirty || pageData.pageNumber <= 0}
                    onClick={() => setPage(pageData.pageNumber - 1)}
                  >
                    {t("table.previous")}
                  </Button>
                  {pages.map((pageNumber) => (
                    <Button
                      key={pageNumber}
                      type="button"
                      variant={
                        pageNumber === pageData.pageNumber + 1
                          ? "default"
                          : "outline"
                      }
                      size="sm"
                      className="min-w-8 px-2"
                      disabled={draftDirty}
                      onClick={() => setPage(pageNumber - 1)}
                    >
                      {pageNumber}
                    </Button>
                  ))}
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    disabled={draftDirty || pageData.last}
                    onClick={() => setPage(pageData.pageNumber + 1)}
                  >
                    {t("table.next")}
                  </Button>
                </div>
              </div>
            ) : null}
          </form>
        </SheetContent>
      </Sheet>
    </div>
  );
}
