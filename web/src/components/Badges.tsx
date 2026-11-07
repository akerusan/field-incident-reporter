import { SEVERITY_LABELS, STATUS_LABELS, type IncidentSeverity, type IncidentStatus } from "@/lib/incidents";

// Same meaning as in the app: severity colors are fixed, and the top two levels add a symbol so
// color isn't the only cue.

const SEVERITY_STYLES: Record<IncidentSeverity, string> = {
  LOW: "bg-green-100 text-green-900 dark:bg-green-900/50 dark:text-green-100",
  MEDIUM: "bg-amber-100 text-amber-900 dark:bg-amber-900/50 dark:text-amber-100",
  HIGH: "bg-orange-100 text-orange-900 dark:bg-orange-900/50 dark:text-orange-100",
  CRITICAL: "bg-red-100 text-red-900 dark:bg-red-900/60 dark:text-red-100",
};

const STATUS_STYLES: Record<IncidentStatus, string> = {
  OPEN: "bg-slate-200 text-slate-800 dark:bg-slate-700 dark:text-slate-100",
  IN_PROGRESS: "bg-blue-100 text-blue-900 dark:bg-blue-900/50 dark:text-blue-100",
  RESOLVED: "bg-green-100 text-green-900 dark:bg-green-900/50 dark:text-green-100",
};

const pill = "inline-flex items-center gap-1 rounded-md px-2 py-0.5 text-xs font-medium whitespace-nowrap";

export function SeverityBadge({ severity }: { severity: IncidentSeverity }) {
  const symbol = severity === "CRITICAL" ? "▲" : severity === "HIGH" ? "!" : null;
  return (
    <span className={`${pill} ${SEVERITY_STYLES[severity]}`}>
      {symbol && <span aria-hidden="true">{symbol}</span>}
      {SEVERITY_LABELS[severity]}
    </span>
  );
}

export function StatusBadge({ status }: { status: IncidentStatus }) {
  return (
    <span className={`${pill} ${STATUS_STYLES[status]}`}>
      {status === "RESOLVED" && <span aria-hidden="true">✓</span>}
      {STATUS_LABELS[status]}
    </span>
  );
}
