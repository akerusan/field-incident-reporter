import Link from "next/link";
import { STATUSES, STATUS_LABELS, type IncidentStatus } from "@/lib/incidents";

/** Counts per status across all incidents; each one links to that status filter. */
export function StatusSummary({ counts }: { counts: Record<IncidentStatus, number> }) {
  return (
    <ul className="grid grid-cols-3 gap-3">
      {STATUSES.map((status) => (
        <li key={status}>
          <Link
            href={`/?status=${status}`}
            className="block rounded-lg border border-slate-200 p-3 hover:border-blue-400 dark:border-slate-700 dark:hover:border-blue-500"
          >
            <div className="text-2xl font-semibold tabular-nums">{counts[status]}</div>
            <div className="text-xs text-slate-600 dark:text-slate-300">{STATUS_LABELS[status]}</div>
          </Link>
        </li>
      ))}
    </ul>
  );
}
