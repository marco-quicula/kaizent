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

  it("pages through more runs than fit on one page", async () => {
    const run = (n: number) => ({
      id: `00000000-0000-0000-0000-${String(n).padStart(12, "0")}`,
      ticketKey: `PC-${n}`,
      title: `Ticket ${n}`,
      status: "AWAITING_REVIEW",
      createdAt: "2026-10-01T10:00:00Z",
    });
    const fetchMock = mockApi({
      "GET /api/v1/runs": (request) => {
        const page = Number(new URL(request.url).searchParams.get("page"));
        return json({ items: [run(page === 0 ? 2 : 1)], page, size: 50, totalItems: 51 });
      },
    });
    renderConsole("/runs");
    expect(await screen.findByText("PC-2")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "2" }));

    expect(await screen.findByText("PC-1")).toBeInTheDocument();
    const pages = fetchMock.mock.calls.map(([r]) => new URL(r.url).searchParams.get("page"));
    expect(pages).toContain("1");
  });

  it("explains how to get a first run when there is none", async () => {
    mockApi({
      "GET /api/v1/runs": () => json({ items: [], page: 0, size: 50, totalItems: 0 }),
    });

    renderConsole("/runs");

    expect(await screen.findByText(/No runs yet/)).toBeInTheDocument();
  });
});
