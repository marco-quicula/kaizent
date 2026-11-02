import { vi } from "vitest";

type Handler = (request: Request) => Response | Promise<Response>;

/** JSON response helper. */
export function json(body: unknown, status = 200, type = "application/json"): Response {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": type } });
}

/**
 * Replaces fetch with handlers keyed by "METHOD /api/v1/path" (without the query string).
 * Unhandled requests answer 404. Returns the mock, to inspect the calls.
 */
export function mockApi(handlers: Record<string, Handler>) {
  const fetchMock = vi.fn<(request: Request) => Promise<Response>>(async (request) => {
    const url = new URL(request.url);
    const handler = handlers[`${request.method} ${url.pathname}`];
    return handler ? handler(request) : json({ title: "Not found" }, 404);
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}
