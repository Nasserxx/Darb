import {
  ExternalLinkIcon,
  LandmarkIcon,
  PencilIcon,
  PlusIcon,
  RefreshCwIcon,
  Trash2Icon,
} from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, Navigate, useParams } from "react-router-dom";
import { toast } from "sonner";

import { ConfirmDeleteDialog } from "@/components/shared/confirm-delete-dialog.tsx";
import { AddressText } from "@/components/address-text.tsx";
import { DataTable } from "@/components/shared/data-table.tsx";
import { PageHeader } from "@/components/shared/page-header.tsx";
import { Badge } from "@/components/ui/badge.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Combobox } from "@/components/ui/combobox.tsx";
import {
  Empty,
  EmptyContent,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@/components/ui/empty.tsx";
import { Field, FieldLabel } from "@/components/ui/field.tsx";
import { Input } from "@/components/ui/input.tsx";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import { useWorkspace } from "@/features/workspace/context/workspace-provider.tsx";
import { DEFAULT_LOCALE } from "@/i18n/index.ts";
import { getCountryOptions } from "@/lib/countries.ts";
import { normalizeApiRole } from "@/lib/navigation/app-nav.ts";
import { usePagination } from "@/lib/hooks/use-pagination.ts";
import { toMutationError } from "@/lib/errors/map-api-error.ts";

import { CitySelect } from "./city-select.tsx";
import { MosqueFormDialog } from "./mosque-form-dialog.tsx";
import {
  useDeleteMosque,
  useMosques,
  useReactivateMosque,
} from "../hooks/use-mosques.ts";
import type { MosqueResponse } from "../types/index.ts";

export function MosquesList() {
  const { user } = useAuth();
  const role = user ? normalizeApiRole(user.role) : null;

  if (role === "SUPER_ADMIN") {
    return <SuperAdminMosquesTable />;
  }
  return <MosqueAdminRedirect />;
}

function MosqueFilterToolbar({
  q,
  country,
  city,
  setFilter,
}: {
  q: string | undefined;
  country: string | undefined;
  city: string | undefined;
  setFilter: (filter: { q?: string; country?: string; city?: string }) => void;
}) {
  const { t, i18n } = useTranslation("app");
  const [draftQ, setDraftQ] = useState(q ?? "");
  const [draftCountry, setDraftCountry] = useState(country ?? "");
  const [draftCity, setDraftCity] = useState(city ?? "");
  const countryOptions = useMemo(
    () => getCountryOptions(i18n.language),
    [i18n.language],
  );

  useEffect(() => {
    setDraftQ(q ?? "");
  }, [q]);

  useEffect(() => {
    setDraftCountry(country ?? "");
  }, [country]);

  useEffect(() => {
    setDraftCity(city ?? "");
  }, [city]);

  function handleSearch() {
    setFilter({
      q: draftQ.trim(),
      country: draftCountry,
      city: draftCity,
    });
  }

  function clearAll() {
    setDraftQ("");
    setDraftCountry("");
    setDraftCity("");
    setFilter({ q: "", country: "", city: "" });
  }

  const appliedQ = q ?? "";
  const appliedCountry = country ?? "";
  const appliedCity = city ?? "";
  const searchDisabled =
    draftQ.trim() === appliedQ.trim() &&
    draftCountry === appliedCountry &&
    draftCity === appliedCity;
  const hasFilters =
    Boolean(q || country || city) ||
    Boolean(draftQ || draftCountry || draftCity);

  return (
    <form
      className="flex flex-wrap items-end gap-3"
      onSubmit={(event) => {
        event.preventDefault();
        if (!searchDisabled) handleSearch();
      }}
    >
      <Field className="w-64">
        <FieldLabel htmlFor="mosque-search">{t("mosques.name")}</FieldLabel>
        <Input
          id="mosque-search"
          dir="auto"
          value={draftQ}
          onChange={(event) => setDraftQ(event.target.value)}
          placeholder={t("mosques.searchPlaceholder")}
        />
      </Field>
      <Field className="w-48">
        <FieldLabel>{t("mosques.country")}</FieldLabel>
        <Combobox
          options={countryOptions}
          value={draftCountry || null}
          onValueChange={(value) => {
            setDraftCountry(value ?? "");
            setDraftCity("");
          }}
          placeholder={t("mosques.allCountries")}
        />
      </Field>
      <Field className="w-48">
        <FieldLabel htmlFor="mosque-city-filter">{t("mosques.city")}</FieldLabel>
        <CitySelect
          id="mosque-city-filter"
          country={draftCountry || undefined}
          value={draftCity || null}
          onValueChange={(value) => setDraftCity(value ?? "")}
          mode="filter"
          activeOnly={false}
          placeholder={t("mosques.filterCity")}
        />
      </Field>
      <Button type="submit" variant="default" disabled={searchDisabled}>
        {t("superAdminScope.search")}
      </Button>
      {hasFilters ? (
        <Button type="button" variant="outline" onClick={clearAll}>
          {t("superAdminScope.clearAll")}
        </Button>
      ) : null}
    </form>
  );
}

function SuperAdminMosquesTable() {
  const { t } = useTranslation("app");
  const { locale } = useParams<{ locale: string }>();
  const localePrefix = locale ?? DEFAULT_LOCALE;
  const { params, setPage, setSize, q, country, city, setFilter } =
    usePagination();
  const { data, isLoading } = useMosques(params);
  const deleteMosque = useDeleteMosque();
  const reactivateMosque = useReactivateMosque();

  const [formOpen, setFormOpen] = useState(false);
  const [editingMosque, setEditingMosque] = useState<MosqueResponse | null>(null);
  const [deletingMosque, setDeletingMosque] = useState<MosqueResponse | null>(null);

  function openCreate() {
    setEditingMosque(null);
    setFormOpen(true);
  }

  function openEdit(mosque: MosqueResponse) {
    setEditingMosque(mosque);
    setFormOpen(true);
  }

  async function handleDeactivate() {
    if (!deletingMosque) return;
    try {
      await deleteMosque.mutateAsync(deletingMosque.id);
      toast.success(t("mosques.deactivateSuccess"));
      setDeletingMosque(null);
    } catch (error) {
      toast.error(toMutationError(error, t).message);
    }
  }

  async function handleReactivate(mosque: MosqueResponse) {
    try {
      await reactivateMosque.mutateAsync(mosque.id);
      toast.success(t("mosques.reactivateSuccess"));
    } catch (error) {
      toast.error(toMutationError(error, t).message);
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={t("mosques.title")}
        description={t("mosques.description")}
        actions={
          <Button onClick={openCreate}>
            <PlusIcon data-icon="inline-start" />
            {t("mosques.create")}
          </Button>
        }
      />

      <MosqueFilterToolbar
        q={q}
        country={country}
        city={city}
        setFilter={setFilter}
      />

      <DataTable
        columns={[
          {
            id: "name",
            header: t("mosques.name"),
            cell: (row) => row.name,
          },
          {
            id: "address",
            header: t("mosques.address"),
            cell: (row) => <AddressText mosque={row} />,
          },
          {
            id: "phone",
            header: t("mosques.phone"),
            cell: (row) => row.phone ?? "—",
          },
          {
            id: "status",
            header: t("mosques.status"),
            cell: (row) => (
              <Badge variant={row.isActive ? "default" : "secondary"}>
                {row.isActive ? t("mosques.active") : t("mosques.inactive")}
              </Badge>
            ),
          },
          {
            id: "actions",
            header: t("mosques.actions"),
            className: "w-36 text-end",
            cell: (row: MosqueResponse) => (
              <div className="flex justify-end gap-1">
                <Button variant="ghost" size="icon-sm" asChild>
                  <Link
                    to={`/${localePrefix}/mosques/${row.id}`}
                    aria-label={t("mosques.openDesk")}
                  >
                    <ExternalLinkIcon />
                  </Link>
                </Button>
                <Button
                  variant="ghost"
                  size="icon-sm"
                  onClick={() => openEdit(row)}
                  aria-label={t("actions.edit")}
                >
                  <PencilIcon />
                </Button>
                {row.isActive ? (
                  <Button
                    variant="ghost"
                    size="icon-sm"
                    onClick={() => setDeletingMosque(row)}
                    aria-label={t("mosques.deactivate")}
                  >
                    <Trash2Icon />
                  </Button>
                ) : (
                  <Button
                    variant="ghost"
                    size="icon-sm"
                    onClick={() => void handleReactivate(row)}
                    aria-label={t("mosques.reactivate")}
                  >
                    <RefreshCwIcon />
                  </Button>
                )}
              </div>
            ),
          },
        ]}
        data={data}
        isLoading={isLoading}
        emptyMessage={
          data && data.content.length === 0 && (q || country || city)
            ? t("mosques.emptyFiltered")
            : t("mosques.empty")
        }
        onPageChange={setPage}
        onSizeChange={setSize}
        pageSize={params.size}
      />

      <MosqueFormDialog
        open={formOpen}
        onOpenChange={setFormOpen}
        mosque={editingMosque}
      />

      <ConfirmDeleteDialog
        open={Boolean(deletingMosque)}
        onOpenChange={(open) => {
          if (!open) setDeletingMosque(null);
        }}
        title={t("mosques.deactivateTitle")}
        description={t("mosques.deactivateDescription")}
        onConfirm={() => void handleDeactivate()}
        isPending={deleteMosque.isPending}
      />
    </div>
  );
}

function MosqueAdminRedirect() {
  const { t } = useTranslation("app");
  const { mosqueId } = useWorkspace();
  const { locale } = useParams<{ locale: string }>();
  const localePrefix = locale ?? DEFAULT_LOCALE;

  if (!mosqueId) {
    return (
      <div className="flex flex-col gap-6">
        <PageHeader
          title={t("mosques.title")}
          description={t("mosques.description")}
        />
        <Empty>
          <EmptyMedia variant="icon">
            <LandmarkIcon />
          </EmptyMedia>
          <EmptyHeader>
            <EmptyTitle>{t("mosques.needOnboardingTitle")}</EmptyTitle>
          </EmptyHeader>
          <EmptyContent>
            <EmptyDescription>{t("mosques.needOnboarding")}</EmptyDescription>
          </EmptyContent>
        </Empty>
      </div>
    );
  }

  return <Navigate to={`/${localePrefix}/mosques/${mosqueId}`} replace />;
}
