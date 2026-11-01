# Kaizent

**An agentic problem-solving tool that learns from every cycle.**

Kaizent (*kaizen* + *agent*) receives a problem, such as a support ticket with a stack
trace or a question about a system, and works out who should handle it. It then picks
the right role, skills, tools and knowledge, proposes a resolution with a confidence
level, and learns from how a human reviews that proposal. Every resolution runs through
a PDCA (Plan-Do-Check-Act) cycle, and agents earn autonomy per subject as their track
record improves.

> **Status: wave 1 in progress.** The vision, the roadmap and the architecture decisions
> are documented, and the first wave (the MVP) is broken down into approved
> [specs](docs/specs/README.md). Implementation has started with spec 0001 (project
> foundation); there is no domain behavior yet.

## How it works (target for the MVP)

```
ticket file ─► triage ─► role ─► skills + knowledge ─► agent proposes ─► human reviews ─► reply file
   (Plan)                                                  (Do)            (Check)             │
                                                                                               ▼
                                                 lessons + metrics per subject ◄────────── (Act)
```

- **Pluggable everywhere.** Ticket sources, reply sinks, skill repositories, knowledge
  sources, tools (MCP) and LLM access are ports with swappable adapters, chosen at setup.
  Models can be reached through provider APIs or through locally installed agent CLI
  tools such as Codex.
- **Human in the loop first.** At the start, a person confirms every answer. Human
  approvals, edits and rejections are both the safety net and the success metric.
- **Graduated autonomy.** Each subject gets a level (Junior → Mid-level → Senior →
  Specialist) that is earned from measured outcomes, never assumed.
- **Starts simple.** Markdown files in git come first. Vector search, MCP tools and
  fine-tuning arrive in later waves.

## Read more

| Document | What it covers |
|---|---|
| [docs/vision.md](docs/vision.md) | Long-term vision, principles and core concepts |
| [docs/roadmap.md](docs/roadmap.md) | Delivery waves: the MVP in detail, later waves at a high level |
| [docs/architecture.md](docs/architecture.md) | Wave 1 technical picture: modules, domain model, pipeline, API |
| [docs/specs/](docs/specs/README.md) | Spec-driven development process and the wave 1 specs |
| [docs/adr/](docs/adr/README.md) | Architecture Decision Records and the list of open decisions |
| [CLAUDE.md](CLAUDE.md) | Context and conventions for AI-assisted development sessions |

## Planned stack

Java 25 · Maven · Spring Boot 4 · Spring AI · Spring Modulith · PostgreSQL · React +
TypeScript. See the [ADRs](docs/adr/README.md) for the reasoning behind each choice.

## Contributing

The project is not yet ready for code contributions. Feedback on the vision, the roadmap
and the open decisions is welcome through GitHub issues. Contributions will use the
[Developer Certificate of Origin](https://developercertificate.org/) (`git commit -s`);
see [ADR-0002](docs/adr/0002-license-apache-2-0-with-dco.md).

## License

[Apache License 2.0](LICENSE).
