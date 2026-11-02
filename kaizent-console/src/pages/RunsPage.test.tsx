import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { json, mockApi } from "../test/api";
import { renderConsole } from "../test/render";

const RUN_ID = "11111111-1111-1111-1111-111111111111";

describe("Runs page", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("lists the runs with their ticket, title and status", async () => {
    mockApi({
      "GET /api/v1/runs": () =>
        json({
          items: [
            {
              id: RUN_ID,
              ticketKey: "PC-001",
              title: "Cannot save an owner",
              status: "AWAITING_REVIEW",
              createdAt: "2026-10-01T10:00:00Z",
            },
          ],
          page: 0,
          size: 50,
          totalItems: 1,
        }),
    });

    renderConsole("/runs");

    expect(await screen.findByText("PC-001")).toBeInTheDocument();
    expect(screen.getByText("Cannot save an owner")).toBeInTheDocument();
    expect(screen.getByText("Awaiting review")).toBeInTheDocument();
  });

  it("opens a run when its row is clicked", async () => {
    mockApi({
      "GET /api/v1/runs": () =>
        json({
          items: [
            {
              id: RUN_ID,
              ticketKey: "PC-001",
              title: "Cannot save an owner",
              status: "RECEIVED",
              createdAt: "2026-10-01T10:00:00Z",
            },
          ],
          page: 0,
          size: 50,
          totalItems: 1,
        }),
    });
    const { router } = renderConsole("/runs");

    await userEvent.click(await screen.findByText("PC-001"));

    expect(router.state.location.pathname).toBe(`/runs/${RUN_ID}`);
  });

  it("explains how to get a first run when there is none", async () => {
    mockApi({
      "GET /api/v1/runs": () => json({ items: [], page: 0, size: 50, totalItems: 0 }),
    });

    renderConsole("/runs");

    expect(await screen.findByText(/No runs yet/)).toBeInTheDocument();
  });
});
