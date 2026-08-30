import { localizeCountry } from "./countries.ts";

export type AddressSource = {
  city?: string | null;
  addressCountry?: string | null;
  addressPostalCode?: string | null;
  addressStreet?: string | null;
  addressHouseNumber?: string | null;
  addressState?: string | null;
};

/** @deprecated Prefer AddressSource — kept for mosque call sites. */
export type MosqueAddressSource = AddressSource;

const CONTROL_OR_BIDI_CHARS_RE =
  /[\p{Cc}\u202a-\u202e\u2066-\u2069\u200b]/gu;
const WHITESPACE_RUNS_RE = /\s+/g;

function normalizeTextPart(value: string | null | undefined): string {
  return (value ?? "")
    .replace(CONTROL_OR_BIDI_CHARS_RE, " ")
    .replace(WHITESPACE_RUNS_RE, " ")
    .trim();
}

export function formatAddress(
  source: AddressSource,
  locale = "en",
): string | null {
  const street = normalizeTextPart(source.addressStreet);
  const houseNumber = normalizeTextPart(source.addressHouseNumber);
  const postalCode = normalizeTextPart(source.addressPostalCode);
  const city = normalizeTextPart(source.city);
  const state = normalizeTextPart(source.addressState);
  const country = localizeCountry(source.addressCountry, locale);

  const streetLine = [street, houseNumber].filter(Boolean).join(" ");
  const cityLine = [postalCode, city].filter(Boolean).join(" ");
  const stateCountry = [state, country].filter(Boolean).join(" ");

  const address = [streetLine, cityLine, stateCountry]
    .filter(Boolean)
    .join(", ");

  return address || null;
}

export function formatMosqueAddress(
  mosque: MosqueAddressSource,
  locale = "en",
): string | null {
  return formatAddress(mosque, locale);
}

export function hasAddressValues(source: AddressSource): boolean {
  return Boolean(
    normalizeTextPart(source.addressCountry) ||
      normalizeTextPart(source.city) ||
      normalizeTextPart(source.addressStreet) ||
      normalizeTextPart(source.addressHouseNumber) ||
      normalizeTextPart(source.addressPostalCode) ||
      normalizeTextPart(source.addressState),
  );
}
