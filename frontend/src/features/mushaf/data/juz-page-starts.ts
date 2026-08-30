/** Standard Madinah 604-page mushaf juz start pages (juz 1 → page 1, juz 30 → page 582). */
export const JUZ_PAGE_STARTS: readonly number[] = [
  1, 22, 42, 62, 82, 102, 122, 142, 162, 182, 201, 221, 242, 262, 282, 302,
  322, 342, 362, 382, 402, 422, 442, 462, 482, 502, 522, 542, 562, 582,
] as const;

export const TOTAL_MUSHAF_PAGES = 604;

export function getJuzForPage(page: number): number {
  for (let j = JUZ_PAGE_STARTS.length - 1; j >= 0; j--) {
    if (page >= JUZ_PAGE_STARTS[j]!) {
      return j + 1;
    }
  }
  return 1;
}

export function getJuzPageRange(juz: number): { from: number; to: number } {
  const index = juz - 1;
  if (index < 0 || index >= JUZ_PAGE_STARTS.length) {
    throw new Error(`Invalid juz: ${juz}`);
  }
  const from = JUZ_PAGE_STARTS[index]!;
  const to =
    index + 1 < JUZ_PAGE_STARTS.length
      ? JUZ_PAGE_STARTS[index + 1]! - 1
      : TOTAL_MUSHAF_PAGES;
  return { from, to };
}

export function getPagesInJuz(juz: number): number[] {
  const { from, to } = getJuzPageRange(juz);
  return Array.from({ length: to - from + 1 }, (_, i) => from + i);
}

export function halfPageId(page: number, half: "A" | "B"): string {
  return `${page}-${half}`;
}

export function parseHalfPageId(id: string): { page: number; half: "A" | "B" } {
  const [pageStr, half] = id.split("-");
  const page = Number(pageStr);
  if (!pageStr || Number.isNaN(page) || (half !== "A" && half !== "B")) {
    throw new Error(`Invalid half-page id: ${id}`);
  }
  return { page, half };
}
