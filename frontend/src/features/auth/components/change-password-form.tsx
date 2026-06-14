import { zodResolver } from "@hookform/resolvers/zod";
import { EyeIcon, EyeOffIcon } from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";
import { toast } from "sonner";

import { Button } from "@/components/ui/button.tsx";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
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
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import {
  changePasswordSchema,
  type ChangePasswordFormValues,
} from "@/features/auth/schemas/change-password.schema.ts";
import {
  applyFieldErrors,
  translateAuthApiMessage,
} from "@/features/auth/utils/form-errors.ts";
import { DEFAULT_LOCALE } from "@/i18n/index.ts";

export function ChangePasswordForm() {
  const { t } = useTranslation("auth");
  const { changePassword, isLoading } = useAuth();
  const { locale } = useParams<{ locale: string }>();
  const localePrefix = locale ?? DEFAULT_LOCALE;
  const [showCurrent, setShowCurrent] = useState(false);
  const [showNew, setShowNew] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<ChangePasswordFormValues>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: {
      currentPassword: "",
      newPassword: "",
    },
  });

  async function onSubmit(values: ChangePasswordFormValues) {
    const result = await changePassword(values);
    if (result.ok) {
      toast.success(t("changePassword.success"));
      reset();
      return;
    }

    if (result.fieldErrors) {
      applyFieldErrors(result.fieldErrors, setError, t);
    }
    toast.error(translateAuthApiMessage(result.message, t));
  }

  return (
    <div className="mx-auto flex w-full max-w-lg flex-col gap-6 p-6 sm:p-10">
      <Card>
        <CardHeader>
          <CardTitle>{t("changePassword.title")}</CardTitle>
          <CardDescription>{t("changePassword.subtitle")}</CardDescription>
        </CardHeader>
        <CardContent>
          <form
            onSubmit={(event) => void handleSubmit(onSubmit)(event)}
            noValidate
            className="flex flex-col gap-6"
          >
            <FieldGroup>
              <Field data-invalid={!!errors.currentPassword}>
                <FieldLabel htmlFor="current-password">
                  {t("changePassword.currentPassword")}
                </FieldLabel>
                <div className="relative">
                  <Input
                    id="current-password"
                    type={showCurrent ? "text" : "password"}
                    autoComplete="current-password"
                    placeholder={t("changePassword.currentPasswordPlaceholder")}
                    className="pe-10"
                    aria-invalid={!!errors.currentPassword}
                    {...register("currentPassword")}
                  />
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    className="absolute end-0 top-0 text-muted-foreground hover:text-foreground"
                    onClick={() => setShowCurrent((visible) => !visible)}
                    aria-label={
                      showCurrent
                        ? t("common:hidePassword")
                        : t("common:showPassword")
                    }
                  >
                    {showCurrent ? <EyeOffIcon /> : <EyeIcon />}
                  </Button>
                </div>
                {errors.currentPassword ? (
                  <FieldDescription className="text-destructive">
                    {errors.currentPassword.message}
                  </FieldDescription>
                ) : null}
              </Field>

              <Field data-invalid={!!errors.newPassword}>
                <FieldLabel htmlFor="new-password">
                  {t("changePassword.newPassword")}
                </FieldLabel>
                <div className="relative">
                  <Input
                    id="new-password"
                    type={showNew ? "text" : "password"}
                    autoComplete="new-password"
                    placeholder={t("changePassword.newPasswordPlaceholder")}
                    className="pe-10"
                    aria-invalid={!!errors.newPassword}
                    {...register("newPassword")}
                  />
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    className="absolute end-0 top-0 text-muted-foreground hover:text-foreground"
                    onClick={() => setShowNew((visible) => !visible)}
                    aria-label={
                      showNew ? t("common:hidePassword") : t("common:showPassword")
                    }
                  >
                    {showNew ? <EyeOffIcon /> : <EyeIcon />}
                  </Button>
                </div>
                {errors.newPassword ? (
                  <FieldDescription className="text-destructive">
                    {errors.newPassword.message}
                  </FieldDescription>
                ) : null}
              </Field>
            </FieldGroup>

            <Button type="submit" disabled={isLoading} className="w-full">
              {isLoading ? (
                <>
                  <Spinner data-icon="inline-start" />
                  {t("common:loading")}
                </>
              ) : (
                t("changePassword.submit")
              )}
            </Button>
          </form>
        </CardContent>
        <CardFooter className="justify-center border-t border-border pt-6">
          <Link
            to={`/${localePrefix}/dashboard`}
            className="text-sm font-medium text-primary underline-offset-4 hover:underline"
          >
            {t("common:back")}
          </Link>
        </CardFooter>
      </Card>
    </div>
  );
}
