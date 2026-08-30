import { useCallback, useMemo } from "react";
import { useSearchParams } from "react-router-dom";

import type { PageParams } from "../types/api.ts";

const DEFAULT_PAGE = 0;
const DEFAULT_SIZE = 20;

export function usePagination(defaultSize = DEFAULT_SIZE) {
  const [searchParams, setSearchParams] = useSearchParams();

  const page = Number(searchParams.get("page") ?? DEFAULT_PAGE);
  const size = Number(searchParams.get("size") ?? defaultSize);
  const sort = searchParams.get("sort") ?? undefined;
  const q = searchParams.get("q") || undefined;
  const country = searchParams.get("country") || undefined;
  const city = searchParams.get("city") || undefined;
  const mosqueId = searchParams.get("mosqueId") || undefined;

  const params: PageParams = useMemo(
    () => ({ page, size, sort, q, country, city, mosqueId }),
    [page, size, sort, q, country, city, mosqueId],
  );

  const setPage = useCallback(
    (nextPage: number) => {
      setSearchParams((prev) => {
        const next = new URLSearchParams(prev);
        next.set("page", String(nextPage));
        return next;
      });
    },
    [setSearchParams],
  );

  const setSize = useCallback(
    (nextSize: number) => {
      setSearchParams((prev) => {
        const next = new URLSearchParams(prev);
        next.set("size", String(nextSize));
        next.set("page", "0");
        return next;
      });
    },
    [setSearchParams],
  );

  const setSort = useCallback(
    (nextSort: string | undefined) => {
      setSearchParams((prev) => {
        const next = new URLSearchParams(prev);
        if (nextSort) {
          next.set("sort", nextSort);
        } else {
          next.delete("sort");
        }
        next.set("page", "0");
        return next;
      });
    },
    [setSearchParams],
  );

  const setFilter = useCallback(
    (partial: Partial<Pick<PageParams, "q" | "country" | "city" | "mosqueId">>) => {
      setSearchParams(
        (prev) => {
          const next = new URLSearchParams(prev);
          for (const [key, value] of Object.entries(partial)) {
            if (value === undefined || value === "") {
              next.delete(key);
            } else {
              next.set(key, value);
            }
          }
          next.set("page", "0");
          return next;
        },
        { replace: true },
      );
    },
    [setSearchParams],
  );

  return {
    page,
    size,
    sort,
    q,
    country,
    city,
    mosqueId,
    params,
    setPage,
    setSize,
    setSort,
    setFilter,
  };
}
