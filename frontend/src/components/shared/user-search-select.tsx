import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";

import { Input } from "@/components/ui/input.tsx";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select.tsx";
import {
  useUser,
  useUserSearch,
} from "@/features/users/hooks/use-users.ts";
import type { UserResponse } from "@/features/users/types/index.ts";
import { formatShortId } from "@/lib/format/ids.ts";
import { cn } from "@/lib/utils.ts";

function userDisplayName(user: Pick<UserResponse, "id" | "fullName">): string {
  const name = user.fullName?.trim();
  return name ? name : formatShortId(user.id);
}

export function UserSearchSelect({
  value,
  onValueChange,
  placeholder,
  className,
}: {
  value: string;
  onValueChange: (userId: string) => void;
  placeholder?: string;
  className?: string;
}) {
  const { t } = useTranslation("app");
  const [query, setQuery] = useState("");

  const { data: searchPage, isFetching } = useUserSearch(query);
  const { data: selectedUser } = useUser(value, { enabled: Boolean(value) });

  const options = useMemo(() => {
    const seen = new Set<string>();
    const users: UserResponse[] = [];
    for (const user of [selectedUser, ...(searchPage?.content ?? [])]) {
      if (user && !seen.has(user.id)) {
        seen.add(user.id);
        users.push(user);
      }
    }
    return users;
  }, [selectedUser, searchPage?.content]);

  const fallbackLabel =
    value && !options.some((user) => user.id === value)
      ? formatShortId(value)
      : null;

  return (
    <div className={cn("flex flex-col gap-2", className)}>
      <Input
        value={query}
        onChange={(event) => setQuery(event.target.value)}
        placeholder={placeholder ?? t("users.searchPlaceholder")}
      />
      <Select
        value={value}
        onValueChange={(next) => {
          onValueChange(next);
          setQuery("");
        }}
      >
        <SelectTrigger>
          <SelectValue placeholder={placeholder ?? t("users.selectUser")} />
        </SelectTrigger>
        <SelectContent>
          {options.map((user) => (
            <SelectItem key={user.id} value={user.id}>
              {userDisplayName(user)}
            </SelectItem>
          ))}
          {fallbackLabel ? (
            <SelectItem key={value} value={value}>
              {fallbackLabel}
            </SelectItem>
          ) : null}
          {options.length === 0 && !fallbackLabel ? (
            <SelectItem value="__no-results__" disabled>
              {isFetching ? t("users.searching") : t("users.noResults")}
            </SelectItem>
          ) : null}
        </SelectContent>
      </Select>
    </div>
  );
}
