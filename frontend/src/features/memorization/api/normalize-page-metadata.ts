import type { HalfPageEntry, PageMetadata } from "../../mushaf/types/index.ts";

/** BE pages/{page} returns HalfPageEntry[]; FE expects PageMetadata.halves. */
export function normalizePageMetadata(raw: unknown): PageMetadata {
  if (Array.isArray(raw)) {
    return { halves: raw as HalfPageEntry[] } as PageMetadata;
  }
  return raw as PageMetadata;
}
