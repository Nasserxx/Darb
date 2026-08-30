#!/usr/bin/env node
/**
 * Key-parity check for Darb i18n catalogs.
 *
 * Compares leaf keys across locales:
 *   - common + auth: en.json / ar.json / de.json
 *   - app: app-en.json / app-ar.json / app-de.json
 *
 * app-en is NOT treated as a peer of en.json — separate namespaces.
 * Exit 0 if all locales match; 1 if any keys are missing.
 */
import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const localesDir = join(__dirname, '..', 'src', 'i18n', 'locales')

function stripBom(text) {
  return text.charCodeAt(0) === 0xfeff ? text.slice(1) : text
}

function loadJson(filename) {
  const raw = readFileSync(join(localesDir, filename), 'utf8')
  return JSON.parse(stripBom(raw))
}

/** Flatten nested objects to dotted leaf keys. Arrays are treated as leaves. */
function flattenLeaves(value, prefix = '', out = new Set()) {
  if (value === null || typeof value !== 'object' || Array.isArray(value)) {
    if (prefix) out.add(prefix)
    return out
  }
  const keys = Object.keys(value)
  if (keys.length === 0) {
    if (prefix) out.add(prefix)
    return out
  }
  for (const key of keys) {
    const next = prefix ? `${prefix}.${key}` : key
    flattenLeaves(value[key], next, out)
  }
  return out
}

function leafKeysFromBundle(bundle, namespaces) {
  const keys = new Set()
  for (const ns of namespaces) {
    const section = bundle[ns]
    if (section == null || typeof section !== 'object') {
      console.error(`Missing top-level namespace "${ns}"`)
      continue
    }
    for (const leaf of flattenLeaves(section)) {
      keys.add(`${ns}:${leaf}`)
    }
  }
  return keys
}

function leafKeysFromApp(appJson) {
  return flattenLeaves(appJson)
}

function diffKeys(reference, candidate, label) {
  const missing = [...reference].filter((k) => !candidate.has(k)).sort()
  const extra = [...candidate].filter((k) => !reference.has(k)).sort()
  return { label, missing, extra }
}

function printDiff(diff) {
  if (diff.missing.length === 0 && diff.extra.length === 0) {
    console.log(`OK  ${diff.label}: keys match (${diff.label.includes('app') ? 'app' : 'common+auth'})`)
    return true
  }
  console.log(`FAIL ${diff.label}`)
  if (diff.missing.length) {
    console.log(`  missing (${diff.missing.length}):`)
    for (const k of diff.missing) console.log(`    - ${k}`)
  }
  if (diff.extra.length) {
    console.log(`  extra (${diff.extra.length}):`)
    for (const k of diff.extra) console.log(`    + ${k}`)
  }
  return false
}

const COMMON_AUTH_NS = ['common', 'auth']

const en = loadJson('en.json')
const ar = loadJson('ar.json')
const de = loadJson('de.json')
const appEn = loadJson('app-en.json')
const appAr = loadJson('app-ar.json')
const appDe = loadJson('app-de.json')

const enKeys = leafKeysFromBundle(en, COMMON_AUTH_NS)
const arKeys = leafKeysFromBundle(ar, COMMON_AUTH_NS)
const deKeys = leafKeysFromBundle(de, COMMON_AUTH_NS)

const appEnKeys = leafKeysFromApp(appEn)
const appArKeys = leafKeysFromApp(appAr)
const appDeKeys = leafKeysFromApp(appDe)

const diffs = [
  diffKeys(enKeys, arKeys, 'ar.json vs en.json'),
  diffKeys(enKeys, deKeys, 'de.json vs en.json'),
  diffKeys(appEnKeys, appArKeys, 'app-ar.json vs app-en.json'),
  diffKeys(appEnKeys, appDeKeys, 'app-de.json vs app-en.json'),
]

let ok = true
for (const d of diffs) {
  if (!printDiff(d)) ok = false
}

if (ok) {
  console.log(
    `pass: 0 missing — common+auth=${enKeys.size} keys, app=${appEnKeys.size} keys across en/ar/de`,
  )
  process.exit(0)
}

const totalMissing =
  diffs.reduce((n, d) => n + d.missing.length, 0)
console.log(`parity failed: ${totalMissing} missing key(s)`)
process.exit(1)
