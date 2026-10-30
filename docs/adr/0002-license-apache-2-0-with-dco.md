# ADR-0002: License under Apache-2.0, with contributions under the DCO

- **Status:** Accepted
- **Date:** 2026-10-30

## Context

Kaizent is a public repository. The license affects adoption (many companies restrict
copyleft licenses such as the AGPL), contribution (CLAs deter some contributors) and the
ability to stop others from selling the project as a closed SaaS.

The options we evaluated:

| Option | Free use | Closed SaaS by third parties | OSI open source |
|---|---|---|---|
| Apache-2.0 / MIT | Yes | Allowed | Yes |
| AGPL-3.0 (optionally dual-licensed, with a CLA) | Yes | Only if the source is published | Yes |
| Functional Source License (FSL) | Yes | Forbidden for 2 years, then Apache/MIT | No |
| Elastic License 2.0 / BSL 1.1 | Yes | Forbidden as a managed service | No |
| PolyForm Noncommercial | Non-commercial only | Forbidden | No |

## Decision

- The project is licensed under the **Apache License 2.0**, the de facto standard in the
  Java and Spring ecosystem.
- Contributions are accepted under the **Developer Certificate of Origin (DCO)**: every
  commit is signed off (`git commit -s`). We do not require a CLA.

## Alternatives considered

- **AGPL-3.0 with a CLA.** It protects against closed SaaS use and keeps a dual-licensing
  option open, but it reduces corporate adoption and adds contribution friction.
  Rejected in favor of maximum adoption.
- **MIT.** Equally permissive, but it has no explicit patent grant. Apache-2.0 is
  preferred for that grant and for consistency with Spring.
- **Source-available licenses (FSL, ELv2, BSL, PolyForm).** The project could not be
  called open source, which works against community building.

## Consequences

- Anyone can use, modify and sell Kaizent, including as a SaaS, without sharing changes.
  We accept this trade-off in exchange for adoption.
- Every dependency must have a license compatible with Apache-2.0. Spring, Spring AI,
  LangChain4j and the MCP Java SDK are all Apache-2.0.
- Protecting the **name** is a separate concern, handled through trademark registration
  if it ever becomes necessary (see the open decisions).
- Changing to a more restrictive license later would only affect new versions and would
  likely cause a fork. Treat this decision as long-lived.
- `LICENSE` lives at the repository root. Contributors need DCO sign-off, which a CI
  check will enforce once code contributions open.
