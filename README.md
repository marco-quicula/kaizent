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
> [specs](docs/specs/README.md). Specs 0001 (project foundation), 0002 (the walking
> skeleton, the first end-to-end cycle with a real model) and 0003 (the PetClinic demo
> workspace and the evaluation runner) are done; spec 0004 (LLM access) is in progress.

## How it works (target for the MVP)

```
ticket file ─► triage ─► role ─► skills + knowledge ─► agent proposes ─► human reviews ─► reply file
   (Plan)                                                  (Do)            (Check)             │
                                                                                               ▼
                                                 lessons + metrics per subject ◄────────── (Act)
```

- **Pluggable everywhere.** Ticket sources, reply sinks, skill repositories, knowledge
  sources, tools (MCP) and LLM access are ports with swappable adapters, chosen at setup.
  Models can be reached through provider APIs or, for single structured calls, through
  CLI tools such as Codex, run in containers built locally.
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

The resolution step needs an **API key** (the Gemini free tier, OpenAI or Anthropic): the
agent loop that resolves a ticket runs on an API model, and the agentic CLI runtime is
planned for wave 2 (ADR-0033). A CLI login can serve the single calls (triage, lessons and
the model check): see [Run with a CLI tool](#run-with-a-cli-tool-local-personal-mode).

The model is set per pipeline step. The default for resolution is `gemini-3.6-flash`. To
use another model, set it in `.env`, for example:

```
KAIZENT_LLM_RESOLUTION_MODEL=gemini-2.5-flash
```

Free tiers have rate limits. A model call that hits a rate limit (HTTP 429) or a
transient server error (5xx) is retried up to 3 times, waiting 2, then 4 seconds, or
longer when the provider asks for it. When the provider asks for more than 60 seconds
(for example, a daily quota), Kaizent does not wait and the run fails at once. The
failure shows the provider's cause: the HTTP status, the provider's error code and
message, and the wait it asked for. Change these limits under `kaizent.llm.retry`
(`max-attempts`, `initial-backoff`, `multiplier`, `max-wait`). If a run fails, wait as
the message says and start again as in item 4.

### Other providers

Besides `gemini-api`, Kaizent has the `openai`, `anthropic` and `ollama` adapters. Each
needs its own key (or, for Ollama, its URL) in `.env`; see `.env.example`:

| Adapter | In `.env` |
|---|---|
| `openai` | `OPENAI_API_KEY` (and `OPENAI_BASE_URL` for another OpenAI-compatible endpoint) |
| `anthropic` | `ANTHROPIC_API_KEY` |
| `ollama` | `OLLAMA_BASE_URL`, for example `http://localhost:11434` |

Then choose the adapter and the model for the step, with a model id from the provider:

```
KAIZENT_LLM_RESOLUTION_ADAPTER=openai
KAIZENT_LLM_RESOLUTION_MODEL=<model id>
```

The application starts even when an adapter has no key. If a step uses it, the startup
log warns about it, and the step's model calls fail with a message that names the
missing variable. Keys are never logged.

Each run records the tokens of its model calls. To record their cost too, give Kaizent the
model's price in US dollars per million tokens, in a git-ignored `config/application.yaml`
in the repository root (model ids with dots need brackets):

```yaml
kaizent:
  llm:
    pricing:
      "[gemini-3.6-flash]": { input-per-million: 0.75, output-per-million: 3.75 }
```

Take the prices from the provider's pricing page; Kaizent ships none, because they
change. Without a price, the cost type is `UNKNOWN`. `GET /api/v1/system` shows the
adapter, model and cost type of each step.

To try the cycle without any model, for example offline, use the scripted `fake` adapter,
which returns a fixed reply:

```
KAIZENT_LLM_RESOLUTION_ADAPTER=fake
```

### Check a model configuration

Before running the pipeline, you can check that an adapter, a model, a key or a CLI login
works: the `llm-check` command makes **one** model call on one ticket of the inbox, prints
the answer, the tokens, the cost and the cost type, and exits. It stores and delivers
nothing. PostgreSQL must be running, as for the application:

```bash
java -jar kaizent-server/target/kaizent-server-0.1.0-SNAPSHOT.jar --spring.profiles.active=llm-check --kaizent.llm-check.ticket=PC-002
```

It uses the configuration of the resolution step (adapter, model, preset). The exit code
is 0 when the call works, and 1 otherwise, with the provider's cause, for example
`HTTP 429 RESOURCE_EXHAUSTED`.

## Run with a CLI tool (local, personal mode)

Instead of an API key, a **single structured call** (the triage, the lesson drafting and
the `llm-check` command) can use a command-line tool that you are already logged in to,
through the `cli` adapter. Today the **Codex CLI** has a preset (`codex`); other tools
follow.

> **Not for the resolution step.** The agentic CLI runtime, where the tool itself resolves
> the ticket with file access, is planned for wave 2 (ADR-0033): the sandbox of Codex is
> documented as restricting writes, not reads (not tested in agent mode), so the read
> restriction that ADR-0032 requires is not demonstrated for an agent. The resolution step needs an API key. The
> skeleton's single resolution call still accepts a CLI preset until spec 0008 replaces it.

> **A local, personal development mode.** Read this before you use it:
>
> - Use your **own** account, on your **own** machine, for your **own** work. Do not use a
>   subscription to serve other people's requests or to run a shared service: for that,
>   use the API adapters with an API key.
> - The tool's login stays in a Docker volume on your machine. Kaizent's application
>   mounts it into the tool's container but never reads, copies or logs it. Inside that
>   container, the image's wrapper keeps the tool's sessions and caches in a temporary
>   folder and moves only the login file between it and the volume (ADR-0031).
> - You build the tool's image yourself from this repository; the project publishes no
>   images or tool binaries.
> - The tool's own read-only sandbox stays on; the container adds a second boundary
>   (ADR-0030).
> - **Check your provider's terms of use.** Whether your plan allows this use is your
>   responsibility.

Each call runs in a **new container** with only what the call needs: no workspace files
for a single call, a read-only root file system, the non-root user 10001, no Linux
capabilities, CPU and memory limits and a timeout (5 minutes by default,
`kaizent.cli.timeout`). Docker is needed: Docker Desktop, or Rancher Desktop with the
`dockerd (moby)` container engine.

1. Build the Codex image (the Codex CLI version is pinned in
   `docker/cli/codex/Dockerfile`):

   ```bash
   docker compose --profile cli build codex-cli
   ```

2. Create the volume for the login, once. Docker Compose does not manage it, so
   `docker compose down -v` (an empty database) keeps your login:

   ```bash
   docker volume create kaizent-cli-codex
   ```

3. Log in once, with your ChatGPT account. The login uses a device code: the command
   prints a URL and a code that you open in your browser. If it says that device code
   login is disabled, enable it in your ChatGPT security settings and try again.

   ```bash
   docker compose --profile cli run --rm codex-cli kaizent-codex login --device-auth
   ```

   The login is kept in the `kaizent-cli-codex` Docker volume. To check it, run
   `docker compose --profile cli run --rm codex-cli kaizent-codex login status`.

4. Select the preset for a single-call step in `.env`. Until the triage step exists, the
   skeleton's resolution call and the `llm-check` command use the `KAIZENT_LLM_RESOLUTION_*`
   settings. A model is optional; without one, Codex uses its default:

   ```
   KAIZENT_LLM_RESOLUTION_ADAPTER=cli
   KAIZENT_LLM_RESOLUTION_PRESET=codex
   KAIZENT_LLM_RESOLUTION_MODEL=
   ```

5. Check it with the `llm-check` command (above), then run the application as usual.
   `GET /api/v1/system` shows the preset and the cost type `SUBSCRIPTION`. Tokens are
   recorded when the tool reports them; there is no price.

To log out and remove the login, run
`docker compose --profile cli run --rm codex-cli kaizent-codex logout`, then
`docker volume rm kaizent-cli-codex`.

Each call runs in its own container, removed when the call ends. If Kaizent logs that it
could not confirm a removal, or that a `docker create` was cut off, remove what is left
with the `docker rm --force <name>` command in the log: it names only that call's
container, and also stops it if it is still running. To find leftovers, list the CLI
containers with `docker ps --all --filter label=br.com.refatorado.kaizent.cli=true`.

To remove all leftovers at once, **stop Kaizent first** (no CLI call in progress), then
prune the stopped or created CLI containers:

```bash
docker container prune --filter label=br.com.refatorado.kaizent.cli=true --filter until=10m
```

A call that is starting shows as `Created` for a moment, so a prune while Kaizent runs
could remove that call's container; the age limit is only an extra safeguard, not a
guarantee. `prune` never touches running containers; use `docker rm --force <name>` for
those. No CLI container holds your login, which stays in the volume.

## Running evaluations

The evaluation runner sends the twelve PetClinic demo tickets through the pipeline with
a real model, checks each reply against its expected outcome in
`examples/petclinic/evals/cases/`, and writes a report. Use it to see whether a change to
the resolution prompt (`kaizent-server/src/main/resources/prompts/`) or to the model made
the replies better or worse
([spec 0003](docs/specs/0003-petclinic-demo-workspace/spec.md)). The workspace's roles,
skills and knowledge are not used by the pipeline yet (specs 0006 to 0008), so changing
them does not change the results today.

1. Set up the application as in [Getting started](#getting-started) (steps 1 to 3) and
   [Run with a real model](#run-with-a-real-model) (the API key). PostgreSQL must be
   running.
2. Stop the application if it is running. Otherwise its worker could take some of the
   evaluation runs, with its own configuration.
3. From the repository root, run:

   ```bash
   java -jar kaizent-server/target/kaizent-server-0.1.0-SNAPSHOT.jar --spring.profiles.active=eval
   ```

   The same command works in PowerShell. The runner starts no web server. It takes the
   cases one after another, waits for each run (at most 10 minutes per case), writes the
   report and exits. Each case makes one model call today, so a full evaluation uses
   twelve calls of your quota. On the Gemini free tier, `gemini-3.6-flash` allowed 5
   requests per minute and 20 per day when this was written (see your limits in
   [Google AI Studio](https://aistudio.google.com/)). Limits are per model. So a full
   evaluation can exceed the per-minute limit, and only one full evaluation fits in a day.
   To stay under a per-minute limit, space the cases out (see "Delay between cases"
   below).
4. Open the report, `target/eval/report-<timestamp>.md`. It shows:
   - the adapter and model of each step;
   - the totals per property (pass, fail, skipped);
   - one row per case, with the run's status and duration;
   - the details: why each check failed or was skipped, and the suggested reply.

Today the runner checks `mentionsAll`, `mentionsNone` and `replyLanguage`. The language
is detected from the reply itself, locally, with [Lingua](https://github.com/pemistahl/lingua).
The other properties are reported as skipped, with the spec that adds them: triage in
spec 0006, citations and confidence in spec 0008. Tokens and cost are not recorded yet.
The results are reported, not enforced, in wave 1.

Good to know:

- **Data:** the demo tickets go to the model provider, as in
  [Run with a real model](#run-with-a-real-model). They are synthetic.
- **No review, no delivery:** evaluation runs never wait for a reviewer and never write
  to `outbox/`. They stay in the database, but the console's run list leaves them out.
- **Failed calls:** rate limits and transient errors are retried first (see
  [Run with a real model](#run-with-a-real-model)). A case whose model call still fails
  fails all its checks, and the report shows the provider's cause, for example
  "Gemini API call failed: HTTP 429 RESOURCE_EXHAUSTED: … (the provider asks to wait
  41s)"; the other cases still run. On the Gemini API, a per-minute limit resets after a
  minute and a daily limit at midnight, Pacific time
  ([rate limits](https://aistudio.google.com/docs/rate-limits)). Then run the evaluation
  again.
- **Delay between cases:** `--kaizent.eval.case-delay=15s` waits 15 seconds after each
  case before starting the next one, whatever the adapter. To stay under a limit of N
  requests per minute, use a little more than 60/N seconds, for example `15s` for 5 per
  minute. The default is no delay.
- **Timeouts:** if a case does not finish within the timeout, the runner stops there and
  reports the remaining cases as `NOT RUN`. Change the timeout with
  `--kaizent.eval.case-timeout=20m`.
- **Fake adapter:** the runner refuses to start when a step uses the `fake` adapter,
  because scripted replies say nothing about a model. Add `--dry-run` only to test the
  runner itself.

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
