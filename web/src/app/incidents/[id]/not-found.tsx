import Link from "next/link";

export default function IncidentNotFound() {
  return (
    <div className="flex flex-col items-start gap-3">
      <h1 className="text-lg font-semibold">Incident not found</h1>
      <p className="text-sm text-slate-600 dark:text-slate-400">It may have been deleted by the technician.</p>
      <Link href="/" className="text-sm text-blue-800 hover:underline dark:text-blue-300">
        ← All incidents
      </Link>
    </div>
  );
}
