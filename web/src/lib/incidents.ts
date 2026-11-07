import type { components } from "@/api/types";

/** Types generated from server/openapi.yaml: the dashboard and the app share one contract. */
export type Incident = components["schemas"]["Incident"];
export type IncidentStatus = components["schemas"]["IncidentStatus"];
export type IncidentSeverity = components["schemas"]["IncidentSeverity"];
export type IncidentCategory = components["schemas"]["IncidentCategory"];

export const STATUSES: readonly IncidentStatus[] = ["OPEN", "IN_PROGRESS", "RESOLVED"];
export const SEVERITIES: readonly IncidentSeverity[] = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];
export const CATEGORIES: readonly IncidentCategory[] = [
  "EQUIPMENT",
  "ROAD_HAZARD",
  "ELECTRICAL",
  "PLUMBING",
  "SAFETY",
  "OTHER",
];

export const STATUS_LABELS: Record<IncidentStatus, string> = {
  OPEN: "Open",
  IN_PROGRESS: "In progress",
  RESOLVED: "Resolved",
};

export const SEVERITY_LABELS: Record<IncidentSeverity, string> = {
  LOW: "Low",
  MEDIUM: "Medium",
  HIGH: "High",
  CRITICAL: "Critical",
};

export const CATEGORY_LABELS: Record<IncidentCategory, string> = {
  EQUIPMENT: "Equipment",
  ROAD_HAZARD: "Road hazard",
  ELECTRICAL: "Electrical",
  PLUMBING: "Plumbing",
  SAFETY: "Safety",
  OTHER: "Other",
};

export type SortOrder = "newest" | "severity";

/** What the list shows, read from the URL so a filtered view can be bookmarked or shared. */
export interface IncidentView {
  status?: IncidentStatus;
  severity?: IncidentSeverity;
  category?: IncidentCategory;
  sort: SortOrder;
}

type SearchParams = Record<string, string | string[] | undefined>;

function pick<T extends string>(value: string | string[] | undefined, allowed: readonly T[]): T | undefined {
  const single = Array.isArray(value) ? value[0] : value;
  return allowed.find((option) => option === single);
}

/** Reads the view from URL search params. Unknown values are ignored rather than trusted. */
export function parseView(params: SearchParams): IncidentView {
  return {
    status: pick(params.status, STATUSES),
    severity: pick(params.severity, SEVERITIES),
    category: pick(params.category, CATEGORIES),
    sort: pick(params.sort, ["newest", "severity"] as const) ?? "newest",
  };
}

const SEVERITY_RANK: Record<IncidentSeverity, number> = { CRITICAL: 0, HIGH: 1, MEDIUM: 2, LOW: 3 };

/** Filters and sorts incidents for the list. Newest first; "severity" puts the most urgent first. */
export function applyView(incidents: readonly Incident[], view: IncidentView): Incident[] {
  const byNewest = (a: Incident, b: Incident) => Date.parse(b.reportedAt) - Date.parse(a.reportedAt);
  return incidents
    .filter((incident) => !view.status || incident.status === view.status)
    .filter((incident) => !view.severity || incident.severity === view.severity)
    .filter((incident) => !view.category || incident.category === view.category)
    .toSorted((a, b) =>
      view.sort === "severity"
        ? SEVERITY_RANK[a.severity] - SEVERITY_RANK[b.severity] || byNewest(a, b)
        : byNewest(a, b),
    );
}

/** How many incidents are in each status, for the summary above the list. */
export function countByStatus(incidents: readonly Incident[]): Record<IncidentStatus, number> {
  const counts: Record<IncidentStatus, number> = { OPEN: 0, IN_PROGRESS: 0, RESOLVED: 0 };
  for (const incident of incidents) counts[incident.status]++;
  return counts;
}

export interface TriageAction {
  status: IncidentStatus;
  label: string;
}

/**
 * The status changes an operator can make. Only operators resolve incidents (technicians can't),
 * and a resolved incident can be reopened.
 */
export function triageActions(status: IncidentStatus): TriageAction[] {
  switch (status) {
    case "OPEN":
      return [
        { status: "IN_PROGRESS", label: "Start work" },
        { status: "RESOLVED", label: "Resolve" },
      ];
    case "IN_PROGRESS":
      return [
        { status: "RESOLVED", label: "Resolve" },
        { status: "OPEN", label: "Back to open" },
      ];
    case "RESOLVED":
      return [{ status: "OPEN", label: "Reopen" }];
  }
}

export function isIncidentStatus(value: unknown): value is IncidentStatus {
  return typeof value === "string" && (STATUSES as readonly string[]).includes(value);
}
