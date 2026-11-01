import { screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { renderConsole } from "../test/render";

describe("System page", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("shows the version from GET /api/v1/system", async () => {
    const fetchMock = vi.fn<(request: Request) => Promise<Response>>(
      async () =>
        new Response(JSON.stringify({ version: "1.2.3", workspace: null, profiles: [] }), {
          headers: { "Content-Type": "application/json" },
        }),
    );
    vi.stubGlobal("fetch", fetchMock);

    renderConsole("/system");

    expect(await screen.findByText("1.2.3")).toBeInTheDocument();
    expect(screen.getByText("Not configured")).toBeInTheDocument();
    const request = fetchMock.mock.calls[0]?.[0];
    expect(new URL(request?.url ?? "").pathname).toBe("/api/v1/system");
  });

  it("shows an error when the API fails", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => new Response("boom", { status: 500 })),
    );

    renderConsole("/system");

    expect(await screen.findByText("Could not load the system information.")).toBeInTheDocument();
  });
});
