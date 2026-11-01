import createClient from "openapi-fetch";
import type { paths } from "./schema";

/** Typed client for the Kaizent REST API, generated from api/openapi.yaml (ADR-0024). */
export const api = createClient<paths>({
  // Same origin as the console; absolute so that it also works where Request needs full URLs.
  baseUrl: `${window.location.origin}/api/v1`,
  // Look up fetch on each call, so that tests can stub it.
  fetch: (request) => globalThis.fetch(request),
});
