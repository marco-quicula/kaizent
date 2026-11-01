# ADR-0025: Console tooling

- **Status:** Accepted
- **Date:** 2026-11-01

## Context

ADR-0008 chose a React + TypeScript SPA built with Vite. We need the rest of the
toolchain, chosen to be simple for contributors who mostly know Java.

## Decision

- **Node.js 24 (LTS)**. The Maven build downloads Node and npm through the frontend Maven
  plugin, so no global installation is needed to build. Console developers can use a
  local Node 24 with `npm run dev`, which proxies API calls to the running server.
- **Mantine** as the component library: complete ready-made components and forms, with
  no Tailwind setup.
- **TanStack Query** for server state and **React Router** for navigation.
- **`openapi-typescript` + `openapi-fetch`** for a typed API client generated from
  `api/openapi.yaml` (ADR-0024).
- **Vitest + React Testing Library** for tests, and **ESLint + Prettier** for linting and
  formatting.
- **npm** as the package manager, with `package-lock.json` committed.

## Alternatives considered

- **shadcn/ui + Tailwind.** Popular and flexible, but it needs more setup and CSS work.
- **MUI.** Mature, but heavier and more opinionated in its styling.
- **pnpm or Yarn.** Not needed for a single package.

## Consequences

- All the libraries above are MIT licensed, which is compatible with Apache-2.0.
- Console contributors need no backend knowledge beyond the OpenAPI contract.
