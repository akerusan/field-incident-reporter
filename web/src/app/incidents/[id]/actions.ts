"use server";

import { revalidatePath } from "next/cache";
import { updateIncidentStatus } from "@/api/client";
import { isIncidentStatus } from "@/lib/incidents";

/**
 * Triage: change an incident's status. A server action is a public endpoint, so its arguments are
 * validated like any request body.
 */
export async function setIncidentStatus(id: string, status: string): Promise<void> {
  if (!isIncidentStatus(status)) throw new Error(`Unknown status: ${status}`);
  await updateIncidentStatus(id, status);
  revalidatePath("/");
  revalidatePath(`/incidents/${id}`);
}
