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

  const params: PageParams = useMemo(
    () => ({ page, size, sort }),
    [page, size, sort],
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

  return { page, size, sort, params, setPage, setSize, setSort };
}
