import { zodResolver } from "@hookform/resolvers/zod";
import { EyeIcon, EyeOffIcon } from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useLocation, useNavigate, useParams, useSearchParams } from "react-router-dom";
import { toast } from "sonner";

import { Button } from "@/components/ui/button.tsx";
import {
  Field,
  FieldDescription,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field.tsx";
import { Input } from "@/components/ui/input.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";
import { AuthShell } from "@/features/auth/components/auth-shell.tsx";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import { getUserSession } from "@/features/auth/session/storage.ts";
import {
  loginSchema,
  type LoginFormValues,
} from "@/features/auth/schemas/login.schema.ts";
import {
  applyFieldErrors,
  translateAuthApiMessage,
} from "@/features/auth/utils/form-errors.ts";
import { workspaceApi } from "@/features/workspace/api/workspace-api.ts";
import { DEFAULT_LOCALE } from "@/i18n/index.ts";
import {
  deriveProfileStatus,
  resolvePostAuthPath,
} from "@/lib/navigation/post-auth.ts";

export function LoginForm() {
  const { t } = useTranslation("auth");
  const { login, isLoading } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const { locale } = useParams<{ locale: string }>();
  const [searchParams] = useSearchParams();
  const [showPassword, setShowPassword] = useState(false);
  const localePrefix = locale ?? DEFAULT_LOCALE;

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: searchParams.get("email") ?? "",
      password: "",
    },
  });

  async function onSubmit(values: LoginFormValues) {
    const result = await login(values);
    if (result.ok) {
      toast.success(t("login.success"));
      const profile = await workspaceApi.getProfile();
      const profileStatus = deriveProfileStatus(profile, false);
      const returnTo =
        (location.state as { from?: { pathname?: string } } | null)?.from
          ?.pathname ?? null;
      const session = getUserSession();
      const path = resolvePostAuthPath({
        locale: localePrefix,
        role: session?.role ?? "",
        profileStatus,
        returnTo,
        inviteCode: searchParams.get("code"),
        roleHint: searchParams.get("role"),
      });
      if (path) {
        navigate(path, { replace: true });
      }
      return;
    }

    if (result.fieldErrors) {
      applyFieldErrors(result.fieldErrors, setError, t);
    }
    toast.error(translateAuthApiMessage(result.message, t));
  }

  return (
    <AuthShell
      title={t("login.title")}
      subtitle={t("login.subtitle")}
      footer={
        <p>
          {t("login.noAccount")}{" "}
          <Link
            to={`/${localePrefix}/register`}
            className="font-medium text-primary underline-offset-4 hover:underline"
          >
            {t("login.signUpLink")}
          </Link>
        </p>
      }
    >
      <form
        onSubmit={(event) => void handleSubmit(onSubmit)(event)}
        noValidate
        className="flex flex-col gap-6"
      >
        <FieldGroup>
          <Field data-invalid={!!errors.email}>
            <FieldLabel htmlFor="login-email">{t("login.email")}</FieldLabel>
            <Input
              id="login-email"
              type="email"
              autoComplete="email"
              placeholder={t("login.emailPlaceholder")}
              aria-invalid={!!errors.email}
              {...register("email")}
            />
            {errors.email ? (
              <FieldDescription className="text-destructive">
                {errors.email.message}
              </FieldDescription>
            ) : null}
          </Field>

          <Field data-invalid={!!errors.password}>
            <FieldLabel htmlFor="login-password">
              {t("login.password")}
            </FieldLabel>
            <div className="relative">
              <Input
                id="login-password"
                type={showPassword ? "text" : "password"}
                autoComplete="current-password"
                placeholder={t("login.passwordPlaceholder")}
                className="pe-10"
                aria-invalid={!!errors.password}
                {...register("password")}
              />
              <Button
                type="button"
                variant="ghost"
                size="icon"
                className="absolute end-0 top-0 text-muted-foreground hover:text-foreground"
                onClick={() => setShowPassword((visible) => !visible)}
                aria-label={
                  showPassword ? t("common:hidePassword") : t("common:showPassword")
                }
              >
                {showPassword ? <EyeOffIcon /> : <EyeIcon />}
              </Button>
            </div>
            {errors.password ? (
              <FieldDescription className="text-destructive">
                {errors.password.message}
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
            t("login.submit")
          )}
        </Button>
      </form>
    </AuthShell>
  );
}
