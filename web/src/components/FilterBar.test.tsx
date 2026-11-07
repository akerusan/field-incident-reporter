import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { parseView } from "@/lib/incidents";
import { FilterBar } from "./FilterBar";

describe("FilterBar", () => {
  it("shows the current view, so the form reflects the URL", () => {
    render(<FilterBar view={parseView({ status: "RESOLVED", sort: "severity" })} />);

    expect(screen.getByLabelText("Status")).toHaveValue("RESOLVED");
    expect(screen.getByLabelText("Severity")).toHaveValue("");
    expect(screen.getByLabelText("Sort")).toHaveValue("severity");
  });

  it("is a GET form, so a filtered list has a shareable URL", () => {
    render(<FilterBar view={parseView({})} />);

    expect(screen.getByRole("form", { name: "Filter incidents" })).toHaveAttribute("method", "get");
  });
});
