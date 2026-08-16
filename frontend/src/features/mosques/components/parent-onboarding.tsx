import { Building2Icon, CheckCircle2Icon } from "lucide-react";
import { useEffect, useState } from "react";
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
import { parentStudentsApi } from "@/features/parent-students/api/parent-students-api.ts";

type ParentOnboardingProps = {
  onComplete: () => void;
};

export function ParentOnboarding({ onComplete }: ParentOnboardingProps) {
  const { t } = useTranslation("app");
  const [inviteCode, setInviteCode] = useState("");
  const [debouncedCode, setDebouncedCode] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [linked, setLinked] = useState(false);
  const [preview, setPreview] = useState<{ mosqueName: string; studentName: string } | null>(null);
  const [previewError, setPreviewError] = useState(false);
  const [previewLoading, setPreviewLoading] = useState(false);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      const code = inviteCode.trim();
      setDebouncedCode(code);
      if (code.length < 8) {
        setPreview(null);
        setPreviewError(false);
        setPreviewLoading(false);
      } else {
        setPreviewError(false);
        setPreviewLoading(true);
      }
    }, 400);
    return () => window.clearTimeout(timer);
  }, [inviteCode]);

  useEffect(() => {
    if (debouncedCode.length < 8) return;

    let cancelled = false;
    void parentStudentsApi
      .previewJoin(debouncedCode)
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
  }, [debouncedCode]);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (!inviteCode.trim()) {
      toast.error(t("onboarding.error"));
      return;
    }

    setIsSubmitting(true);
    try {
      await parentStudentsApi.linkByInviteCode({ inviteCode: inviteCode.trim() });
      toast.success(t("onboarding.success"));
      setLinked(true);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : t("onboarding.error"));
    } finally {
      setIsSubmitting(false);
    }
  }

  const isValid = debouncedCode.length >= 8 && !previewError && preview !== null;

  if (linked) {
    return (
      <Card className="overflow-hidden border-border/80 shadow-sm">
        <CardHeader className="border-b border-border/60 bg-card">
          <CardTitle className="font-serif text-2xl">
            {t("onboarding.parent.linkedTitle")}
          </CardTitle>
          <CardDescription>
            {t("onboarding.parent.linkedDescription")}
          </CardDescription>
        </CardHeader>
        <CardContent className="pt-6">
          <div className="flex flex-col gap-4">
            <div className="flex items-center gap-3 rounded-lg border border-primary/30 bg-primary/5 p-4">
              <CheckCircle2Icon className="shrink-0 text-primary" />
              <div className="flex flex-col gap-1">
                <p className="font-medium">{preview?.mosqueName}</p>
                <p className="text-sm text-muted-foreground">{preview?.studentName}</p>
              </div>
            </div>
            <Button type="button" onClick={onComplete}>
              {t("onboarding.parent.continue")}
            </Button>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="overflow-hidden border-border/80 shadow-sm">
      <CardHeader className="border-b border-border/60 bg-card">
        <CardTitle className="font-serif text-2xl">
          {t("onboarding.stepTitle")}
        </CardTitle>
        <CardDescription>
          {t("onboarding.parent.stepDescription")}
        </CardDescription>
      </CardHeader>
      <CardContent className="pt-6">
        <form className="flex flex-col gap-6" onSubmit={handleSubmit}>
          <FieldGroup>
            <Field data-invalid={previewError && debouncedCode.length >= 8}>
              <FieldLabel htmlFor="parent-invite-code">
                {t("onboarding.mosqueAdmin.inviteCode")}
              </FieldLabel>
              <Input
                id="parent-invite-code"
                aria-invalid={previewError && debouncedCode.length >= 8}
                placeholder={t("onboarding.parent.invitePlaceholder")}
                value={inviteCode}
                onChange={(e) => setInviteCode(e.target.value)}
              />
              <FieldDescription>
                {t("onboarding.parent.inviteHint")}
              </FieldDescription>
            </Field>
          </FieldGroup>

          {debouncedCode.length >= 8 ? (
            previewLoading ? (
              <div className="flex items-center gap-2 text-sm text-muted-foreground">
                <Spinner />
                {t("onboarding.parent.previewLoading")}
              </div>
            ) : previewError ? (
              <Alert variant="destructive">
                <AlertTitle>{t("onboarding.parent.invalidCode")}</AlertTitle>
              </Alert>
            ) : preview ? (
              <Alert>
                <Building2Icon />
                <AlertTitle>{t("onboarding.parent.joinPreview")}</AlertTitle>
                <AlertDescription>
                  {preview.mosqueName} &middot; {preview.studentName}
                </AlertDescription>
              </Alert>
            ) : null
          ) : null}

          <Button type="submit" disabled={!isValid || isSubmitting}>
            {isSubmitting ? <Spinner data-icon="inline-start" /> : null}
            {t("onboarding.parent.joinSubmit")}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}
