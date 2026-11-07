import { describe, expect, it } from "vitest";
import { applyView, countByStatus, parseView, triageActions, type Incident } from "./incidents";

function incident(overrides: Partial<Incident>): Incident {
  return {
    id: "id",
    title: "Broken pump",
    description: "",
    category: "EQUIPMENT",
    severity: "LOW",
    status: "OPEN",
    locationNote: "",
    photos: [],
    reportedAt: "2026-11-07T08:00:00Z",
    updatedAt: "2026-11-07T08:00:00Z",
    ...overrides,
  };
}

describe("parseView", () => {
  it("reads known values from the URL", () => {
    expect(parseView({ status: "RESOLVED", severity: "HIGH", category: "SAFETY", sort: "severity" })).toEqual({
      status: "RESOLVED",
      severity: "HIGH",
      category: "SAFETY",
      sort: "severity",
    });
  });

  it("ignores unknown values instead of trusting them", () => {
    expect(parseView({ status: "DELETED", sort: "random" })).toEqual({
      status: undefined,
      severity: undefined,
      category: undefined,
      sort: "newest",
    });
  });

  it("uses the first value of a repeated parameter", () => {
    expect(parseView({ status: ["OPEN", "RESOLVED"] }).status).toBe("OPEN");
  });
});

describe("applyView", () => {
  const older = incident({ id: "older", severity: "CRITICAL", reportedAt: "2026-11-06T08:00:00Z" });
  const newer = incident({ id: "newer", severity: "LOW", status: "RESOLVED", reportedAt: "2026-11-07T08:00:00Z" });

  it("lists newest first by default", () => {
    expect(applyView([older, newer], parseView({})).map((i) => i.id)).toEqual(["newer", "older"]);
  });

  it("can put the most severe first", () => {
    expect(applyView([newer, older], parseView({ sort: "severity" })).map((i) => i.id)).toEqual(["older", "newer"]);
  });

  it("filters by status, severity and category together", () => {
    expect(applyView([older, newer], parseView({ status: "RESOLVED" })).map((i) => i.id)).toEqual(["newer"]);
    expect(applyView([older, newer], parseView({ severity: "CRITICAL", category: "PLUMBING" }))).toEqual([]);
  });

  it("doesn't reorder its input", () => {
    const input = [older, newer];
    applyView(input, parseView({}));
    expect(input.map((i) => i.id)).toEqual(["older", "newer"]);
  });
});

describe("countByStatus", () => {
  it("counts every status, including ones with no incidents", () => {
    expect(countByStatus([incident({ status: "OPEN" }), incident({ status: "OPEN" })])).toEqual({
      OPEN: 2,
      IN_PROGRESS: 0,
      RESOLVED: 0,
    });
  });
});

describe("triageActions", () => {
  it("lets an operator start, resolve and reopen", () => {
    expect(triageActions("OPEN").map((a) => a.status)).toEqual(["IN_PROGRESS", "RESOLVED"]);
    expect(triageActions("IN_PROGRESS").map((a) => a.status)).toEqual(["RESOLVED", "OPEN"]);
    expect(triageActions("RESOLVED").map((a) => a.status)).toEqual(["OPEN"]);
  });
});
