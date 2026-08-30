import { useTranslation } from "react-i18next";

import { Badge } from "@/components/ui/badge";

/** Distinct from active membership badges (default/secondary). */
export function FormerMemberBadge() {
  const { t } = useTranslation("app");

  return <Badge variant="outline">{t("membership.formerMember")}</Badge>;
}
