const KEY = "darb.sa.lastMosque";

export type LastMosque = { id: string; name: string };

export function readLastMosque(): LastMosque | null {
  try {
    const raw = sessionStorage.getItem(KEY);
    if (!raw) {
      return null;
    }
    const parsed = JSON.parse(raw) as unknown;
    if (
      typeof parsed !== "object" ||
      parsed === null ||
      typeof (parsed as LastMosque).id !== "string" ||
      typeof (parsed as LastMosque).name !== "string"
    ) {
      return null;
    }
    return { id: (parsed as LastMosque).id, name: (parsed as LastMosque).name };
  } catch {
    return null;
  }
}

export function writeLastMosque(m: LastMosque): void {
  try {
    sessionStorage.setItem(KEY, JSON.stringify(m));
  } catch {
    // sessionStorage may be unavailable (private mode / quota)
  }
}

export function clearLastMosque(): void {
  try {
    sessionStorage.removeItem(KEY);
  } catch {
    // ignore
  }
}
