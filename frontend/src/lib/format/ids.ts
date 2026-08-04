export function formatShortId(id: string): string {
  return `${id.slice(0, 8)}…`;
}
