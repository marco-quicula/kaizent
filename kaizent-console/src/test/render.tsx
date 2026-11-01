import { QueryClient } from "@tanstack/react-query";
import { render } from "@testing-library/react";
import { createMemoryRouter } from "react-router";
import { App } from "../App";
import { routes } from "../routes";

/** Renders the whole console at the given path. */
export function renderConsole(path: string) {
  const router = createMemoryRouter(routes, { initialEntries: [path] });
  // No retries, so that error states show up immediately.
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return { router, ...render(<App router={router} queryClient={queryClient} />) };
}
