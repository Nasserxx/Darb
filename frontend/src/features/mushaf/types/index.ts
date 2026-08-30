export type AppLocale = "en" | "ar" | "de";

export type PageHalf = "A" | "B";

export type MushafEdition = "MADINAH_604";

export interface HalfPageEntry {
  id: string;
  edition: MushafEdition;
  page: number;
  half: PageHalf;
  juz: number;
  surahFrom: number;
  ayahFrom: number;
  surahTo: number;
  ayahTo: number;
}

export interface JuzIndexEntry {
  juz: number;
  pageFrom: number;
  pageTo: number;
  halfCount: number;
}

export interface PageMetadata {
  page: number;
  juz: number;
  halves: HalfPageEntry[];
}

export interface MushafMetadataResponse {
  edition: MushafEdition;
  totalPages: number;
  totalHalves: number;
  juzIndex: JuzIndexEntry[];
}

export interface MushafMap {
  edition: MushafEdition;
  halves: HalfPageEntry[];
}

export interface JuzMetadataResponse {
  juz: number;
  pageFrom: number;
  pageTo: number;
  halves: HalfPageEntry[];
}
