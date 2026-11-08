import Link from "next/link";
import { notFound } from "next/navigation";
import { getIncident, photoUrl } from "@/api/client";
import { SeverityBadge, StatusBadge } from "@/components/Badges";
import { formatInstant } from "@/lib/format";
import { CATEGORY_LABELS, triageActions } from "@/lib/incidents";
import { setIncidentStatus } from "./actions";

/** One incident: what the technician reported, and the operator's triage actions. */
export default async function IncidentPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const incident = await getIncident(id);
  if (!incident) notFound();

  return (
    <article className="flex flex-col gap-6">
      <Link href="/" className="text-sm text-blue-800 hover:underline dark:text-blue-300">
        ← All incidents
      </Link>

      <header className="flex flex-col gap-2">
        <h1 className="text-2xl font-semibold">{incident.title}</h1>
        <div className="flex flex-wrap gap-2">
          <SeverityBadge severity={incident.severity} />
          <StatusBadge status={incident.status} />
        </div>
      </header>

      <section aria-labelledby="triage" className="flex flex-col gap-2">
        <h2 id="triage" className="text-sm font-semibold uppercase tracking-wide text-slate-600 dark:text-slate-400">
          Triage
        </h2>
        <div className="flex flex-wrap gap-2">
          {triageActions(incident.status).map((action) => (
            <form key={action.status} action={setIncidentStatus.bind(null, incident.id, action.status)}>
              <button
                type="submit"
                className="rounded-md border border-slate-300 px-3 py-1.5 text-sm font-medium hover:bg-slate-100 dark:border-slate-600 dark:hover:bg-slate-800"
              >
                {action.label}
              </button>
            </form>
          ))}
        </div>
        {incident.status === "RESOLVED" && (
          <p className="text-sm text-slate-600 dark:text-slate-400">
            Resolved incidents are read-only for technicians. Reopen it to let them change it again.
          </p>
        )}
      </section>

      <dl className="grid gap-4 sm:grid-cols-2">
        <Field label="Description">{incident.description || "No description"}</Field>
        <Field label="Location">{incident.locationNote || "Not given"}</Field>
        <Field label="Category">{CATEGORY_LABELS[incident.category]}</Field>
        <Field label="Reported">{formatInstant(incident.reportedAt)}</Field>
        <Field label="Last updated">{formatInstant(incident.updatedAt)}</Field>
      </dl>

      {incident.photos.length > 0 && (
        <section aria-labelledby="photos" className="flex flex-col gap-2">
          <h2 id="photos" className="text-sm font-semibold uppercase tracking-wide text-slate-600 dark:text-slate-400">
            Photos
          </h2>
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
            {incident.photos.map((photo, index) => (
              <a key={photo.id} href={photoUrl(photo.path)} target="_blank" rel="noreferrer">
                {/* A plain img: photos come from the local mock API, which next/image would need configuring for. */}
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img
                  src={photoUrl(photo.path)}
                  alt={`Photo ${index + 1} of ${incident.title}`}
                  className="aspect-[4/3] w-full rounded-lg object-cover"
                />
              </a>
            ))}
          </div>
        </section>
      )}
    </article>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <dt className="text-xs font-medium text-slate-600 dark:text-slate-400">{label}</dt>
      <dd className="mt-1">{children}</dd>
    </div>
  );
}
