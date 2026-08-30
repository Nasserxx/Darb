import { useState } from "react";
import { useTranslation } from "react-i18next";

import { Button } from "@/components/ui/button.tsx";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog.tsx";
import { Field, FieldDescription, FieldLabel } from "@/components/ui/field.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";
import { Textarea } from "@/components/ui/textarea.tsx";
import { MIN_AUDIT_REASON_LENGTH } from "@/lib/api/audit-reason.ts";

type SuperAdminAuditReasonDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description?: string;
  confirmLabel: string;
  onConfirm: (reason: string) => void | Promise<void>;
  isPending?: boolean;
};

export function SuperAdminAuditReasonDialog({
  open,
  onOpenChange,
  title,
  description,
  confirmLabel,
  onConfirm,
  isPending,
}: SuperAdminAuditReasonDialogProps) {
  const { t } = useTranslation("app");
  const [reason, setReason] = useState("");

  const trimmed = reason.trim();
  const canConfirm = trimmed.length >= MIN_AUDIT_REASON_LENGTH;

  function handleOpenChange(nextOpen: boolean) {
    if (!nextOpen) setReason("");
    onOpenChange(nextOpen);
  }

  async function handleConfirm() {
    if (!canConfirm || isPending) return;
    await onConfirm(trimmed);
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>
            {description ?? t("superAdmin.auditReason.description")}
          </DialogDescription>
        </DialogHeader>
        <Field>
          <FieldLabel htmlFor="sa-audit-reason">
            {t("superAdmin.auditReason.label")}
          </FieldLabel>
          <Textarea
            id="sa-audit-reason"
            value={reason}
            onChange={(event) => setReason(event.target.value)}
            placeholder={t("superAdmin.auditReason.placeholder")}
            disabled={isPending}
            rows={3}
          />
          {!canConfirm && reason.length > 0 ? (
            <FieldDescription className="text-destructive">
              {t("superAdmin.auditReason.tooShort")}
            </FieldDescription>
          ) : null}
        </Field>
        <DialogFooter>
          <Button
            type="button"
            variant="outline"
            disabled={isPending}
            onClick={() => handleOpenChange(false)}
          >
            {t("cancel", { ns: "common" })}
          </Button>
          <Button
            type="button"
            disabled={!canConfirm || isPending}
            onClick={() => void handleConfirm()}
          >
            {isPending ? <Spinner data-icon="inline-start" /> : null}
            {confirmLabel}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
