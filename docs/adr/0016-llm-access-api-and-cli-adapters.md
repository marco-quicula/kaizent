# ADR-0016: LLM access through two ports, with API and CLI adapters

- **Status:** Accepted
- **Date:** 2026-11-01
- **Supersedes:** [ADR-0007](0007-llm-gateway-provider-neutral.md)

## Context

ADR-0007 put all model access behind a single `LlmGateway` port with Spring AI as the
adapter. Two new facts change that:

1. The maintainer has no paid API access yet, but does have subscriptions to agentic
   command-line tools (Codex CLI, Claude Code, Google Antigravity `agy`, Gemini CLI).
   These tools have **non-interactive (headless) modes** that scripts can call (for
   example, `codex exec --json`, `claude -p --output-format json`,
   `agy -p --output-format json`).
2. Those tools are **agents, not models**: they run their own tool loop (reading files,
   running commands). Driving them as a plain "chat completion" would waste their
   abilities and would not fit a single port.

The Gemini API also offers a free tier (with an AI Studio key). Free-tier data may be
used by Google to improve its products, which is acceptable for our synthetic demo data.

## Decision

- Model access uses **two ports**:

| Port | Purpose | Used for |
|---|---|---|
| `LlmGateway` | One structured model call: prompt in, typed JSON out, plus usage | Triage, lesson drafting |
| `AgentRuntime` | One agentic task: role, problem, skills catalog, knowledge access, output schema and budget in; result, trace and usage out | Resolution |

- Each port has **API** and **CLI** adapter families:

| Adapter | `LlmGateway` | `AgentRuntime` |
|---|---|---|
| **API (Spring AI):** Gemini, OpenAI, Anthropic, Ollama | Spring AI ChatClient with structured output | **Our own loop**: Spring AI tool calling with our tools (`search_knowledge`, `read_document`, `read_skill`) |
| **CLI:** Codex, Claude Code, Antigravity, Gemini CLI, generic | Headless call with the tool's own tools disabled; JSON output | **The tool's loop**: read-only access to the workspace's knowledge and skills folders; must answer with our JSON schema, citing sources by relative path |
| **Fake** (tests) | Scripted responses | Scripted results |

- The CLI adapters run commands through a **`CliExecutor`** seam:
  - `LocalProcessExecutor` (wave 1) runs the tool as a local process;
  - `RemoteRunnerExecutor` (wave 5) calls an "agent runner" service, for example a
    Kubernetes pod with the tools installed.
- The CLI adapters are **restricted by default**: a read-only sandbox, no shell or write
  tools where the tool supports it, a working directory limited to the workspace, and a
  **mandatory timeout** (one known Antigravity issue reports headless runs that hang
  when started as a subprocess).
- CLI tools are configured through **presets** (Codex first, validated in wave 1) plus a
  **generic command template** (command, arguments, how the prompt is passed, how output
  is parsed). Exact flags are verified against the installed version during
  implementation.
- Which adapter and model serve each pipeline step is set by **configuration profiles**
  (for example, `triage → gemini-api:<model>`, `resolution → cli:codex`).
- **Usage and cost:** every call records tokens when the adapter reports them and a
  **cost type**: `API` (priced from a configurable pricing table), `SUBSCRIPTION` (no
  per-call price) or `UNKNOWN`. Quotas are never hard-coded. Rate-limit errors (HTTP 429)
  are retried with backoff.
- **Budgets** per run: maximum tokens and cost for API adapters; timeout and, where the
  tool supports it, maximum turns for CLI adapters.
- **Credentials:** API keys come from environment variables or a git-ignored `.env`
  file. CLI tools use their own login on the machine that runs them. Kaizent never stores
  or copies those credentials.
- **Terms of use:** the CLI adapters are documented as a **local, personal development
  mode**. Each user is responsible for complying with their provider's terms, which may
  not allow using a subscription to serve other people's requests.
- The wave 1 MVP is validated with **Codex CLI** (CLI path), the **Gemini API free tier**
  (API path) and the **fake** adapters (tests).

## Alternatives considered

- **API only (ADR-0007).** Blocks development without paid API access.
- **One port, with the CLI as a "chat model".** Loses the CLI tools' agent abilities, and
  their own loops do not fit a single completion call.
- **Expose Kaizent's tools to the CLI tools through an MCP server in wave 1.** This would
  give uniform tools and a complete trace across adapters, but it adds a component to the
  MVP. It is planned for wave 2, before the vector store (wave 3) makes it necessary.

## Consequences

- The core depends on `LlmGateway` and `AgentRuntime`, never on Spring AI or on a CLI.
- The trace detail differs by adapter: complete for our own loop, best effort (parsed
  from the tool's event stream) for CLI tools.
- CLI calls are slower (process start-up) and limited by subscription quotas. That is
  acceptable for one user processing one ticket at a time.
- A Kubernetes "agent runner" and a how-to for it are added to wave 5.
