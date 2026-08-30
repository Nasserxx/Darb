import { Combobox as ComboboxPrimitive } from "@base-ui/react/combobox";
import { CheckIcon, ChevronDownIcon, XIcon } from "lucide-react";
import { useMemo, useState } from "react";

import { cn } from "@/lib/utils.ts";

export type ComboboxOption = { value: string; label: string };

type ComboboxProps = {
  options: ComboboxOption[];
  value: string | null;
  onValueChange: (value: string | null) => void;
  placeholder?: string;
  className?: string;
  id?: string;
  disabled?: boolean;
  searchPlaceholder?: string;
  emptyLabel?: string;
  /** When set, empty filter shows an action to commit the typed query. */
  onCreateValue?: (value: string) => void;
  createLabel?: (query: string) => string;
  clearAriaLabel?: string;
};

export function Combobox({
  options,
  value,
  onValueChange,
  placeholder,
  className,
  id,
  disabled,
  searchPlaceholder,
  emptyLabel,
  onCreateValue,
  createLabel,
  clearAriaLabel,
}: ComboboxProps) {
  const [inputValue, setInputValue] = useState("");
  const items = useMemo(() => options.map((option) => option.value), [options]);
  const labelByValue = useMemo(
    () => new Map(options.map((option) => [option.value, option.label])),
    [options],
  );
  const selected = options.find((option) => option.value === value);
  const trimmedQuery = inputValue.trim();
  const showCreate = Boolean(onCreateValue) && trimmedQuery.length > 0;

  return (
    <ComboboxPrimitive.Root
      items={items}
      value={value}
      onValueChange={onValueChange}
      itemToStringLabel={(itemValue) => labelByValue.get(itemValue) ?? itemValue}
      id={id}
      disabled={disabled}
      onInputValueChange={setInputValue}
    >
      <ComboboxPrimitive.Trigger
        type="button"
        data-slot="combobox-trigger"
        className={cn(
          "flex h-10 w-full items-center justify-between gap-2 rounded-lg border border-input bg-card px-3 py-2 text-sm whitespace-nowrap text-foreground shadow-xs transition-colors outline-none focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/30 disabled:cursor-not-allowed disabled:opacity-50 [&_svg]:opacity-60",
          className,
        )}
      >
        <span
          dir="auto"
          className={cn("truncate", !selected && "text-muted-foreground")}
        >
          {selected?.label ?? placeholder ?? "Select…"}
        </span>
        {value != null ? (
          <span
            role="button"
            tabIndex={0}
            aria-label={clearAriaLabel ?? "Clear"}
            className="shrink-0 opacity-60 hover:opacity-100"
            onPointerDown={(event) => {
              event.preventDefault();
              event.stopPropagation();
              onValueChange(null);
            }}
            onClick={(event) => {
              event.preventDefault();
              event.stopPropagation();
              onValueChange(null);
            }}
            onKeyDown={(event) => {
              if (event.key !== "Enter" && event.key !== " ") return;
              event.preventDefault();
              event.stopPropagation();
              onValueChange(null);
            }}
          >
            <XIcon className="size-4" />
          </span>
        ) : null}
        <ChevronDownIcon className="size-4 opacity-60" />
      </ComboboxPrimitive.Trigger>
      <ComboboxPrimitive.Portal>
        <ComboboxPrimitive.Positioner sideOffset={4} className="z-50">
          <ComboboxPrimitive.Popup
            data-slot="combobox-popup"
            className="w-(--anchor-width) min-w-32 overflow-hidden rounded-lg border border-border bg-popover p-1 text-popover-foreground shadow-md"
          >
            <ComboboxPrimitive.Input
              data-slot="combobox-input"
              placeholder={searchPlaceholder ?? "Search…"}
              className="mb-1 flex h-10 w-full min-w-0 rounded-lg border border-input bg-card px-3 py-2 text-sm text-foreground shadow-xs outline-none placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/30"
            />
            <ComboboxPrimitive.Empty>
              {showCreate ? (
                <button
                  type="button"
                  className="flex w-full cursor-default items-center rounded-md px-2 py-1.5 text-start text-sm outline-none data-highlighted:bg-muted data-highlighted:text-foreground hover:bg-muted"
                  onClick={() => {
                    onCreateValue?.(trimmedQuery);
                  }}
                >
                  <span dir="auto">
                    {createLabel?.(trimmedQuery) ??
                      `Use "${trimmedQuery}"`}
                  </span>
                </button>
              ) : (
                <span className="block px-2 py-1.5 text-sm text-muted-foreground">
                  {emptyLabel ?? "No results"}
                </span>
              )}
            </ComboboxPrimitive.Empty>
            <ComboboxPrimitive.List className="max-h-64 overflow-auto">
              {(item: string) => (
                <ComboboxPrimitive.Item
                  key={item}
                  value={item}
                  data-slot="combobox-item"
                  className="flex w-full cursor-default items-center justify-between gap-2 rounded-md px-2 py-1.5 text-sm outline-none select-none data-highlighted:bg-muted data-highlighted:text-foreground data-disabled:pointer-events-none data-disabled:opacity-50"
                >
                  <span dir="auto" className="truncate">
                    {labelByValue.get(item) ?? item}
                  </span>
                  <ComboboxPrimitive.ItemIndicator className="shrink-0">
                    <CheckIcon className="size-4" />
                  </ComboboxPrimitive.ItemIndicator>
                </ComboboxPrimitive.Item>
              )}
            </ComboboxPrimitive.List>
          </ComboboxPrimitive.Popup>
        </ComboboxPrimitive.Positioner>
      </ComboboxPrimitive.Portal>
    </ComboboxPrimitive.Root>
  );
}
