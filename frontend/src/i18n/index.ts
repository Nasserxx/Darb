import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'

import ar from './locales/ar.json'
import de from './locales/de.json'
import en from './locales/en.json'

export const LOCALE_STORAGE_KEY = 'darb-locale'
export const DEFAULT_LOCALE = 'en'
export const SUPPORTED_LOCALES = ['en', 'ar', 'de'] as const
export type Locale = (typeof SUPPORTED_LOCALES)[number]

export const RTL_LOCALES: ReadonlySet<Locale> = new Set(['ar'])

/** Maps API role enum names (e.g. STUDENT, MOSQUE_ADMIN) to auth:roles.* keys. */
const API_ROLE_TO_I18N_KEY: Record<string, string> = {
  STUDENT: 'student',
  TEACHER: 'teacher',
  PARENT: 'parent',
  MOSQUE_ADMIN: 'mosque_admin',
}

type LocaleBundle = { common: Record<string, unknown>; auth: Record<string, unknown> }

const resources = {
  en: { common: (en as LocaleBundle).common, auth: (en as LocaleBundle).auth },
  ar: { common: (ar as LocaleBundle).common, auth: (ar as LocaleBundle).auth },
  de: { common: (de as LocaleBundle).common, auth: (de as LocaleBundle).auth },
} as const

function readStoredLocale(): string | null {
  if (typeof window === 'undefined') {
    return null
  }
  return localStorage.getItem(LOCALE_STORAGE_KEY)
}

function normalizeLocale(value: string | null | undefined): Locale {
  if (value && SUPPORTED_LOCALES.includes(value as Locale)) {
    return value as Locale
  }
  return DEFAULT_LOCALE
}

export function isRtlLocale(locale?: string): boolean {
  return RTL_LOCALES.has(normalizeLocale(locale ?? i18n.language))
}

/** i18n key under auth namespace for a role returned by the API (uppercase enum name). */
export function roleI18nKey(apiRole: string): string {
  const normalized = apiRole.trim().toUpperCase()
  const slug = API_ROLE_TO_I18N_KEY[normalized] ?? apiRole.trim().toLowerCase()
  return `roles.${slug}`
}

export function translateRole(
  apiRole: string,
  options?: { lng?: string },
): string {
  return i18n.t(roleI18nKey(apiRole), { ns: 'auth', ...options })
}

export function persistLocale(locale: Locale): void {
  if (typeof window !== 'undefined') {
    localStorage.setItem(LOCALE_STORAGE_KEY, locale)
  }
  void i18n.changeLanguage(locale)
  document.documentElement.lang = locale
  document.documentElement.dir = isRtlLocale(locale) ? 'rtl' : 'ltr'
}

const initialLocale = normalizeLocale(readStoredLocale())

void i18n.use(initReactI18next).init({
  resources,
  lng: initialLocale,
  fallbackLng: DEFAULT_LOCALE,
  supportedLngs: [...SUPPORTED_LOCALES],
  ns: ['common', 'auth'],
  defaultNS: 'common',
  interpolation: { escapeValue: false },
})

document.documentElement.lang = initialLocale
document.documentElement.dir = isRtlLocale(initialLocale) ? 'rtl' : 'ltr'

export default i18n
