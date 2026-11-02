import { screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { json, mockApi } from "../test/api";
import { renderConsole } from "../test/render";

describe("console shell", () => {
  beforeEach(() => {
    mockApi({ "GET /api/v1/runs": () => json({ items: [], page: 0, size: 50, totalItems: 0 }) });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("shows the product name and the placeholder navigation", () => {
    renderConsole("/runs");

    expect(screen.getByRole("heading", { name: "Kaizent" })).toBeInTheDocument();
    const nav = screen.getByRole("navigation", { name: "Main navigation" });
    for (const label of ["Runs", "Lessons", "Subjects", "Intake", "System"]) {
      expect(nav).toHaveTextContent(label);
    }
  });

  it("redirects / to the Runs page", async () => {
    const { router } = renderConsole("/");

    expect(await screen.findByRole("heading", { name: "Runs" })).toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/runs");
  });
});
