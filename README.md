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
> [specs](docs/specs/README.md). Specs 0001 (project foundation) and 0002 (the walking
> skeleton, the first end-to-end cycle with a real model) are done; spec 0003 (the
> PetClinic demo workspace and the evaluation runner) is in progress.

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
console) on your machine. The application picks up the demo ticket
`examples/petclinic/inbox/PC-001.md`, asks a model for a proposed reply, and shows the run
in the console, where you can approve it; the approved reply is written to
`examples/petclinic/outbox/PC-001.reply.md`. Without a model configured, the run fails with
"GEMINI_API_KEY is not set", which the console shows; see
[Run with a real model](#run-with-a-real-model).

### Prerequisites

- **JDK 25**, for example [Eclipse Temurin](https://adoptium.net/). It must be the JDK
  that your shell uses: `JAVA_HOME` must point to it and its `bin` folder must come first
  in `PATH`. Both `./mvnw` (step 3) and `java -jar` (step 4) use it. Check with
  `java -version` and, on macOS or Linux, `echo $JAVA_HOME` (on Windows PowerShell,
  `echo $env:JAVA_HOME`); both must show version 25. The build stops with "Kaizent
  requires JDK 25 or newer" when an older JDK is active.
  - On **Windows**, if you installed the JDK by hand, open *Settings → System → About →
    Advanced system settings → Environment Variables*, set `JAVA_HOME` to the JDK folder
    (for example `C:\Program Files\Eclipse Adoptium\jdk-25...`), and put `%JAVA_HOME%\bin`
    at the top of `Path`. Package managers such as Scoop or winget usually do this for
    you. **Open a new terminal** afterwards: terminals that are already open keep the old
    values.
- **Docker** with Docker Compose.
- Git. You do not need Maven or Node.js: the Maven Wrapper (`./mvnw`) downloads Maven,
  and the build downloads Node 24 for the console.

On **Windows**, run the commands in **PowerShell**; each step shows the Windows variant
where a command differs. The repository's `.gitattributes` keeps LF line endings in the
checkout (CRLF only for Windows `.cmd` scripts), which the formatting checks require, even
when Git is configured with `core.autocrlf=true`.

### 1. Clone and configure

```bash
git clone https://github.com/marco-quicula/kaizent.git
cd kaizent
cp .env.example .env
```

On Windows (PowerShell), use `copy .env.example .env` for the last command.

Edit `.env` and set `KAIZENT_DB_PASSWORD` to a password of your choice for the local
database. `.env` is git-ignored; never commit it. Both Docker Compose and the
application read it from the repository root.

### 2. Start PostgreSQL

```bash
docker compose up -d
```

`docker compose ps` should show the `postgres` service as `healthy`. It listens on
`127.0.0.1:5432`.

The database volume keeps the password it was created with. If you change
`KAIZENT_DB_PASSWORD` after the first start (or start over with a new `.env`), recreate
the volume with `docker compose down -v` (this deletes the local data) and start it again;
otherwise the application fails with "password authentication failed".

### 3. Build

```bash
./mvnw verify
```

On Windows (PowerShell), use `.\mvnw.cmd verify`. `./mvnw` is the macOS and Linux (or Git
Bash) script and does not run in PowerShell.

This builds the console and the server and runs all the checks and tests. The tests start
their own PostgreSQL with Testcontainers, so Docker must be running. The first build takes
a few minutes while dependencies are downloaded. Use `./mvnw package -DskipTests`
(Windows: `.\mvnw.cmd package -DskipTests`) for a faster build without tests.

### 4. Run

From the repository root (so that the application finds `.env`), with the same JDK 25:

```bash
java -jar kaizent-server/target/kaizent-server-0.1.0-SNAPSHOT.jar
```

The same command works in PowerShell.

Then open:

- <http://localhost:8080/>: the console. **Runs** lists the runs; open one to see the
  ticket, the proposal and the **Approve** button. Lessons, Subjects and Intake come with
  later specs;
- <http://localhost:8080/actuator/health>: the health check, which reports `UP`
  including the database;
- <http://localhost:8080/api/v1/system>: the REST API, described by
  <http://localhost:8080/api/v1/openapi.yaml>.

The application listens on `127.0.0.1` only. Stop it with `Ctrl+C`, and stop the
database with `docker compose down`.

## Run with a real model

By default, Kaizent resolves tickets with the **Gemini API** (`gemini-api` adapter). Its
free tier is enough for the demo.

> **What leaves your machine:** the ticket title and body, and the prompt, are sent to the
> model provider. The demo tickets are synthetic. Do not put real customer or personal data
> in a ticket unless your agreement with the provider allows it.

1. Create an API key in [Google AI Studio](https://aistudio.google.com/apikey).
2. Add it to `.env` (never commit it):

   ```
   GEMINI_API_KEY=<your key>
   ```

3. Restart the application (`Ctrl+C`, then the `java -jar` command of step 4).
4. Kaizent processes each ticket **once**. If the tickets were already processed (for
   example, they failed before the key was set), start again with an empty database:

   ```bash
   docker compose down -v
   ```

   ```bash
   docker compose up -d
   ```

   Then restart the application. Retrying a failed run from the console comes in a later
   spec.
5. Open <http://localhost:8080/>. After a few seconds, the run for `PC-001` is **Awaiting
   review**. Open it, read the diagnosis and the suggested reply, and click **Approve**. The
   reply is written to `examples/petclinic/outbox/PC-001.reply.md`.

The model is set per pipeline step. The default for resolution is `gemini-3.6-flash`. To
use another model, set it in `.env`, for example:

```
KAIZENT_LLM_RESOLUTION_MODEL=gemini-2.5-flash
```

Free tiers have rate limits. If a run fails with a quota or rate-limit error, wait a minute
and start again as in item 4. Automatic retries come in a later spec.

To try the cycle without any model, for example offline, use the scripted `fake` adapter,
which returns a fixed reply:

```
KAIZENT_LLM_RESOLUTION_ADAPTER=fake
```

## Read more

| Document | What it covers |
|---|---|
| [docs/vision.md](docs/vision.md) | Long-term vision, principles and core concepts |
| [docs/roadmap.md](docs/roadmap.md) | Delivery waves: the MVP in detail, later waves at a high level |
| [docs/architecture.md](docs/architecture.md) | Wave 1 technical picture: modules, domain model, pipeline, API |
| [docs/specs/](docs/specs/README.md) | Spec-driven development process and the wave 1 specs |
| [docs/adr/](docs/adr/README.md) | Architecture Decision Records and the list of open decisions |
| [AGENTS.md](AGENTS.md) | Context and conventions for AI coding agents (Codex, Claude Code and others) |

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
