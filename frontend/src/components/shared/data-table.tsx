import type { ReactNode } from "react";
import { useTranslation } from "react-i18next";

import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
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

const DEFAULT_PAGE_SIZE_OPTIONS = [20, 100];

function getVisiblePageNumbers(currentPage: number, totalPages: number): number[] {
  if (totalPages <= 0) return [];
  if (totalPages <= 7) {
    return Array.from({ length: totalPages }, (_, i) => i + 1);
  }

  const current = currentPage + 1;
  const pages = new Set<number>([1, totalPages]);
  for (let i = current - 1; i <= current + 1; i++) {
    if (i >= 1 && i <= totalPages) pages.add(i);
  }
  return [...pages].sort((a, b) => a - b);
}

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
  pageSize?: number;
  onSizeChange?: (size: number) => void;
  pageSizeOptions?: number[];
};

export function DataTable<T>({
  columns,
  data,
  isLoading,
  emptyMessage,
  onPageChange,
  pageSize,
  onSizeChange,
  pageSizeOptions = DEFAULT_PAGE_SIZE_OPTIONS,
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

  const showFooter =
    data && onPageChange && (data.totalPages > 1 || onSizeChange);
  const visiblePages = data
    ? getVisiblePageNumbers(data.pageNumber, data.totalPages)
    : [];

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
      {showFooter ? (
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div className="flex flex-wrap items-center gap-4">
            <p className="text-sm text-muted-foreground">
              {t("table.pageInfo", {
                page: data.pageNumber + 1,
                total: data.totalPages,
                count: data.totalElements,
              })}
            </p>
            {onSizeChange ? (
              <div className="flex items-center gap-2">
                <span className="text-sm text-muted-foreground">
                  {t("table.rowsPerPage")}
                </span>
                <Select
                  value={String(pageSize ?? pageSizeOptions[0])}
                  onValueChange={(value) => onSizeChange(Number(value))}
                >
                  <SelectTrigger className="h-8 w-[4.5rem]">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {pageSizeOptions.map((option) => (
                      <SelectItem key={option} value={String(option)}>
                        {option}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            ) : null}
          </div>
          {data.totalPages > 1 ? (
            <div className="flex items-center gap-1">
              <Button
                variant="outline"
                size="sm"
                disabled={data.pageNumber <= 0}
                onClick={() => onPageChange(data.pageNumber - 1)}
              >
                {t("table.previous")}
              </Button>
              {visiblePages.flatMap((page, index) => {
                const items: ReactNode[] = [];
                if (index > 0 && visiblePages[index - 1]! < page - 1) {
                  items.push(
                    <span
                      key={`ellipsis-${page}`}
                      className="px-1 text-sm text-muted-foreground"
                      aria-hidden
                    >
                      …
                    </span>,
                  );
                }
                items.push(
                  <Button
                    key={page}
                    variant={page === data.pageNumber + 1 ? "default" : "outline"}
                    size="sm"
                    className="min-w-8 px-2"
                    onClick={() => onPageChange(page - 1)}
                  >
                    {page}
                  </Button>,
                );
                return items;
              })}
              <Button
                variant="outline"
                size="sm"
                disabled={data.last}
                onClick={() => onPageChange(data.pageNumber + 1)}
              >
                {t("table.next")}
              </Button>
            </div>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}
