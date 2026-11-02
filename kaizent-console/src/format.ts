import type { RunStatus } from "./api/types";

/** A date and time in the reviewer's locale, for example "Oct 1, 2026, 10:00 AM". */
export function formatDateTime(value: string): string {
  return new Date(value).toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" });
}

/** A run status for people, for example "Awaiting review". */
export function statusLabel(status: RunStatus): string {
  const words = status.toLowerCase().split("_").join(" ");
  return words.charAt(0).toUpperCase() + words.slice(1);
}
