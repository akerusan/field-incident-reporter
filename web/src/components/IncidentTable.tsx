import Link from "next/link";
import { formatInstant } from "@/lib/format";
import { CATEGORY_LABELS, type Incident } from "@/lib/incidents";
import { SeverityBadge, StatusBadge } from "./Badges";

/** The triage list. Each title links to the incident. */
export function IncidentTable({ incidents }: { incidents: readonly Incident[] }) {
  if (incidents.length === 0) {
    return (
      <p className="rounded-lg border border-dashed border-slate-300 p-8 text-center text-sm text-slate-600 dark:border-slate-600 dark:text-slate-300">
        No incidents match these filters.
      </p>
    );
  }
  return (
    <div className="overflow-x-auto rounded-lg border border-slate-200 dark:border-slate-700">
      <table className="w-full text-left text-sm">
        <thead className="bg-slate-100 text-xs uppercase tracking-wide text-slate-600 dark:bg-slate-800 dark:text-slate-300">
          <tr>
            <th scope="col" className="px-3 py-2">Incident</th>
            <th scope="col" className="px-3 py-2">Severity</th>
            <th scope="col" className="px-3 py-2">Status</th>
            <th scope="col" className="px-3 py-2">Category</th>
            <th scope="col" className="px-3 py-2">Reported</th>
            <th scope="col" className="px-3 py-2 text-right">Photos</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-200 dark:divide-slate-700">
          {incidents.map((incident) => (
            <tr key={incident.id} className="hover:bg-slate-50 dark:hover:bg-slate-800/60">
              <td className="px-3 py-2">
                <Link href={`/incidents/${incident.id}`} className="font-medium text-blue-800 hover:underline dark:text-blue-300">
                  {incident.title}
                </Link>
                {incident.locationNote && (
                  <div className="text-xs text-slate-600 dark:text-slate-400">{incident.locationNote}</div>
                )}
              </td>
              <td className="px-3 py-2"><SeverityBadge severity={incident.severity} /></td>
              <td className="px-3 py-2"><StatusBadge status={incident.status} /></td>
              <td className="px-3 py-2">{CATEGORY_LABELS[incident.category]}</td>
              <td className="px-3 py-2 whitespace-nowrap">{formatInstant(incident.reportedAt)}</td>
              <td className="px-3 py-2 text-right tabular-nums">{incident.photos.length}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
