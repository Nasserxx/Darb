import { zodResolver } from "@hookform/resolvers/zod";
import { EyeIcon, EyeOffIcon } from "lucide-react";
import { useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";

import { Button } from "@/components/ui/button.tsx";
import {
  Field,
  FieldDescription,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field.tsx";
import { Input } from "@/components/ui/input.tsx";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";
import { AuthShell } from "@/features/auth/components/auth-shell.tsx";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import {
  createRegisterSchema,
  toRegisterRequestBody,
} from "@/features/auth/schemas/register.schema.ts";
import { AddressFields } from "@/features/users/components/address-fields.tsx";
import type { z } from "zod";
import {
  applyFieldErrors,
  translateAuthApiMessage,
} from "@/features/auth/utils/form-errors.ts";
import { DEFAULT_LOCALE } from "@/i18n/index.ts";

const REGISTER_ROLES = [
  "student",
  "teacher",
  "parent",
  "mosque_admin",
] as const;

const GENDERS = ["MALE", "FEMALE"] as const;

type RegisterFormInput = z.input<ReturnType<typeof createRegisterSchema>>;

export function RegisterForm() {
  const { t } = useTranslation("auth");
  const { register: registerUser, isLoading } = useAuth();
  const navigate = useNavigate();
  const { locale } = useParams<{ locale: string }>();
  const [showPassword, setShowPassword] = useState(false);
  const localePrefix = locale ?? DEFAULT_LOCALE;

  const {
    register,
    control,
    handleSubmit,
    watch,
    setValue,
    setError,
    formState: { errors },
  } = useForm<RegisterFormInput>({
    // ponytail: rebuild schema each validate so locale switch picks up messages
    resolver: (values, context, options) =>
      zodResolver(createRegisterSchema(t))(values, context, options),
    defaultValues: {
      fullName: "",
      email: "",
      phone: "",
      password: "",
      confirmPassword: "",
      role: "student",
      gender: undefined,
      dateOfBirth: "",
      addressCountry: "",
      city: "",
      addressStreet: "",
      addressHouseNumber: "",
      addressPostalCode: "",
      addressState: "",
    },
  });

  async function onSubmit(values: RegisterFormInput) {
    const parsed = createRegisterSchema(t).parse(values);
    const result = await registerUser(toRegisterRequestBody(parsed));
    if (result.ok) {
      toast.success(t("register.success"));
      const params = new URLSearchParams({ email: values.email });
      navigate(`/${localePrefix}/login?${params.toString()}`, { replace: true });
      return;
    }

    if (result.fieldErrors) {
      applyFieldErrors(result.fieldErrors, setError, t);
    }
    toast.error(translateAuthApiMessage(result.message, t));
  }

  return (
    <AuthShell
      title={t("register.title")}
      subtitle={t("register.subtitle")}
      footer={
        <p>
          {t("register.hasAccount")}{" "}
          <Link
            to={`/${localePrefix}/login`}
            className="font-medium text-primary underline-offset-4 hover:underline"
          >
            {t("register.signInLink")}
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
          <Field data-invalid={!!errors.fullName}>
            <FieldLabel htmlFor="register-fullName">
              {t("register.fullName")}
            </FieldLabel>
            <Input
              id="register-fullName"
              autoComplete="name"
              placeholder={t("register.fullNamePlaceholder")}
              aria-invalid={!!errors.fullName}
              {...register("fullName")}
            />
            {errors.fullName ? (
              <FieldDescription className="text-destructive">
                {errors.fullName.message}
              </FieldDescription>
            ) : null}
          </Field>

          <Field data-invalid={!!errors.email}>
            <FieldLabel htmlFor="register-email">{t("register.email")}</FieldLabel>
            <Input
              id="register-email"
              type="email"
              autoComplete="email"
              placeholder={t("register.emailPlaceholder")}
              aria-invalid={!!errors.email}
              {...register("email")}
            />
            {errors.email ? (
              <FieldDescription className="text-destructive">
                {errors.email.message}
              </FieldDescription>
            ) : null}
          </Field>

          <Field data-invalid={!!errors.phone}>
            <FieldLabel htmlFor="register-phone">
              {t("register.phone")}{" "}
              <span className="font-normal text-muted-foreground">
                ({t("common:optional")})
              </span>
            </FieldLabel>
            <Input
              id="register-phone"
              type="tel"
              autoComplete="tel"
              placeholder={t("register.phonePlaceholder")}
              aria-invalid={!!errors.phone}
              {...register("phone")}
            />
            {errors.phone ? (
              <FieldDescription className="text-destructive">
                {errors.phone.message}
              </FieldDescription>
            ) : null}
          </Field>

          <Field data-invalid={!!errors.password}>
            <FieldLabel htmlFor="register-password">
              {t("register.password")}
            </FieldLabel>
            <div className="relative">
              <Input
                id="register-password"
                type={showPassword ? "text" : "password"}
                autoComplete="new-password"
                placeholder={t("register.passwordPlaceholder")}
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
            ) : (
              <FieldDescription>{t("register.passwordHint")}</FieldDescription>
            )}
          </Field>

          <Field data-invalid={!!errors.confirmPassword}>
            <FieldLabel htmlFor="register-confirmPassword">
              {t("register.confirmPassword")}
            </FieldLabel>
            <div className="relative">
              <Input
                id="register-confirmPassword"
                type={showPassword ? "text" : "password"}
                autoComplete="new-password"
                placeholder={t("register.confirmPasswordPlaceholder")}
                className="pe-10"
                aria-invalid={!!errors.confirmPassword}
                {...register("confirmPassword")}
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
            {errors.confirmPassword ? (
              <FieldDescription className="text-destructive">
                {errors.confirmPassword.message}
              </FieldDescription>
            ) : null}
          </Field>

          <Field data-invalid={!!errors.role}>
            <FieldLabel htmlFor="register-role">{t("register.role")}</FieldLabel>
            <Controller
              name="role"
              control={control}
              render={({ field }) => (
                <Select value={field.value} onValueChange={field.onChange}>
                  <SelectTrigger id="register-role" aria-invalid={!!errors.role}>
                    <SelectValue placeholder={t("register.rolePlaceholder")} />
                  </SelectTrigger>
                  <SelectContent>
                    {REGISTER_ROLES.map((role) => (
                      <SelectItem key={role} value={role}>
                        {t(`roles.${role}`)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
            {errors.role ? (
              <FieldDescription className="text-destructive">
                {errors.role.message}
              </FieldDescription>
            ) : null}
          </Field>

          <Field data-invalid={!!errors.gender}>
            <FieldLabel htmlFor="register-gender">
              {t("register.gender")}{" "}
              <span className="font-normal text-muted-foreground">
                ({t("common:optional")})
              </span>
            </FieldLabel>
            <Controller
              name="gender"
              control={control}
              render={({ field }) => (
                <Select
                  value={field.value}
                  onValueChange={field.onChange}
                >
                  <SelectTrigger id="register-gender" aria-invalid={!!errors.gender}>
                    <SelectValue placeholder={t("register.genderPlaceholder")} />
                  </SelectTrigger>
                  <SelectContent>
                    {GENDERS.map((gender) => (
                      <SelectItem key={gender} value={gender}>
                        {t(`genders.${gender}`)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
            {errors.gender ? (
              <FieldDescription className="text-destructive">
                {errors.gender.message}
              </FieldDescription>
            ) : null}
          </Field>

          <Field data-invalid={!!errors.dateOfBirth}>
            <FieldLabel htmlFor="register-dob">
              {t("register.dateOfBirth")}{" "}
              <span className="font-normal text-muted-foreground">
                ({t("common:optional")})
              </span>
            </FieldLabel>
            <Input
              id="register-dob"
              type="date"
              aria-invalid={!!errors.dateOfBirth}
              {...register("dateOfBirth")}
            />
            {errors.dateOfBirth ? (
              <FieldDescription className="text-destructive">
                {errors.dateOfBirth.message}
              </FieldDescription>
            ) : (
              <FieldDescription>
                {t("register.dateOfBirthPlaceholder")}
              </FieldDescription>
            )}
          </Field>

          <AddressFields
            register={register}
            errors={errors}
            watch={watch}
            setValue={setValue}
            idPrefix="register"
            defaultOpen={false}
          />
        </FieldGroup>

        <Button type="submit" disabled={isLoading} className="w-full">
          {isLoading ? (
            <>
              <Spinner data-icon="inline-start" />
              {t("common:loading")}
            </>
          ) : (
            t("register.submit")
          )}
        </Button>
      </form>
    </AuthShell>
  );
}
