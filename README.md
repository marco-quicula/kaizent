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

## Getting started

The application runs locally: PostgreSQL in Docker, and the server (which also serves the
console) on your machine. There is no domain behavior yet: the console shell and the
System page are what you can see so far.

### Prerequisites

- **JDK 25**, for example [Eclipse Temurin](https://adoptium.net/).
- **Docker** with Docker Compose.
- Git. You do not need Maven or Node.js: the Maven Wrapper (`./mvnw`) downloads Maven,
  and the build downloads Node 24 for the console.

### 1. Clone and configure

```bash
git clone https://github.com/marco-quicula/kaizent.git
cd kaizent
cp .env.example .env
```

Edit `.env` and set `KAIZENT_DB_PASSWORD` to a password of your choice for the local
database. `.env` is git-ignored; never commit it. Both Docker Compose and the
application read it from the repository root.

### 2. Start PostgreSQL

```bash
docker compose up -d
```

`docker compose ps` should show the `postgres` service as `healthy`. It listens on
`127.0.0.1:5432`.

If you change `KAIZENT_DB_PASSWORD` after the first start, recreate the database volume
with `docker compose down -v` (this deletes the local data), then start it again.

### 3. Build

```bash
./mvnw verify
```

This builds the console and the server and runs all the checks and tests. The tests start
their own PostgreSQL with Testcontainers, so Docker must be running. The first build takes
a few minutes while dependencies are downloaded. Use `./mvnw package -DskipTests` for a
faster build without tests.

### 4. Run

From the repository root (so that the application finds `.env`):

```bash
java -jar kaizent-server/target/kaizent-server-0.1.0-SNAPSHOT.jar
```

Then open:

- <http://localhost:8080/>: the console, with the Runs, Lessons, Subjects, Intake and
  System pages (only System shows data for now);
- <http://localhost:8080/actuator/health>: the health check, which reports `UP`
  including the database;
- <http://localhost:8080/api/v1/system>: the REST API, described by
  <http://localhost:8080/api/v1/openapi.yaml>.

The application listens on `127.0.0.1` only. Stop it with `Ctrl+C`, and stop the
database with `docker compose down`.

## Read more

| Document | What it covers |
|---|---|
| [docs/vision.md](docs/vision.md) | Long-term vision, principles and core concepts |
| [docs/roadmap.md](docs/roadmap.md) | Delivery waves: the MVP in detail, later waves at a high level |
| [docs/architecture.md](docs/architecture.md) | Wave 1 technical picture: modules, domain model, pipeline, API |
| [docs/specs/](docs/specs/README.md) | Spec-driven development process and the wave 1 specs |
| [docs/adr/](docs/adr/README.md) | Architecture Decision Records and the list of open decisions |
| [CLAUDE.md](CLAUDE.md) | Context and conventions for AI-assisted development sessions |

## Stack

Java 25 · Maven · Spring Boot 4 · Spring AI · Spring Modulith · PostgreSQL 18 · React +
TypeScript (Vite, Mantine). See the [ADRs](docs/adr/README.md) for the reasoning behind
each choice.

## Contributing

Contributions are welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md) first: Kaizent uses
spec-driven development, and every commit must be signed off under the
[Developer Certificate of Origin](https://developercertificate.org/) (`git commit -s`).
Please follow the [Code of Conduct](CODE_OF_CONDUCT.md), and report security problems as
described in [SECURITY.md](SECURITY.md).

## Repository settings (maintainer)

These settings live on GitHub, not in the repository. Check them when setting up a fork
that should behave like the main repository:

- [ ] **DCO:** the [DCO GitHub App](https://github.com/apps/dco) is installed on the
  repository.
- [ ] **Secret scanning and push protection:** enabled in Settings → Advanced Security.
- [ ] **Private vulnerability reporting:** enabled in Settings → Advanced Security, so
  that SECURITY.md's "Report a vulnerability" works.
- [ ] **Dependabot alerts:** enabled in Settings → Advanced Security. Version updates come
  from `.github/dependabot.yml`.
- [ ] **Merge button:** only **Allow squash merging** is enabled, and **Automatically
  delete head branches** is on (Settings → General → Pull Requests).
- [ ] **Ruleset for `main`** (Settings → Rules → Rulesets): require a pull request before
  merging, allow only squash merges, block force pushes and deletions, and **require
  status checks to pass** with the `DCO` check and the CI check `Build and test`. A check
  can only be selected after it has run once on a pull request.

## License

[Apache License 2.0](LICENSE).
