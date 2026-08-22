import { useTranslation } from "react-i18next";

import { cn } from "@/lib/utils.ts";
import { formatMosqueAddress, type MosqueAddressSource } from "@/lib/address.ts";

type AddressTextProps = {
  mosque: MosqueAddressSource;
  className?: string;
};

export function AddressText({ mosque, className }: AddressTextProps) {
  const { t, i18n } = useTranslation("app");
  const address = formatMosqueAddress(mosque, i18n.language);

  if (!address) {
    return (
      <span className={cn("text-muted-foreground", className)}>
        {t("mosques.addressNotProvided")}
      </span>
    );
  }

  return (
    <span dir="auto" className={className}>
      {address}
    </span>
  );
}
