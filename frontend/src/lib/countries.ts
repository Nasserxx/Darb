export type CountryOption = { value: string; label: string };

/** Full ISO 3166-1 alpha-2 country codes (249). Intl.supportedValuesOf("region")
 *  is not a valid API, so the static list is the source of truth. */
const ISO_COUNTRY_CODES = [
  "AD","AE","AF","AG","AI","AL","AM","AO","AQ","AR","AS","AT","AU","AW","AX","AZ",
  "BA","BB","BD","BE","BF","BG","BH","BI","BJ","BL","BM","BN","BO","BQ","BR","BS",
  "BT","BV","BW","BY","BZ","CA","CC","CD","CF","CG","CH","CI","CK","CL","CM","CN",
  "CO","CR","CU","CV","CW","CX","CY","CZ","DE","DJ","DK","DM","DO","DZ","EC","EE",
  "EG","EH","ER","ES","ET","FI","FJ","FK","FM","FO","FR","GA","GB","GD","GE","GF",
  "GG","GH","GI","GL","GM","GN","GP","GQ","GR","GS","GT","GU","GW","GY","HK","HM",
  "HN","HR","HT","HU","ID","IE","IL","IM","IN","IO","IQ","IR","IS","IT","JE","JM",
  "JO","JP","KE","KG","KH","KI","KM","KN","KP","KR","KW","KY","KZ","LA","LB","LC",
  "LI","LK","LR","LS","LT","LU","LV","LY","MA","MC","MD","ME","MF","MG","MH","MK",
  "ML","MM","MN","MO","MP","MQ","MR","MS","MT","MU","MV","MW","MX","MY","MZ","NA",
  "NC","NE","NF","NG","NI","NL","NO","NP","NR","NU","NZ","OM","PA","PE","PF","PG",
  "PH","PK","PL","PM","PN","PR","PS","PT","PW","PY","QA","RE","RO","RS","RU","RW",
  "SA","SB","SC","SD","SE","SG","SH","SI","SJ","SK","SL","SM","SN","SO","SR","SS",
  "ST","SV","SX","SY","SZ","TC","TD","TF","TG","TH","TJ","TK","TL","TM","TN","TO",
  "TR","TT","TV","TW","TZ","UA","UG","UM","US","UY","UZ","VA","VC","VE","VG","VI",
  "VN","VU","WF","WS","YE","YT","ZA","ZM","ZW",
] as const;

const displayNamesCache = new Map<string, Intl.DisplayNames>();

function getDisplayNames(locale: string): Intl.DisplayNames {
  let displayNames = displayNamesCache.get(locale);
  if (!displayNames) {
    displayNames = new Intl.DisplayNames(locale, { type: "region" });
    displayNamesCache.set(locale, displayNames);
  }
  return displayNames;
}

function countryName(code: string, locale: string): string {
  const name = getDisplayNames(locale).of(code);
  if (name && name !== code) return name;
  const fallback = new Intl.DisplayNames("en", { type: "region" }).of(code);
  return fallback && fallback !== code ? fallback : code;
}

export function getCountryOptions(locale: string = "en"): CountryOption[] {
  return ISO_COUNTRY_CODES.map((code) => ({
    value: code,
    label: `${countryName(code, locale)} (${code})`,
  })).sort((a, b) => a.label.localeCompare(b.label, locale));
}

/** Localized region name for an ISO 3166-1 alpha-2 code, or `null` when no code
 *  is given. Falls back to the code itself when the locale cannot name it. */
export function localizeCountry(
  code: string | null | undefined,
  locale: string,
): string | null {
  if (!code) return null;
  try {
    const name = getDisplayNames(locale).of(code);
    return name && name !== code ? name : code;
  } catch {
    return code;
  }
}
