import { zodResolver } from "@hookform/resolvers/zod";
import { Building2Icon, CopyIcon, KeyRoundIcon } from "lucide-react";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert.tsx";
import { AddressText } from "@/components/address-text.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card.tsx";
import {
  Field,
  FieldDescription,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field.tsx";
import { Input } from "@/components/ui/input.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs.tsx";
import {
  applyFieldErrors,
  toMutationError,
} from "@/lib/errors/map-api-error.ts";
import { DEFAULT_TIME_ZONE } from "@/lib/timezones.ts";

import { MosqueFields } from "./mosque-fields.tsx";
import {
  mosqueCreateSchema,
  toMosqueCreateRequestBody,
  type MosqueCreateFormValues,
} from "../schemas/mosque-create.schema.ts";
import {
  mosqueJoinSchema,
  toMosqueJoinRequestBody,
  type MosqueJoinFormValues,
} from "../schemas/mosque-join.schema.ts";
import {
  useJoinMosque,
  useJoinPreview,
  useOnboardMosque,
} from "../hooks/use-mosques.ts";

type MosqueAdminOnboardingProps = {
  onComplete: (inviteCode?: string) => void;
};

export function MosqueAdminOnboarding({ onComplete }: MosqueAdminOnboardingProps) {
  const { t } = useTranslation("app");
  const onboardMosque = useOnboardMosque();
  const joinMosque = useJoinMosque();
  const [createdInviteCode, setCreatedInviteCode] = useState<string | null>(null);

  const createForm = useForm<MosqueCreateFormValues>({
    resolver: zodResolver(mosqueCreateSchema),
    defaultValues: {
      name: "",
      city: "",
      phone: "",
      email: "",
      logoUrl: "",
      timezone: DEFAULT_TIME_ZONE,
      addressCountry: "",
      addressPostalCode: "",
      addressStreet: "",
      addressHouseNumber: "",
      addressState: "",
    },
  });

  const joinForm = useForm<MosqueJoinFormValues>({
    resolver: zodResolver(mosqueJoinSchema),
    defaultValues: { inviteCode: "" },
  });

  const inviteCodeValue = joinForm.watch("inviteCode");
  const [debouncedCode, setDebouncedCode] = useState("");

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setDebouncedCode(inviteCodeValue.trim());
    }, 400);
    return () => window.clearTimeout(timer);
  }, [inviteCodeValue]);

  const joinPreview = useJoinPreview(debouncedCode, {
    enabled: debouncedCode.length >= 8,
  });

  async function handleCreateSubmit(values: MosqueCreateFormValues) {
    try {
      const result = await onboardMosque.mutateAsync(toMosqueCreateRequestBody(values));
      if (result.inviteCode) {
        setCreatedInviteCode(result.inviteCode);
        toast.success(t("onboarding.mosqueAdmin.createSuccess"));
        return;
      }
      onComplete();
    } catch (error) {
      const { message, fieldErrors } = toMutationError(error, t);
      if (fieldErrors) {
        applyFieldErrors(fieldErrors, createForm.setError, t);
      }
      toast.error(message);
    }
  }

  async function handleJoinSubmit(values: MosqueJoinFormValues) {
    try {
      await joinMosque.mutateAsync(toMosqueJoinRequestBody(values));
      toast.success(t("onboarding.success"));
      onComplete();
    } catch (error) {
      const { message, fieldErrors } = toMutationError(error, t);
      if (fieldErrors) {
        applyFieldErrors(fieldErrors, joinForm.setError, t);
      }
      toast.error(message);
    }
  }

  async function copyInviteCode() {
    if (!createdInviteCode) return;
    try {
      await navigator.clipboard.writeText(createdInviteCode);
      toast.success(t("onboarding.mosqueAdmin.inviteCodeCopied"));
    } catch {
      toast.error(t("onboarding.error"));
    }
  }

  if (createdInviteCode) {
    return (
      <Card className="overflow-hidden border-border/80 shadow-sm">
        <CardHeader className="brand-panel-pattern border-b border-border/60">
          <CardTitle className="font-serif text-2xl">
            {t("onboarding.mosqueAdmin.createSuccess")}
          </CardTitle>
          <CardDescription>{t("onboarding.mosqueAdmin.inviteCodeHint")}</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-6 pt-6">
          <Alert className="border-accent/40 bg-accent/10">
            <KeyRoundIcon />
            <AlertTitle>{t("onboarding.mosqueAdmin.inviteCode")}</AlertTitle>
            <AlertDescription className="font-mono text-base tracking-wide text-foreground">
              {createdInviteCode}
            </AlertDescription>
          </Alert>
          <div className="flex flex-col gap-3 sm:flex-row">
            <Button variant="outline" onClick={() => void copyInviteCode()}>
              <CopyIcon data-icon="inline-start" />
              {t("onboarding.mosqueAdmin.copyInviteCode")}
            </Button>
            <Button onClick={() => onComplete(createdInviteCode)}>
              {t("onboarding.mosqueAdmin.continueToDashboard")}
            </Button>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="overflow-hidden border-border/80 shadow-sm">
      <CardHeader className="brand-panel-pattern border-b border-border/60">
        <CardTitle className="font-serif text-2xl">{t("onboarding.stepTitle")}</CardTitle>
        <CardDescription>{t("onboarding.mosqueAdmin.stepDescription")}</CardDescription>
      </CardHeader>
      <CardContent className="pt-6">
        <Tabs defaultValue="create" className="flex flex-col gap-6">
          <TabsList className="grid w-full grid-cols-2">
            <TabsTrigger value="create">{t("onboarding.mosqueAdmin.createTab")}</TabsTrigger>
            <TabsTrigger value="join">{t("onboarding.mosqueAdmin.joinTab")}</TabsTrigger>
          </TabsList>

          <TabsContent value="create" className="flex flex-col gap-6">
            <form
              className="flex flex-col gap-6"
              onSubmit={(event) => {
                event.preventDefault();
                void createForm.handleSubmit(handleCreateSubmit)();
              }}
            >
              <MosqueFields
                register={createForm.register}
                errors={createForm.formState.errors}
                watch={createForm.watch}
                setValue={createForm.setValue}
                idPrefix="onboard-mosque"
              />
              <Button type="submit" disabled={onboardMosque.isPending}>
                {onboardMosque.isPending ? <Spinner data-icon="inline-start" /> : null}
                {t("onboarding.mosqueAdmin.createSubmit")}
              </Button>
            </form>
          </TabsContent>

          <TabsContent value="join" className="flex flex-col gap-6">
            <form
              className="flex flex-col gap-6"
              onSubmit={(event) => {
                event.preventDefault();
                void joinForm.handleSubmit(handleJoinSubmit)();
              }}
            >
              <FieldGroup>
                <Field data-invalid={!!joinForm.formState.errors.inviteCode}>
                  <FieldLabel htmlFor="onboard-invite-code">
                    {t("onboarding.mosqueAdmin.inviteCode")}
                  </FieldLabel>
                  <Input
                    id="onboard-invite-code"
                    aria-invalid={!!joinForm.formState.errors.inviteCode}
                    placeholder={t("onboarding.mosqueAdmin.inviteCodePlaceholder")}
                    {...joinForm.register("inviteCode")}
                  />
                  {joinForm.formState.errors.inviteCode ? (
                    <FieldDescription className="text-destructive">
                      {joinForm.formState.errors.inviteCode.message}
                    </FieldDescription>
                  ) : (
                    <FieldDescription>{t("onboarding.mosqueAdmin.inviteCodeHint")}</FieldDescription>
                  )}
                </Field>
              </FieldGroup>

              {debouncedCode.length >= 8 ? (
                joinPreview.isLoading ? (
                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <Spinner />
                    {t("onboarding.mosqueAdmin.previewLoading")}
                  </div>
                ) : joinPreview.isError ? (
                  <Alert variant="destructive">
                    <AlertTitle>{t("onboarding.mosqueAdmin.joinInvalidCode")}</AlertTitle>
                  </Alert>
                ) : joinPreview.data ? (
                  <Alert>
                    <Building2Icon />
                    <AlertTitle>{t("onboarding.mosqueAdmin.joinPreview")}</AlertTitle>
                    <AlertDescription>
                      <div className="flex flex-col gap-1">
                        {joinPreview.data.mosqueName}
                        <AddressText mosque={joinPreview.data} />
                      </div>
                    </AlertDescription>
                  </Alert>
                ) : null
              ) : null}

              <Button type="submit" disabled={joinMosque.isPending || !joinPreview.data}>
                {joinMosque.isPending ? <Spinner data-icon="inline-start" /> : null}
                {t("onboarding.mosqueAdmin.joinSubmit")}
              </Button>
            </form>
          </TabsContent>
        </Tabs>
      </CardContent>
    </Card>
  );
}
