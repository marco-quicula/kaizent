import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { RunDetail } from "../api/types";
import { json, mockApi } from "../test/api";
import { renderConsole } from "../test/render";

const RUN_ID = "11111111-1111-1111-1111-111111111111";
const PATH = `/api/v1/runs/${RUN_ID}`;

const awaitingReview: RunDetail = {
  id: RUN_ID,
  status: "AWAITING_REVIEW",
  problem: {
    ticketKey: "PC-001",
    source: "file-inbox",
    title: "Cannot save an owner",
    body: "The telephone field rejects (555) 123-4567.",
    labels: [],
    requester: "Ana",
    createdAt: "2026-10-01T10:00:00Z",
    receivedAt: "2026-10-01T10:01:00Z",
  },
  proposal: {
    diagnosis: "The field accepts digits only.",
    suggestedReply: "Hello Ana, type 5551234567.",
    rawConfidence: 0.8,
  },
  createdAt: "2026-10-01T10:01:00Z",
  updatedAt: "2026-10-01T10:02:00Z",
};

describe("Run detail page", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("shows the problem and the proposal", async () => {
    mockApi({ [`GET ${PATH}`]: () => json(awaitingReview) });

    renderConsole(`/runs/${RUN_ID}`);

    expect(await screen.findByText("PC-001: Cannot save an owner")).toBeInTheDocument();
    expect(screen.getByText("The telephone field rejects (555) 123-4567.")).toBeInTheDocument();
    expect(screen.getByText("The field accepts digits only.")).toBeInTheDocument();
    expect(screen.getByText("Hello Ana, type 5551234567.")).toBeInTheDocument();
    expect(screen.getByText("Confidence 80%")).toBeInTheDocument();
    expect(screen.getByText("Awaiting review")).toBeInTheDocument();
  });

  it("approves the run and shows the new status", async () => {
    const delivered: RunDetail = {
      ...awaitingReview,
      status: "DELIVERED",
      verdict: {
        type: "APPROVED",
        finalReply: "Hello Ana, type 5551234567.",
        reviewer: "local",
        reviewedAt: "2026-10-01T10:03:00Z",
      },
    };
    let approved = false;
    const fetchMock = mockApi({
      [`GET ${PATH}`]: () => json(approved ? delivered : awaitingReview),
      [`POST ${PATH}/verdict`]: () => {
        approved = true;
        return json(delivered);
      },
      "GET /api/v1/runs": () => json({ items: [], page: 0, size: 50, totalItems: 0 }),
    });
    renderConsole(`/runs/${RUN_ID}`);

    await userEvent.click(await screen.findByRole("button", { name: "Approve" }));

    expect(await screen.findByText("Delivered")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Approve" })).not.toBeInTheDocument();
    const post = fetchMock.mock.calls.map(([request]) => request).find((r) => r.method === "POST");
    expect(await post?.json()).toEqual({ type: "APPROVED" });
  });

  it("shows why an approval was refused", async () => {
    mockApi({
      [`GET ${PATH}`]: () => json(awaitingReview),
      [`POST ${PATH}/verdict`]: () =>
        json(
          { title: "Run is not awaiting review", status: 409, detail: "Run is DELIVERED" },
          409,
          "application/problem+json",
        ),
    });
    renderConsole(`/runs/${RUN_ID}`);

    await userEvent.click(await screen.findByRole("button", { name: "Approve" }));

    expect(await screen.findByText("Run is DELIVERED")).toBeInTheDocument();
  });

  it("does not offer Approve for a failed run and shows the failure", async () => {
    mockApi({
      [`GET ${PATH}`]: () =>
        json({
          ...awaitingReview,
          status: "FAILED",
          proposal: undefined,
          failure: { reason: "ERROR", message: "GEMINI_API_KEY is not set" },
        }),
    });

    renderConsole(`/runs/${RUN_ID}`);

    expect(await screen.findByText("GEMINI_API_KEY is not set")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Approve" })).not.toBeInTheDocument();
  });
});
