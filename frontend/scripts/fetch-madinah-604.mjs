#!/usr/bin/env node
/**
 * Fetch / convert Madinah 604 mushaf page images for local development.
 *
 * License / source (operators must comply — do not redistribute without rights):
 *   King Fahd Glorious Quran Printing Complex (KFGQPC)
 *   Digital Mushaf portal: https://dm.qurancomplex.gov.sa/
 *
 * Expected output layout (not committed to git):
 *   frontend/public/mushaf/madinah-604/1.webp … 604.webp
 *   frontend/public/mushaf/madinah-604/placeholder.svg  (shipped separately)
 *
 * This script does NOT download or convert page binaries automatically.
 * KFGQPC terms and portal access are not freely automatable for CI/git.
 * Run manually after obtaining licensed page images, then convert to WebP.
 *
 * Usage:
 *   node frontend/scripts/fetch-madinah-604.mjs
 *   node frontend/scripts/fetch-madinah-604.mjs --help
 *
 * Exit 0 always (help / instructions only).
 */
import { mkdirSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = dirname(fileURLToPath(import.meta.url));
const OUT_DIR = join(__dirname, "..", "public", "mushaf", "madinah-604");
const TOTAL_PAGES = 604;

function printHelp() {
  console.log(`
Madinah 604 mushaf assets — fetch / convert helper (stub)

Source / license
  KFGQPC Digital Mushaf: https://dm.qurancomplex.gov.sa/
  Obtain page images only under KFGQPC licensing terms for your deployment.
  Do NOT commit the 604 WebP binaries to git.

Target directory
  ${OUT_DIR}
  Files: 1.webp … ${TOTAL_PAGES}.webp  (plus placeholder.svg already in repo)

Manual workflow (example)
  1. Download licensed page images from the KFGQPC Digital Mushaf portal.
  2. Place source files in a local working folder (outside the repo if preferred).
  3. Convert each page to WebP, e.g. with cwebp / sharp / ImageMagick:
       cwebp -q 80 page-001.png -o ${join(OUT_DIR, "1.webp")}
  4. Name outputs as {page}.webp for pages 1–${TOTAL_PAGES}.
  5. Keep placeholder.svg for onError fallback when a page file is absent.

Automation
  No public machine-download URL is wired here. If your org has a licensed
  mirror or artifact store, extend this script to pull from that private source
  and write WebPs into the target directory above.

This invocation printed instructions only; no files were downloaded.
`);
}

mkdirSync(OUT_DIR, { recursive: true });
printHelp();
process.exit(0);
