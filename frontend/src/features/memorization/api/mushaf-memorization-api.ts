import { fetchData, mutateData } from "@/lib/api/pagination.ts";
import type {
  JuzMetadataResponse,
  MushafMetadataResponse,
  PageMetadata,
} from "@/features/mushaf/types/index.ts";
import {
  getHalvesForJuz,
  getLocalMushafMetadata,
  getPageMetadata,
} from "@/features/mushaf/lib/mushaf-map.ts";

import type {
  CoverageGrain,
  CoverageResponse,
  LessonAssignmentResponse,
  LessonAssignmentUpsertRequest,
  MemorizationAttemptCreateRequest,
  MemorizationAttemptResponse,
} from "../types/index.ts";
import { normalizePageMetadata } from "./normalize-page-metadata.ts";

const BASE = "/api/v1/memorization";

function buildQuery(params: Record<string, string | number | undefined>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== "") {
      search.set(key, String(value));
    }
  }
  const query = search.toString();
  return query ? `?${query}` : "";
}

async function fetchWithLocalFallback<T>(
  path: string,
  fallback: () => T,
): Promise<T> {
  try {
    return await fetchData<T>(path);
  } catch {
    return fallback();
  }
}

export const mushafMemorizationApi = {
  fetchMushafMetadata() {
    return fetchWithLocalFallback<MushafMetadataResponse>(
      `${BASE}/mushaf/madinah-604/metadata`,
      getLocalMushafMetadata,
    );
  },

  fetchJuzMetadata(juz: number) {
    return fetchWithLocalFallback<JuzMetadataResponse>(
      `${BASE}/mushaf/madinah-604/metadata/juz/${juz}`,
      () => {
        const halves = getHalvesForJuz(juz);
        const pageFrom = halves[0]?.page ?? 1;
        const pageTo = halves[halves.length - 1]?.page ?? pageFrom;
        return { juz, pageFrom, pageTo, halves };
      },
    );
  },

  async fetchPageMetadata(page: number): Promise<PageMetadata> {
    try {
      const data = await fetchData<unknown>(
        `${BASE}/mushaf/madinah-604/pages/${page}`,
      );
      return normalizePageMetadata(data);
    } catch {
      return getPageMetadata(page);
    }
  },

  fetchCoverage(studentId: string, circleId: string, grain: CoverageGrain) {
    return fetchData<CoverageResponse>(
      `${BASE}/students/${studentId}/coverage${buildQuery({ circleId, grain })}`,
    ).catch(() => ({
      grain,
      circleId,
      entries: [],
    }));
  },

  fetchAttempts(
    studentId: string,
    params: { circleId?: string; juz?: number; page?: number } = {},
  ) {
    return fetchData<MemorizationAttemptResponse[]>(
      `${BASE}/students/${studentId}/attempts${buildQuery(params)}`,
    );
  },

  fetchAttempt(
    studentId: string,
    page: number,
    half: string,
    circleId: string,
  ) {
    return fetchData<MemorizationAttemptResponse>(
      `${BASE}/students/${studentId}/attempts/${page}/${half}${buildQuery({ circleId })}`,
    ).catch(() => null);
  },

  createAttempt(studentId: string, body: MemorizationAttemptCreateRequest) {
    return mutateData<MemorizationAttemptResponse>(
      `${BASE}/students/${studentId}/attempts`,
      {
        method: "POST",
        body: JSON.stringify(body),
      },
    );
  },

  fetchLesson(studentId: string, circleId: string) {
    return fetchData<LessonAssignmentResponse>(
      `${BASE}/students/${studentId}/lesson${buildQuery({ circleId })}`,
    ).catch(() => null);
  },

  upsertLesson(studentId: string, body: LessonAssignmentUpsertRequest) {
    return mutateData<LessonAssignmentResponse>(
      `${BASE}/students/${studentId}/lesson`,
      {
        method: "PUT",
        body: JSON.stringify(body),
      },
    );
  },
};
