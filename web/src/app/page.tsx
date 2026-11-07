import { listIncidents } from "@/api/client";
import { FilterBar } from "@/components/FilterBar";
import { IncidentTable } from "@/components/IncidentTable";
import { StatusSummary } from "@/components/StatusSummary";
import { applyView, countByStatus, parseView } from "@/lib/incidents";

/** The triage list: every incident, filtered and sorted from the URL. */
export default async function IncidentsPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const view = parseView(await searchParams);
  const incidents = await listIncidents();
  const visible = applyView(incidents, view);

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-semibold">Incidents</h1>
      <StatusSummary counts={countByStatus(incidents)} />
      <FilterBar view={view} />
      <p className="text-sm text-slate-600 dark:text-slate-400" aria-live="polite">
        Showing {visible.length} of {incidents.length} incidents
      </p>
      <IncidentTable incidents={visible} />
    </div>
  );
}
