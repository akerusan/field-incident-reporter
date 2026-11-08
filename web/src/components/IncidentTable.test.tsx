import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import type { Incident } from "@/lib/incidents";
import { IncidentTable } from "./IncidentTable";

const incident: Incident = {
  id: "9c3d7e12",
  title: "Exposed wiring in junction box",
  description: "",
  category: "ELECTRICAL",
  severity: "CRITICAL",
  status: "IN_PROGRESS",
  locationNote: "Corridor 2F east",
  photos: [{ id: "p1", path: "/uploads/wiring-1.jpg" }],
  reportedAt: "2026-11-07T01:45:00Z",
  updatedAt: "2026-11-07T01:45:00Z",
};

describe("IncidentTable", () => {
  it("shows each incident with its severity, status and a link to it", () => {
    render(<IncidentTable incidents={[incident]} />);

    const row = screen.getByRole("row", { name: /Exposed wiring/ });
    expect(within(row).getByRole("link", { name: "Exposed wiring in junction box" })).toHaveAttribute(
      "href",
      "/incidents/9c3d7e12",
    );
    expect(within(row).getByText("Critical")).toBeInTheDocument();
    expect(within(row).getByText("In progress")).toBeInTheDocument();
    expect(within(row).getByText("Electrical")).toBeInTheDocument();
    expect(within(row).getByText("Corridor 2F east")).toBeInTheDocument();
  });

  it("says so when nothing matches the filters", () => {
    render(<IncidentTable incidents={[]} />);

    expect(screen.getByText("No incidents match these filters.")).toBeInTheDocument();
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });
});
