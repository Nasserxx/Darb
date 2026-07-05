import type { ReactNode } from "react";
import { useTranslation } from "react-i18next";

import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import type { PageResponse } from "@/lib/types/api.ts";

type Column<T> = {
  id: string;
  header: string;
  cell: (row: T) => ReactNode;
  className?: string;
};

type DataTableProps<T> = {
  columns: Column<T>[];
  data?: PageResponse<T>;
  isLoading?: boolean;
  emptyMessage?: string;
  onPageChange?: (page: number) => void;
};

export function DataTable<T>({
  columns,
  data,
  isLoading,
  emptyMessage,
  onPageChange,
}: DataTableProps<T>) {
  const { t } = useTranslation("app");
  const rows = data?.content ?? [];

  if (isLoading) {
    return (
      <div className="flex flex-col gap-2">
        {Array.from({ length: 5 }).map((_, index) => (
          <Skeleton key={index} className="h-10 w-full" />
        ))}
      </div>
    );
  }

  if (rows.length === 0) {
    return (
      <p className="rounded-lg border border-dashed border-border px-4 py-8 text-center text-sm text-muted-foreground">
        {emptyMessage ?? t("table.empty")}
      </p>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="overflow-hidden rounded-lg border border-border">
        <Table>
          <TableHeader>
            <TableRow>
              {columns.map((column) => (
                <TableHead key={column.id} className={column.className}>
                  {column.header}
                </TableHead>
              ))}
            </TableRow>
          </TableHeader>
          <TableBody>
            {rows.map((row, rowIndex) => (
              <TableRow key={rowIndex} className="[content-visibility:auto]">
                {columns.map((column) => (
                  <TableCell key={column.id} className={column.className}>
                    {column.cell(row)}
                  </TableCell>
                ))}
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
      {data && data.totalPages > 1 && onPageChange ? (
        <div className="flex items-center justify-between gap-4">
          <p className="text-sm text-muted-foreground">
            {t("table.pageInfo", {
              page: data.pageNumber + 1,
              total: data.totalPages,
              count: data.totalElements,
            })}
          </p>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={data.pageNumber <= 0}
              onClick={() => onPageChange(data.pageNumber - 1)}
            >
              {t("table.previous")}
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={data.last}
              onClick={() => onPageChange(data.pageNumber + 1)}
            >
              {t("table.next")}
            </Button>
          </div>
        </div>
      ) : null}
    </div>
  );
}
