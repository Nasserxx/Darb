import type { PageParams } from "@/lib/types/api.ts";
import type { UserPickerParams } from "../types/index.ts";

export const userKeys = {
  all: ["users"] as const,
  lists: () => [...userKeys.all, "list"] as const,
  list: (params: PageParams = {}) => [...userKeys.lists(), params] as const,
  search: (query: string) => [...userKeys.all, "search", query] as const,
  picker: (params: UserPickerParams) =>
    [...userKeys.all, "picker", params] as const,
  pickerStates: (country: string) =>
    [...userKeys.all, "pickerStates", country] as const,
  pickerCities: (country: string, state?: string) =>
    [...userKeys.all, "pickerCities", country, state ?? ""] as const,
  details: () => [...userKeys.all, "detail"] as const,
  detail: (id: string) => [...userKeys.details(), id] as const,
  me: () => [...userKeys.all, "me"] as const,
};
