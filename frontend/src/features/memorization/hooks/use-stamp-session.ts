import { useCallback, useMemo, useState } from "react";

import type { MemorizationStamp, StampType } from "../types/index.ts";

const MAX_STAMPS = 50;

export interface StampSessionState {
  stamps: MemorizationStamp[];
  activeType: StampType;
  tajweedCount: number;
  hifzCount: number;
  canAdd: boolean;
}

export interface UseStampSessionOptions {
  initialStamps?: MemorizationStamp[];
  readonly?: boolean;
}

export function useStampSession(options: UseStampSessionOptions = {}) {
  const { initialStamps = [], readonly = false } = options;
  const [stamps, setStamps] = useState<MemorizationStamp[]>(initialStamps);
  const [activeType, setActiveType] = useState<StampType>("TAJWEED");

  const counts = useMemo(() => {
    let tajweedCount = 0;
    let hifzCount = 0;
    for (const stamp of stamps) {
      if (stamp.type === "TAJWEED") tajweedCount++;
      else hifzCount++;
    }
    return { tajweedCount, hifzCount };
  }, [stamps]);

  const canAdd = !readonly && stamps.length < MAX_STAMPS;

  const addStamp = useCallback(
    (stamp: Omit<MemorizationStamp, "type"> & { type?: StampType }) => {
      if (readonly || stamps.length >= MAX_STAMPS) return false;
      setStamps((prev) => [
        ...prev,
        {
          type: stamp.type ?? activeType,
          surah: stamp.surah,
          ayah: stamp.ayah,
          x: stamp.x,
          y: stamp.y,
        },
      ]);
      return true;
    },
    [activeType, readonly, stamps.length],
  );

  const addImageStamp = useCallback(
    (x: number, y: number, surah: number, ayah: number) => {
      return addStamp({ surah, ayah, x, y });
    },
    [addStamp],
  );

  const addAyahStamp = useCallback(
    (surah: number, ayah: number) => {
      return addStamp({ surah, ayah });
    },
    [addStamp],
  );

  const undoLast = useCallback(() => {
    if (readonly) return;
    setStamps((prev) => prev.slice(0, -1));
  }, [readonly]);

  const resetStamps = useCallback((next: MemorizationStamp[] = []) => {
    setStamps(next);
  }, []);

  const state: StampSessionState = {
    stamps,
    activeType,
    ...counts,
    canAdd,
  };

  return {
    ...state,
    setActiveType,
    addStamp,
    addImageStamp,
    addAyahStamp,
    undoLast,
    resetStamps,
  };
}

export function stampKey(stamp: Pick<MemorizationStamp, "surah" | "ayah">): string {
  return `${stamp.surah}:${stamp.ayah}`;
}
