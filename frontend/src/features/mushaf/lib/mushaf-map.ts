import madinah604 from "../data/madinah-604.json";
import { SURAH_AYAH_COUNTS } from "../data/surah-ayah-counts.ts";
import { JUZ_PAGE_STARTS, TOTAL_MUSHAF_PAGES, getJuzForPage, getJuzPageRange } from "../data/juz-page-starts.ts";
import type {
  HalfPageEntry,
  JuzIndexEntry,
  MushafEdition,
  MushafMap,
  MushafMetadataResponse,
  PageMetadata,
} from "../types/index.ts";

const map = madinah604 as MushafMap;

const halfByKey = new Map<string, HalfPageEntry>(
  map.halves.map((half) => [`${half.page}-${half.half}`, half]),
);

const SURAH_LENGTHS = SURAH_AYAH_COUNTS;

function ayahPosition(surah: number, ayah: number): number {
  return surah * 1000 + ayah;
}

export function getEdition(): MushafEdition {
  return map.edition;
}

export function getAllHalves(): readonly HalfPageEntry[] {
  return map.halves;
}

export function getHalfPage(page: number, half: "A" | "B"): HalfPageEntry {
  const entry = halfByKey.get(`${page}-${half}`);
  if (!entry) {
    throw new Error(`Half-page not found: ${page}-${half}`);
  }
  return entry;
}

export function getHalvesForJuz(juz: number): HalfPageEntry[] {
  const { from, to } = getJuzPageRange(juz);
  const halves: HalfPageEntry[] = [];
  for (let page = from; page <= to; page++) {
    halves.push(getHalfPage(page, "A"), getHalfPage(page, "B"));
  }
  return halves;
}

export function isAyahInHalf(
  page: number,
  half: "A" | "B",
  surah: number,
  ayah: number,
): boolean {
  const entry = getHalfPage(page, half);
  const pos = ayahPosition(surah, ayah);
  const from = ayahPosition(entry.surahFrom, entry.ayahFrom);
  const to = ayahPosition(entry.surahTo, entry.ayahTo);
  return pos >= from && pos <= to;
}

export function getPageMetadata(page: number): PageMetadata {
  return {
    page,
    juz: getJuzForPage(page),
    halves: [getHalfPage(page, "A"), getHalfPage(page, "B")],
  };
}

export function getLocalMushafMetadata(): MushafMetadataResponse {
  const juzIndex: JuzIndexEntry[] = JUZ_PAGE_STARTS.map((pageFrom, index) => {
    const juz = index + 1;
    const { to: pageTo } = getJuzPageRange(juz);
    return {
      juz,
      pageFrom,
      pageTo,
      halfCount: (pageTo - pageFrom + 1) * 2,
    };
  });

  return {
    edition: map.edition,
    totalPages: TOTAL_MUSHAF_PAGES,
    totalHalves: map.halves.length,
    juzIndex,
  };
}

export function expandAyahRange(entry: HalfPageEntry): Array<{ surah: number; ayah: number }> {
  const ayahs: Array<{ surah: number; ayah: number }> = [];
  let surah = entry.surahFrom;
  let ayah = entry.ayahFrom;

  while (true) {
    ayahs.push({ surah, ayah });
    if (surah === entry.surahTo && ayah === entry.ayahTo) {
      break;
    }
    const maxAyah = SURAH_LENGTHS[surah - 1]!;
    if (ayah < maxAyah) {
      ayah += 1;
    } else {
      surah += 1;
      ayah = 1;
    }
  }

  return ayahs;
}

export function mushafPageImageUrl(page: number): string {
  return `/mushaf/madinah-604/${page}.webp`;
}
