import type { Incident, IncidentStatus } from "@/lib/incidents";

/** The mock API (server/). Read on the server for data, and in the browser for photo URLs. */
export const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:3000";

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status?: number,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function request(path: string, init?: RequestInit): Promise<Response> {
  try {
    // Triage changes what everyone sees, so never serve a cached list.
    return await fetch(`${API_URL}${path}`, { cache: "no-store", ...init });
  } catch {
    throw new ApiError(`Can't reach the API at ${API_URL}.`);
  }
}

async function failure(response: Response): Promise<ApiError> {
  const body = (await response.json().catch(() => null)) as { message?: string } | null;
  return new ApiError(body?.message ?? `The API answered ${response.status}.`, response.status);
}

/** All incidents, newest first. Filtering happens in the dashboard so the summary counts stay global. */
export async function listIncidents(): Promise<Incident[]> {
  const response = await request("/incidents?_sort=reportedAt&_order=desc");
  if (!response.ok) throw await failure(response);
  return (await response.json()) as Incident[];
}

/** One incident, or null if it doesn't exist (any more). */
export async function getIncident(id: string): Promise<Incident | null> {
  const response = await request(`/incidents/${encodeURIComponent(id)}`);
  if (response.status === 404) return null;
  if (!response.ok) throw await failure(response);
  return (await response.json()) as Incident;
}

/** Operator triage: changes only the status (PATCH), so the technician's content is untouched. */
export async function updateIncidentStatus(id: string, status: IncidentStatus): Promise<Incident> {
  const response = await request(`/incidents/${encodeURIComponent(id)}`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ status, updatedAt: new Date().toISOString() }),
  });
  if (!response.ok) throw await failure(response);
  return (await response.json()) as Incident;
}

/** Photo paths from the API are relative (`/uploads/…`); resolve them against the API's URL. */
export function photoUrl(path: string): string {
  return new URL(path, API_URL).toString();
}
