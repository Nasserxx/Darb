import { zodResolver } from "@hookform/resolvers/zod";
import { Controller, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";

import { PageHeader } from "@/components/shared/page-header.tsx";
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
import { Skeleton } from "@/components/ui/skeleton.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";
import { translateRole } from "@/i18n/index.ts";
import { hasAddressValues } from "@/lib/address.ts";
import {
  applyFieldErrors,
  toMutationError,
} from "@/lib/errors/map-api-error.ts";

import { AddressFields } from "./address-fields.tsx";
import {
  toUserUpdateRequestBody,
  userUpdateSchema,
  type UserUpdateFormValues,
} from "../schemas/user-update.schema.ts";
import { useCurrentUser, useUpdateCurrentUser } from "../hooks/use-users.ts";

export function ProfileForm() {
  const { t } = useTranslation("app");
  const { data: user, isLoading } = useCurrentUser();
  const updateUser = useUpdateCurrentUser();

  const addressOpen = user
    ? hasAddressValues({
        city: user.city,
        addressCountry: user.addressCountry,
        addressPostalCode: user.addressPostalCode,
        addressStreet: user.addressStreet,
        addressHouseNumber: user.addressHouseNumber,
        addressState: user.addressState,
      })
    : false;

  const {
    register,
    handleSubmit,
    control,
    watch,
    setValue,
    setError,
    formState: { errors },
  } = useForm<UserUpdateFormValues>({
    resolver: zodResolver(userUpdateSchema),
    values: user
      ? {
          fullName: user.fullName,
          phone: user.phone ?? "",
          gender: user.gender ?? undefined,
          dateOfBirth: user.dateOfBirth ?? "",
          avatarUrl: user.avatarUrl ?? "",
          addressCountry: user.addressCountry ?? "",
          city: user.city ?? "",
          addressStreet: user.addressStreet ?? "",
          addressHouseNumber: user.addressHouseNumber ?? "",
          addressPostalCode: user.addressPostalCode ?? "",
          addressState: user.addressState ?? "",
        }
      : undefined,
  });

  async function onSubmit(values: UserUpdateFormValues) {
    try {
      await updateUser.mutateAsync(toUserUpdateRequestBody(values));
      toast.success(t("profile.updateSuccess"));
    } catch (error) {
      const { message, fieldErrors } = toMutationError(error, t);
      if (fieldErrors) {
        applyFieldErrors(fieldErrors, setError, t);
      }
      toast.error(message);
    }
  }

  if (isLoading) {
    return (
      <div className="flex flex-col gap-6">
        <Skeleton className="h-10 w-64" />
        <Skeleton className="h-64 w-full max-w-xl" />
      </div>
    );
  }

  if (!user) {
    return null;
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={t("profile.title")}
        description={t("profile.description")}
      />
      <form
        onSubmit={(event) => void handleSubmit(onSubmit)(event)}
        noValidate
        className="flex max-w-xl flex-col gap-6"
      >
        <FieldGroup>
          <Field>
            <FieldLabel>{t("profile.email")}</FieldLabel>
            <Input value={user.email} disabled readOnly />
          </Field>

          <Field>
            <FieldLabel>{t("profile.role")}</FieldLabel>
            <Input value={translateRole(user.role)} disabled readOnly />
          </Field>

          <Field data-invalid={!!errors.fullName}>
            <FieldLabel htmlFor="profile-full-name">{t("profile.fullName")}</FieldLabel>
            <Input
              id="profile-full-name"
              aria-invalid={!!errors.fullName}
              {...register("fullName")}
            />
            {errors.fullName ? (
              <FieldDescription className="text-destructive">
                {errors.fullName.message}
              </FieldDescription>
            ) : null}
          </Field>

          <Field data-invalid={!!errors.phone}>
            <FieldLabel htmlFor="profile-phone">{t("profile.phone")}</FieldLabel>
            <Input id="profile-phone" aria-invalid={!!errors.phone} {...register("phone")} />
            {errors.phone ? (
              <FieldDescription className="text-destructive">
                {errors.phone.message}
              </FieldDescription>
            ) : null}
          </Field>

          <Field data-invalid={!!errors.gender}>
            <FieldLabel>{t("profile.gender")}</FieldLabel>
            <Controller
              name="gender"
              control={control}
              render={({ field }) => (
                <Select
                  value={field.value ?? ""}
                  onValueChange={(value) =>
                    field.onChange(value === "" ? undefined : value)
                  }
                >
                  <SelectTrigger aria-invalid={!!errors.gender}>
                    <SelectValue placeholder={t("profile.selectPlaceholder")} />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="MALE">{t("profile.genderMale")}</SelectItem>
                    <SelectItem value="FEMALE">{t("profile.genderFemale")}</SelectItem>
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
            <FieldLabel htmlFor="profile-dob">{t("profile.dateOfBirth")}</FieldLabel>
            <Input
              id="profile-dob"
              type="date"
              aria-invalid={!!errors.dateOfBirth}
              {...register("dateOfBirth")}
            />
            {errors.dateOfBirth ? (
              <FieldDescription className="text-destructive">
                {errors.dateOfBirth.message}
              </FieldDescription>
            ) : null}
          </Field>

          <Field data-invalid={!!errors.avatarUrl}>
            <FieldLabel htmlFor="profile-avatar">{t("profile.avatarUrl")}</FieldLabel>
            <Input
              id="profile-avatar"
              aria-invalid={!!errors.avatarUrl}
              {...register("avatarUrl")}
            />
            {errors.avatarUrl ? (
              <FieldDescription className="text-destructive">
                {errors.avatarUrl.message}
              </FieldDescription>
            ) : null}
          </Field>

          <AddressFields
            register={register}
            errors={errors}
            watch={watch}
            setValue={setValue}
            idPrefix="profile"
            defaultOpen={addressOpen}
          />
        </FieldGroup>

        <Button type="submit" disabled={updateUser.isPending} className="w-fit">
          {updateUser.isPending ? (
            <>
              <Spinner data-icon="inline-start" />
              {t("actions.save")}
            </>
          ) : (
            t("actions.save")
          )}
        </Button>
      </form>
    </div>
  );
}
