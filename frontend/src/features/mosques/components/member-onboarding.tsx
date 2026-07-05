import { zodResolver } from "@hookform/resolvers/zod";
import { Building2Icon, ClockIcon, SearchIcon } from "lucide-react";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert.tsx";
import { Button } from "@/components/ui/button.tsx";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card.tsx";
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
import type { MosqueSearchResult } from "@/features/mosques/types/onboard.ts";
import { studentsApi } from "@/features/students/api/students-api.ts";
import { teachersApi } from "@/features/teachers/api/teachers-api.ts";
import { useWorkspace } from "@/features/workspace/context/workspace-provider.tsx";
import {
  applyFieldErrors,
  toMutationError,
} from "@/lib/errors/map-api-error.ts";
import { consumeJoinIntent } from "@/lib/navigation/post-auth.ts";

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
  const { t } = useTranslation("app");
  const { refreshProfile } = useWorkspace();
  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState<MosqueSearchResult[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [isSubmittingRequest, setIsSubmittingRequest] = useState(false);
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

  useEffect(() => {
    const intent = consumeJoinIntent();
    if (intent?.code) {
      joinForm.setValue("inviteCode", intent.code);
      setDebouncedCode(intent.code);
    }
  }, [joinForm]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setDebouncedCode(inviteCodeValue.trim());
    }, 400);
    return () => window.clearTimeout(timer);
  }, [inviteCodeValue]);

  const [preview, setPreview] = useState<{ mosqueName: string } | null>(null);
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

  async function handleSearch() {
    setIsSearching(true);
    try {
      const results = await mosquesApi.search(searchQuery.trim());
      setSearchResults(results);
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
                    <AlertDescription>{preview.mosqueName}</AlertDescription>
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
            <FieldGroup>
              <Field>
                <FieldLabel htmlFor="mosque-search">{t("onboarding.member.searchLabel")}</FieldLabel>
                <div className="flex flex-col gap-3 sm:flex-row">
                  <Input
                    id="mosque-search"
                    value={searchQuery}
                    onChange={(event) => setSearchQuery(event.target.value)}
                    placeholder={t("onboarding.member.searchPlaceholder")}
                  />
                  <Button
                    type="button"
                    variant="secondary"
                    disabled={isSearching || !searchQuery.trim()}
                    onClick={() => void handleSearch()}
                  >
                    <SearchIcon data-icon="inline-start" />
                    {t("onboarding.member.searchButton")}
                  </Button>
                </div>
              </Field>
            </FieldGroup>

            <div className="flex flex-col gap-3">
              {searchResults.map((mosque) => (
                <Card key={mosque.id} className="border-border/70">
                  <CardContent className="flex flex-col gap-3 pt-6 sm:flex-row sm:items-center sm:justify-between">
                    <div className="flex flex-col gap-1">
                      <p className="font-medium">{mosque.name}</p>
                      <p className="text-sm text-muted-foreground">{mosque.city}</p>
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
              {!isSearching && searchResults.length === 0 && searchQuery ? (
                <p className="text-sm text-muted-foreground">
                  {t("onboarding.member.noResults")}
                </p>
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
            </Alert>
          </TabsContent>
        </Tabs>
      </CardContent>
    </Card>
  );
}
