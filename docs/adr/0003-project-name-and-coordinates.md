# ADR-0003: Project name and coordinates

- **Status:** Accepted
- **Date:** 2026-10-30

## Context

The repository was created as "OmniSolve", but a commercial product with that name
already exists. We need a distinctive name before publishing any documentation, along
with stable Maven coordinates and a Java base package.

## Decision

- The project is called **Kaizent** (*kaizen*, continuous improvement, + *agent*). The
  name reflects the PDCA cycle at the core of the product.
- Home: `kaizent.refatorado.com.br`, a subdomain of a domain the maintainer owns.
- Maven groupId and Java base package: **`br.com.refatorado.kaizent`**.
- The GitHub repository will be renamed from `omnisolve` to `kaizent`. GitHub keeps
  redirects from the old name.

## Alternatives considered

- **Keeping "OmniSolve".** This risks a trademark conflict and confusion with an existing
  product.
- **Other names** (Resolva, Shewhart, Triagent, Ciclo). Kaizent was preferred because it
  ties directly to continuous improvement and was not found in use.
- **An `io.github.<user>` groupId.** Rejected because owning a domain gives stable
  coordinates that do not depend on a GitHub account name.

## Consequences

- All documentation, code and artifacts use "Kaizent" and `br.com.refatorado.kaizent`.
- The name was checked informally. A formal trademark search and registration remain
  open (see the open decisions).
