import { ChevronLeftIcon, CopyIcon, PencilIcon, RefreshCwIcon, Trash2Icon } from "lucide-react";
import { useState } from "react";
import type { ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";
import { toast } from "sonner";

import { AddressText } from "@/components/address-text.tsx";
import { ConfirmDeleteDialog } from "@/components/shared/confirm-delete-dialog.tsx";
import { PageHeader } from "@/components/shared/page-header.tsx";
import { Badge } from "@/components/ui/badge.tsx";
import { Button } from "@/components/ui/button.tsx";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card.tsx";
import { Skeleton } from "@/components/ui/skeleton.tsx";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import { MosqueFormDialog } from "@/features/mosques/components/mosque-form-dialog.tsx";
import {
  useDeleteMosque,
  useMosque,
  useMosqueInviteCodes,
  useReactivateMosque,
  useRotateInviteCodes,
} from "@/features/mosques/hooks/use-mosques.ts";
import type { MosqueInviteCodesResponse } from "@/features/mosques/types/onboard.ts";
import { DEFAULT_LOCALE } from "@/i18n/index.ts";
import { normalizeApiRole } from "@/lib/navigation/app-nav.ts";
import { toMutationError } from "@/lib/errors/map-api-error.ts";

function DetailRow({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="flex items-baseline justify-between gap-4">
      <span className="text-sm text-muted-foreground">{label}</span>
      <span className="text-sm font-medium">{value ?? "—"}</span>
    </div>
  );
}

const INVITE_CODE_KEYS: Array<{
  key: keyof MosqueInviteCodesResponse;
  labelKey: string;
}> = [
  { key: "adminInviteCode", labelKey: "admin.inviteCodes.admin" },
  { key: "teacherInviteCode", labelKey: "admin.inviteCodes.teacher" },
  { key: "studentInviteCode", labelKey: "admin.inviteCodes.student" },
];

export function MosqueDetailPage() {
  const { t } = useTranslation("app");
  const { user } = useAuth();
  const { id, locale } = useParams<{ id: string; locale: string }>();
  const localePrefix = locale ?? DEFAULT_LOCALE;
  const role = user ? normalizeApiRole(user.role) : null;
  const isSuperAdmin = role === "SUPER_ADMIN";

  const { data: mosque, isLoading, isError } = useMosque(id ?? "");
  const inviteCodesQuery = useMosqueInviteCodes(id ?? "", {
    enabled: Boolean(mosque?.isActive),
  });
  const rotateCodes = useRotateInviteCodes(id ?? "");
  const reactivateMosque = useReactivateMosque();
  const deleteMosque = useDeleteMosque();
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);

  async function copyCode(code: string | undefined) {
    if (!code) return;
    try {
      await navigator.clipboard.writeText(code);
      toast.success(t("admin.inviteCodes.copied"));
    } catch {
      toast.error(t("onboarding.error"));
    }
  }

  async function handleRotate() {
    try {
      await rotateCodes.mutateAsync();
      toast.success(t("mosques.rotateSuccess"));
    } catch (error) {
      toast.error(toMutationError(error, t).message);
    }
  }

  async function handleReactivate() {
    if (!mosque) return;
    try {
      await reactivateMosque.mutateAsync(mosque.id);
      toast.success(t("mosques.reactivateSuccess"));
    } catch (error) {
      toast.error(toMutationError(error, t).message);
    }
  }

  async function handleDeactivate() {
    if (!mosque) return;
    try {
      await deleteMosque.mutateAsync(mosque.id);
      toast.success(t("mosques.deactivateSuccess"));
      setDeleteOpen(false);
    } catch (error) {
      toast.error(toMutationError(error, t).message);
    }
  }

  if (isLoading) {
    return (
      <div className="flex flex-col gap-4">
        <Skeleton className="h-10 w-64" />
        <Skeleton className="h-32 w-full" />
        <Skeleton className="h-48 w-full" />
      </div>
    );
  }

  if (isError || !mosque) {
    return (
      <div className="flex flex-col gap-4">
        <PageHeader title={t("mosques.details")} />
        <p className="text-sm text-muted-foreground">{t("table.empty")}</p>
        <Button variant="outline" asChild>
          <Link to={`/${localePrefix}/mosques`}>
            <ChevronLeftIcon data-icon="inline-start" />
            {t("mosques.backToList")}
          </Link>
        </Button>
      </div>
    );
  }

  const isActive = mosque.isActive;

  return (
    <div className="flex flex-col gap-6">
      <Button variant="ghost" size="sm" className="w-fit" asChild>
        <Link to={`/${localePrefix}/mosques`}>
          <ChevronLeftIcon data-icon="inline-start" />
          {t("mosques.backToList")}
        </Link>
      </Button>

      <PageHeader
        title={mosque.name}
        description={mosque.city ?? undefined}
        actions={
          <div className="flex flex-wrap items-center gap-2">
            <Badge variant={isActive ? "default" : "secondary"}>
              {isActive ? t("mosques.active") : t("mosques.inactive")}
            </Badge>
            {isActive ? (
              <Button variant="outline" onClick={() => setEditOpen(true)}>
                <PencilIcon data-icon="inline-start" />
                {t("actions.edit")}
              </Button>
            ) : null}
            {isSuperAdmin && !isActive ? (
              <Button onClick={() => void handleReactivate()}>
                <RefreshCwIcon data-icon="inline-start" />
                {t("mosques.reactivate")}
              </Button>
            ) : null}
            {isSuperAdmin && isActive ? (
              <Button variant="destructive" onClick={() => setDeleteOpen(true)}>
                <Trash2Icon data-icon="inline-start" />
                {t("mosques.deactivate")}
              </Button>
            ) : null}
          </div>
        }
      />

      <Card>
        <CardHeader>
          <CardTitle>{t("mosques.details")}</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-3">
          <DetailRow
            label={t("mosques.address")}
            value={<AddressText mosque={mosque} />}
          />
          <DetailRow label={t("mosques.phone")} value={mosque.phone} />
          <DetailRow label={t("mosques.email")} value={mosque.email} />
          <DetailRow label={t("mosques.timezone")} value={mosque.timezone} />
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="flex flex-row items-start justify-between gap-4">
          <div className="flex flex-col gap-1">
            <CardTitle>{t("admin.sections.inviteCodes")}</CardTitle>
            <CardDescription>{t("mosques.inviteCodesDescription")}</CardDescription>
          </div>
          <Button
            size="sm"
            variant="outline"
            disabled={!isActive || rotateCodes.isPending}
            onClick={() => void handleRotate()}
          >
            <RefreshCwIcon data-icon="inline-start" />
            {t("mosques.rotateCodes")}
          </Button>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          {!isActive ? (
            <p className="text-sm text-muted-foreground">
              {t("mosques.inviteCodesUnavailable")}
            </p>
          ) : inviteCodesQuery.isLoading ? (
            <div className="flex flex-col gap-2">
              <Skeleton className="h-14 w-full" />
              <Skeleton className="h-14 w-full" />
              <Skeleton className="h-14 w-full" />
            </div>
          ) : inviteCodesQuery.isError || !inviteCodesQuery.data ? (
            <p className="text-sm text-muted-foreground">{t("table.empty")}</p>
          ) : (
            INVITE_CODE_KEYS.map(({ key, labelKey }) => {
              const code = inviteCodesQuery.data?.[key];
              return (
                <div
                  key={key}
                  className="flex flex-col gap-2 rounded-lg border border-border/70 p-4 sm:flex-row sm:items-center sm:justify-between"
                >
                  <div className="flex flex-col gap-1">
                    <p className="text-sm font-medium">{t(labelKey)}</p>
                    <p className="font-mono text-sm">{code ?? "—"}</p>
                  </div>
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={!code}
                    onClick={() => void copyCode(code)}
                  >
                    <CopyIcon data-icon="inline-start" />
                    {t("admin.inviteCodes.copy")}
                  </Button>
                </div>
              );
            })
          )}
        </CardContent>
      </Card>

      <ConfirmDeleteDialog
        open={deleteOpen}
        onOpenChange={setDeleteOpen}
        title={t("mosques.deactivateTitle")}
        description={t("mosques.deactivateDescription")}
        onConfirm={() => void handleDeactivate()}
        isPending={deleteMosque.isPending}
      />

      <MosqueFormDialog
        open={editOpen}
        onOpenChange={setEditOpen}
        mosque={mosque}
      />
    </div>
  );
}
