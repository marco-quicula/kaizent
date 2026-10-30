# Architecture Decision Records

This folder records the significant decisions about Kaizent, together with the reasons
behind them. The format is described in [ADR-0001](0001-record-architecture-decisions.md).

## Accepted decisions

| ADR | Decision |
|---|---|
| [0001](0001-record-architecture-decisions.md) | Record architecture decisions as ADRs |
| [0002](0002-license-apache-2-0-with-dco.md) | License under Apache-2.0, with contributions under the DCO |
| [0003](0003-project-name-and-coordinates.md) | Name: Kaizent; coordinates `br.com.refatorado.kaizent` |
| [0004](0004-language-policy.md) | English for all repository content |
| [0005](0005-backend-stack-java-spring.md) | Java 25, Maven, Spring Boot 4, Spring AI |
| [0006](0006-modular-monolith-hexagonal.md) | Modular monolith with ports and adapters (Spring Modulith) |
| [0007](0007-llm-gateway-provider-neutral.md) | Provider-neutral LLM access with cost tracking and budgets |
| [0008](0008-api-first-react-console.md) | API-first backend with a React + TypeScript SPA console |
| [0009](0009-postgresql-persistence.md) | PostgreSQL (Flyway) for operational data; pgvector later |
| [0010](0010-skills-agent-skills-format-in-git.md) | Skills in the Agent Skills format, versioned in git |
| [0011](0011-markdown-knowledge-before-vectors.md) | Markdown knowledge in git before vector retrieval |
| [0012](0012-human-in-the-loop-and-autonomy-levels.md) | Human in the loop; autonomy levels earned per subject |
| [0013](0013-demo-domain-spring-petclinic.md) | Spring PetClinic as the demo domain, with synthetic data |
| [0014](0014-file-based-connectors-for-mvp.md) | File-based connectors and a canonical problem model for the MVP |

## Open decisions

These questions are known but not decided yet. Each one becomes an ADR when it is
resolved. The "Needed by" column says when the answer is required.

| # | Question | Needed by | Notes |
|---|---|---|---|
| 1 | Agent loop: plain Spring AI ChatClient with tool calling, or Embabel on top of Spring AI? | Wave 1 start | Spike both on one demo ticket before committing. |
| 2 | Exact versions of Spring Boot 4.x and Spring AI | Wave 1 start | Pin the latest compatible GA releases. |
| 3 | Maven layout: one module with Spring Modulith packages, or multiple Maven modules (core, adapters, app, console)? | Wave 1 start | |
| 4 | Default LLM provider and models for development and CI | Wave 1 start | Weigh cost against quality; consider a cheap model for triage. |
| 5 | Text search for `search_knowledge`: in-memory (for example, Lucene) or PostgreSQL full-text search? | Wave 1 | |
| 6 | How confidence is computed: self-reported by the model, evidence-based, or a combination | Wave 1 | Recorded from day one; calibrated against verdicts later. |
| 7 | Ticket file format (front matter fields) and the shape of the canonical `Problem` model | Wave 1 | |
| 8 | Layout of the deployment configuration repository (roles, skills, knowledge, inbox/outbox) | Wave 1 | Inside the main repository for the demo, separate for real deployments. |
| 9 | Frontend tooling details: Node version, component library, API client generation from OpenAPI | Wave 1 | |
| 10 | CI platform and checks (GitHub Actions; build, tests, DCO, license headers, secret scanning) | Wave 1 | |
| 11 | Redaction of personal data before prompts leave the system | Before the first real connector (wave 2) | |
| 12 | Thresholds and minimum sample sizes for the autonomy levels | Wave 4 | Data collected from wave 1. |
| 13 | Delivery of skill improvement proposals: branch, pull request, or both; which git hosting APIs | Wave 2 | |
| 14 | Embedding model(s) and the chunking strategy | Wave 3 | Local ONNX option vs. cloud APIs. |
| 15 | Trademark search and registration for "Kaizent" | Before public promotion | |
