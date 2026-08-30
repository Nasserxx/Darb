import { useState } from "react";

import type { PageHalf } from "../types/index.ts";

const PLACEHOLDER = "/mushaf/madinah-604/placeholder.svg";

type MushafPageImageProps = {
  page: number;
  half?: PageHalf;
  className?: string;
  alt?: string;
};

/**
 * Renders a mushaf page image. Tries `{page}.webp` first, then falls back to placeholder.
 * Production requires licensed WebP assets under public/mushaf/madinah-604/.
 */
export function MushafPageImage({ page, half, className, alt }: MushafPageImageProps) {
  const [src, setSrc] = useState(`/mushaf/madinah-604/${page}.webp`);
  const label = alt ?? `Mushaf page ${page}${half ? ` (${half})` : ""}`;

  return (
    <img
      src={src}
      alt={label}
      className={className}
      loading="lazy"
      decoding="async"
      onError={() => {
        if (src !== PLACEHOLDER) {
          setSrc(PLACEHOLDER);
        }
      }}
    />
  );
}
