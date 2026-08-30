/** Header Super Admin overrides must send for gated mutates. */
export const AUDIT_REASON_HEADER = "X-Audit-Reason";

export const MIN_AUDIT_REASON_LENGTH = 8;

/** Merge `X-Audit-Reason` into existing fetch headers. */
export function withAuditReason(
  headers: HeadersInit | undefined,
  reason: string,
): HeadersInit {
  const next = new Headers(headers);
  next.set(AUDIT_REASON_HEADER, reason.trim());
  return next;
}
