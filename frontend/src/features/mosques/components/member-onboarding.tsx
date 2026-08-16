import { zodResolver } from "@hookform/resolvers/zod";
import { Building2Icon, ClockIcon, SearchIcon, XIcon } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert.tsx";
import { AddressText } from "@/components/address-text.tsx";
import { Button } from "@/components/ui/button.tsx";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card.tsx";
import { Combobox } from "@/components/ui/combobox.tsx";
import {
  Field,
  FieldDescription,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field.tsx";
import { Input } from "@/components/ui/input.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs.tsx";
import { mosquesApi } from "@/features/mosques/api/mosques-api.ts";
import {
  mosqueJoinSchema,
  type MosqueJoinFormValues,
} from "@/features/mosques/schemas/mosque-join.schema.ts";
import type {
  MosqueJoinPreviewResponse,
  MosqueSearchResult,
} from "@/features/mosques/types/onboard.ts";
import { studentsApi } from "@/features/students/api/students-api.ts";
import { teachersApi } from "@/features/teachers/api/teachers-api.ts";
import { workspaceApi } from "@/features/workspace/api/workspace-api.ts";
import { useWorkspace } from "@/features/workspace/context/workspace-provider.tsx";
import { getCountryOptions } from "@/lib/countries.ts";
import {
  applyFieldErrors,
  toMutationError,
} from "@/lib/errors/map-api-error.ts";
import {
  consumeJoinIntent,
  deriveProfileStatus,
} from "@/lib/navigation/post-auth.ts";
import type { PageResponse } from "@/lib/types/api.ts";

type MemberOnboardingProps = {
  role: "TEACHER" | "STUDENT";
  initialCode?: string;
  isPending?: boolean;
  pendingMosqueName?: string | null;
  onComplete: () => void;
};

export function MemberOnboarding({
  role,
  initialCode,
  isPending,
  pendingMosqueName,
  onComplete,
}: MemberOnboardingProps) {
  const { t, i18n } = useTranslation("app");
  const { refreshProfile } = useWorkspace();
  const [q, setQ] = useState("");
  const [country, setCountry] = useState<string | null>(null);
  const [city, setCity] = useState("");
  const [page, setPage] = useState(0);
  const [results, setResults] = useState<PageResponse<MosqueSearchResult> | null>(
    null,
  );
  const [isSearching, setIsSearching] = useState(false);
  const [isSubmittingRequest, setIsSubmittingRequest] = useState(false);
  const [isCancelling, setIsCancelling] = useState(false);
  const [defaultTab, setDefaultTab] = useState(
    isPending ? "pending" : "invite",
  );

  const joinForm = useForm<MosqueJoinFormValues>({
    resolver: zodResolver(mosqueJoinSchema),
    defaultValues: { inviteCode: initialCode ?? "" },
  });

  const inviteCodeValue = joinForm.watch("inviteCode");
  const [debouncedCode, setDebouncedCode] = useState(initialCode?.trim() ?? "");
  const memberRole = role === "TEACHER" ? "teacher" : "student";
  const countryOptions = useMemo(
    () => getCountryOptions(i18n.language),
    [i18n.language],
  );

  useEffect(() => {
    const intent = consumeJoinIntent();
    if (intent && intent.role === role && intent.code) {
      joinForm.setValue("inviteCode", intent.code);
      setDebouncedCode(intent.code);
    }
  }, [joinForm, role]);

  useEffect(() => {
    if (!isPending) return;

    let active = true;
    const checkApproval = async () => {
      try {
        const profile = await workspaceApi.getProfile();
        if (active && deriveProfileStatus(profile, false) === "assigned") {
          onComplete();
        }
      } catch {
        // transient errors are ignored; the next poll retries
      }
    };

    const interval = window.setInterval(() => void checkApproval(), 10_000);
    return () => {
      active = false;
      window.clearInterval(interval);
    };
  }, [isPending, onComplete]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setDebouncedCode(inviteCodeValue.trim());
    }, 400);
    return () => window.clearTimeout(timer);
  }, [inviteCodeValue]);

  const [preview, setPreview] = useState<MosqueJoinPreviewResponse | null>(null);
  const [previewError, setPreviewError] = useState(false);
  const [previewLoading, setPreviewLoading] = useState(false);

  useEffect(() => {
    if (debouncedCode.length < 8) {
      setPreview(null);
      setPreviewError(false);
      return;
    }

    let cancelled = false;
    setPreviewLoading(true);
    setPreviewError(false);

    void mosquesApi
      .previewMemberJoin(debouncedCode, memberRole)
      .then((data) => {
        if (!cancelled) {
          setPreview(data);
          setPreviewError(false);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setPreview(null);
          setPreviewError(true);
        }
      })
      .finally(() => {
        if (!cancelled) setPreviewLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [debouncedCode, memberRole]);

  async function handleJoinSubmit(values: MosqueJoinFormValues) {
    try {
      const body = { inviteCode: values.inviteCode.trim() };
      if (role === "TEACHER") {
        await teachersApi.join(body);
      } else {
        await studentsApi.join(body);
      }
      toast.success(t("onboarding.success"));
      await refreshProfile();
      onComplete();
    } catch (error) {
      const { message, fieldErrors } = toMutationError(error, t);
      if (fieldErrors) {
        applyFieldErrors(fieldErrors, joinForm.setError, t);
      }
      toast.error(message);
    }
  }

  async function handleSearch(nextPage = 0) {
    setIsSearching(true);
    setPage(nextPage);
    try {
      const data = await mosquesApi.search({
        q: q.trim(),
        country: country ?? undefined,
        city: city.trim(),
        page: nextPage,
      });
      setResults(data);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : t("onboarding.error"));
    } finally {
      setIsSearching(false);
    }
  }

  async function handleJoinRequest(mosqueId: string) {
    setIsSubmittingRequest(true);
    try {
      await mosquesApi.createJoinRequest(mosqueId);
      toast.success(t("onboarding.member.requestSent"));
      await refreshProfile();
      setDefaultTab("pending");
    } catch (error) {
      toast.error(error instanceof Error ? error.message : t("onboarding.error"));
    } finally {
      setIsSubmittingRequest(false);
    }
  }

  async function handleCancelRequest() {
    setIsCancelling(true);
    try {
      await mosquesApi.cancelMyJoinRequest();
      toast.success(t("onboarding.member.cancelledRequest"));
      await refreshProfile();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : t("onboarding.error"));
    } finally {
      setIsCancelling(false);
    }
  }

  const rows = results?.content ?? [];
  const hasActiveFilters = Boolean(q.trim() || country || city.trim());

  return (
    <Card className="overflow-hidden border-border/80 shadow-sm">
      <CardHeader className="border-b border-border/60 bg-card">
        <CardTitle className="font-serif text-2xl">{t("onboarding.stepTitle")}</CardTitle>
        <CardDescription>{t("onboarding.member.stepDescription")}</CardDescription>
      </CardHeader>
      <CardContent className="pt-6">
        <Tabs value={defaultTab} onValueChange={setDefaultTab} className="flex flex-col gap-6">
          <TabsList className="grid w-full grid-cols-3">
            <TabsTrigger value="invite">{t("onboarding.member.inviteTab")}</TabsTrigger>
            <TabsTrigger value="search">{t("onboarding.member.searchTab")}</TabsTrigger>
            <TabsTrigger value="pending">{t("onboarding.member.pendingTab")}</TabsTrigger>
          </TabsList>

          <TabsContent value="invite" className="flex flex-col gap-6">
            <form
              className="flex flex-col gap-6"
              onSubmit={(event) => {
                event.preventDefault();
                void joinForm.handleSubmit(handleJoinSubmit)();
              }}
            >
              <FieldGroup>
                <Field data-invalid={!!joinForm.formState.errors.inviteCode}>
                  <FieldLabel htmlFor="member-invite-code">
                    {t("onboarding.mosqueAdmin.inviteCode")}
                  </FieldLabel>
                  <Input
                    id="member-invite-code"
                    aria-invalid={!!joinForm.formState.errors.inviteCode}
                    placeholder={t("onboarding.member.invitePlaceholder")}
                    {...joinForm.register("inviteCode")}
                  />
                  {joinForm.formState.errors.inviteCode ? (
                    <FieldDescription className="text-destructive">
                      {joinForm.formState.errors.inviteCode.message}
                    </FieldDescription>
                  ) : (
                    <FieldDescription>{t("onboarding.member.inviteHint")}</FieldDescription>
                  )}
                </Field>
              </FieldGroup>

              {debouncedCode.length >= 8 ? (
                previewLoading ? (
                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <Spinner />
                    {t("onboarding.mosqueAdmin.previewLoading")}
                  </div>
                ) : previewError ? (
                  <Alert variant="destructive">
                    <AlertTitle>{t("onboarding.mosqueAdmin.joinInvalidCode")}</AlertTitle>
                  </Alert>
                ) : preview ? (
                  <Alert>
                    <Building2Icon />
                    <AlertTitle>{t("onboarding.mosqueAdmin.joinPreview")}</AlertTitle>
                    <AlertDescription>
                      <div className="flex flex-col gap-1">
                        {preview.mosqueName}
                        <AddressText mosque={preview} />
                      </div>
                    </AlertDescription>
                  </Alert>
                ) : null
              ) : null}

              <Button type="submit" disabled={!preview || joinForm.formState.isSubmitting}>
                {joinForm.formState.isSubmitting ? (
                  <Spinner data-icon="inline-start" />
                ) : null}
                {t("onboarding.member.joinSubmit")}
              </Button>
            </form>
          </TabsContent>

          <TabsContent value="search" className="flex flex-col gap-6">
            <form
              className="flex flex-col gap-6"
              onSubmit={(event) => {
                event.preventDefault();
                void handleSearch();
              }}
            >
              <FieldGroup>
                <div className="flex flex-wrap items-end gap-3">
                  <Field className="w-64">
                    <FieldLabel htmlFor="mosque-search">
                      {t("onboarding.member.searchLabel")}
                    </FieldLabel>
                    <Input
                      id="mosque-search"
                      value={q}
                      onChange={(event) => setQ(event.target.value)}
                      placeholder={t("onboarding.member.searchPlaceholder")}
                    />
                  </Field>
                  <Field className="w-48">
                    <FieldLabel>{t("mosques.country")}</FieldLabel>
                    <Combobox
                      options={countryOptions}
                      value={country}
                      onValueChange={setCountry}
                      placeholder={t("mosques.allCountries")}
                    />
                  </Field>
                  <Field className="w-48">
                    <FieldLabel htmlFor="mosque-city-search">
                      {t("mosques.city")}
                    </FieldLabel>
                    <Input
                      id="mosque-city-search"
                      value={city}
                      onChange={(event) => setCity(event.target.value)}
                      placeholder={t("onboarding.member.cityPlaceholder")}
                    />
                  </Field>
                  <Button type="submit" variant="secondary" disabled={isSearching}>
                    <SearchIcon data-icon="inline-start" />
                    {t("onboarding.member.searchButton")}
                  </Button>
                  {hasActiveFilters ? (
                    <Button
                      type="button"
                      variant="outline"
                      onClick={() => {
                        setQ("");
                        setCountry(null);
                        setCity("");
                        void handleSearch(0);
                      }}
                    >
                      {t("mosques.clearFilters")}
                    </Button>
                  ) : null}
                </div>
              </FieldGroup>
            </form>

            <div className="flex flex-col gap-3">
              {rows.map((mosque) => (
                <Card key={mosque.id} className="border-border/70">
                  <CardContent className="flex flex-col gap-3 pt-6 sm:flex-row sm:items-center sm:justify-between">
                    <div className="flex flex-col gap-1">
                      <p className="font-medium">{mosque.name}</p>
                      <AddressText
                        mosque={mosque}
                        className="text-sm text-muted-foreground"
                      />
                    </div>
                    <Button
                      size="sm"
                      disabled={isSubmittingRequest}
                      onClick={() => void handleJoinRequest(mosque.id)}
                    >
                      {t("onboarding.member.requestJoin")}
                    </Button>
                  </CardContent>
                </Card>
              ))}
              {!isSearching && results !== null && rows.length === 0 ? (
                <p className="text-sm text-muted-foreground">
                  {t("onboarding.member.noResults")}
                </p>
              ) : null}
              {results && results.totalPages > 1 ? (
                <div className="flex items-center justify-between gap-4">
                  <p className="text-sm text-muted-foreground">
                    {t("table.pageInfo", {
                      page: page + 1,
                      total: results.totalPages,
                      count: results.totalElements,
                    })}
                  </p>
                  <div className="flex items-center gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={page <= 0}
                      onClick={() => void handleSearch(page - 1)}
                    >
                      {t("table.previous")}
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={results.last}
                      onClick={() => void handleSearch(page + 1)}
                    >
                      {t("table.next")}
                    </Button>
                  </div>
                </div>
              ) : null}
            </div>
          </TabsContent>

          <TabsContent value="pending">
            <Alert>
              <ClockIcon />
              <AlertTitle>{t("onboarding.member.pendingTitle")}</AlertTitle>
              <AlertDescription>
                {pendingMosqueName
                  ? t("onboarding.member.pendingDescription", {
                      mosque: pendingMosqueName,
                    })
                  : t("onboarding.member.pendingDescriptionGeneric")}
              </AlertDescription>
              <div className="mt-4">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={isCancelling}
                  onClick={() => void handleCancelRequest()}
                >
                  {isCancelling ? (
                    <Spinner data-icon="inline-start" />
                  ) : (
                    <XIcon data-icon="inline-start" />
                  )}
                  {t("onboarding.member.cancelRequest")}
                </Button>
              </div>
            </Alert>
          </TabsContent>
        </Tabs>
      </CardContent>
    </Card>
  );
}
