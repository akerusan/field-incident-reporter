const dateFormat = new Intl.DateTimeFormat("en", { dateStyle: "medium", timeStyle: "short" });

/** A readable date and time for an ISO-8601 instant from the API. */
export function formatInstant(instant: string): string {
  return dateFormat.format(new Date(instant));
}
