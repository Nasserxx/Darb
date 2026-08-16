import { localizeCountry } from "./countries.ts";

export type MosqueAddressSource = {
  city?: string | null;
  addressCountry?: string | null;
  addressPostalCode?: string | null;
  addressStreet?: string | null;
  addressHouseNumber?: string | null;
  addressState?: string | null;
};

const CONTROL_OR_BIDI_CHARS_RE =
  /[\p{Cc}\u202a-\u202e\u2066-\u2069\u200b]/gu;
const WHITESPACE_RUNS_RE = /\s+/g;

function normalizeTextPart(value: string | null | undefined): string {
  return (value ?? "")
    .replace(CONTROL_OR_BIDI_CHARS_RE, " ")
    .replace(WHITESPACE_RUNS_RE, " ")
    .trim();
}

export function formatMosqueAddress(
  mosque: MosqueAddressSource,
  locale = "en",
): string | null {
  const street = normalizeTextPart(mosque.addressStreet);
  const houseNumber = normalizeTextPart(mosque.addressHouseNumber);
  const postalCode = normalizeTextPart(mosque.addressPostalCode);
  const city = normalizeTextPart(mosque.city);
  const state = normalizeTextPart(mosque.addressState);
  const country = localizeCountry(mosque.addressCountry, locale);

  const streetLine = [street, houseNumber].filter(Boolean).join(" ");
  const cityLine = [postalCode, city].filter(Boolean).join(" ");
  const stateCountry = [state, country].filter(Boolean).join(" ");

  const address = [streetLine, cityLine, stateCountry]
    .filter(Boolean)
    .join(", ");

  return address || null;
}
