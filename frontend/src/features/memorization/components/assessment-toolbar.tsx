import { useTranslation } from "react-i18next";

import { Button } from "@/components/ui/button.tsx";
import type { StampType } from "@/features/memorization/types/index.ts";

interface AssessmentToolbarProps {
  activeType: StampType;
  onTypeChange: (type: StampType) => void;
  onUndo: () => void;
  onSave: () => void;
  tajweedCount: number;
  hifzCount: number;
  isSaving?: boolean;
  canUndo?: boolean;
}

export function AssessmentToolbar({
  activeType,
  onTypeChange,
  onUndo,
  onSave,
  tajweedCount,
  hifzCount,
  isSaving,
  canUndo = true,
}: AssessmentToolbarProps) {
  const { t } = useTranslation("app");

  return (
    <div className="flex flex-wrap items-center gap-2 rounded-lg border bg-muted/30 p-2">
      <Button
        type="button"
        size="sm"
        variant={activeType === "TAJWEED" ? "default" : "outline"}
        onClick={() => onTypeChange("TAJWEED")}
      >
        {t("memorization.stampTajweed")}
      </Button>
      <Button
        type="button"
        size="sm"
        variant={activeType === "HIFZ" ? "default" : "outline"}
        onClick={() => onTypeChange("HIFZ")}
      >
        {t("memorization.stampHifz")}
      </Button>
      <Button
        type="button"
        size="sm"
        variant="outline"
        onClick={onUndo}
        disabled={!canUndo}
      >
        {t("memorization.undo")}
      </Button>
      <Button type="button" size="sm" onClick={onSave} disabled={isSaving}>
        {isSaving ? t("memorization.saving") : t("actions.save")}
      </Button>
      <span className="ms-auto text-sm text-muted-foreground tabular-nums">
        {t("memorization.stampCounts", { tajweed: tajweedCount, hifz: hifzCount })}
      </span>
    </div>
  );
}

export function ReadonlyStampSummary({
  stampCount,
  tajweedCount,
  hifzCount,
}: {
  stampCount: number;
  tajweedCount: number;
  hifzCount: number;
}) {
  const { t } = useTranslation("app");

  if (stampCount === 0) {
    return (
      <p className="text-sm text-muted-foreground">{t("memorization.noStamps")}</p>
    );
  }

  return (
    <p className="text-sm text-muted-foreground tabular-nums">
      {t("memorization.stampCounts", { tajweed: tajweedCount, hifz: hifzCount })}
    </p>
  );
}
