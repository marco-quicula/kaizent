# ADR-0033: Agentic CLI runtime deferred to wave 2

- **Status:** Accepted
- **Date:** 2026-11-14
- **Supersedes:** the part of [ADR-0016](0016-llm-access-api-and-cli-adapters.md) that puts
  the agentic CLI runtime in wave 1 (and its validation with Codex as the resolution
  runtime). The layers and the gate of [ADR-0032](0032-defense-against-prompt-injection.md)
  are unchanged.

## Context

ADR-0016 planned two ways to run the resolution step in wave 1: Kaizent's own loop through
an API adapter (`AgentRuntime`, native), and the loop of a CLI tool (Codex first) with
read-only access to the role's knowledge and skills. ADR-0032 made the second one depend
on a gate: layer 1 (least capability) must be in place and validated with the real tool
before the agentic CLI runtime is released.

The discovery for the single-call preset (spec 0004 T7) and the tool's documented read-only
sandbox indicate that the sandbox restricts **writes, not reads**: an agent could read other
files in the container, including the tool's login (not tested in agent mode). A mounted
read-only workspace limits writes and the paths we grant, but it is not shown to stop reads
of the rest of the container, and the login has to be in the container for the tool to
work (ADR-0031). Layer 1 of ADR-0032 requires this restriction to be validated with the
real tool, and that validation, with whatever design it needs, is deferred with the runtime.
That layer is therefore not demonstrated, and cannot be assumed, for Codex as an agent, and a
documented warning does not lift the gate.

An external review (2026-11) reached the same conclusion from the specs: the validation done
for single calls (no tools, no workspace) says nothing about the isolation of an agent.

## Decision

- **In wave 1, the resolution step uses only the native runtime**: Kaizent's own loop
  through an API adapter, with Kaizent's three read tools over the role's scope. The
  wave 1 delivers the `AgentRuntime` port, the `TraceSink` and the fake `AgentRuntime`
  (scripted, for tests), so that the seam exists and is tested.
- **The CLI agent adapter (`CliAgentRuntime`) moves to wave 2**, with the other agentic CLI
  work: the `agent` call of the presets and the event mapping into `AGENT_EVENT`s.
- The **candidate design for wave 2** is Kaizent as an MCP server (open decision 2): the CLI
  agent gets Kaizent's tools (`search_knowledge`, `read_document`, `read_skill`), enforced
  by the server over the role's scope, and no shell and no file tools of its own. MCP alone
  does **not** guarantee the read restriction: the tool may keep other capabilities, reach
  its own credentials or open network channels. So the design stays subject to the gate of
  ADR-0032 layer 1, with a negative validation on the real binary (it must fail to read the
  login and files outside the grant), a restrictive tool configuration, a scope enforced
  by the server and a network control that fits the design.
- **Single structured calls through CLI tools stay in wave 1** (triage, lesson and the
  `llm-check` command): they run with the tool's own tools disabled and no workspace path
  (spec 0004 AC-7), so layer 1 holds by construction. A subscription login keeps working for
  them, in the local, personal mode of ADR-0016.
- The application refuses to start with `kaizent.agent.resolution.runtime: cli` in wave 1,
  with a message that points to this ADR.

## Alternatives considered

- **Release the CLI runtime with a warning** ("unsafe for untrusted input"). Rejected by
  ADR-0032: every ticket is untrusted input, and a subscription login in reach of an agent
  is exactly the leak the layers exist to prevent.
- **Block reads in the container** (a custom seccomp or LSM profile, a wrapper that hides the
  login from the agent). Fragile and specific to each tool's layout, and the tool itself
  needs the login. It may come back as part of the wave 2 gate, not as a wave 1 task.
- **Drop CLI agents altogether.** ADR-0016 kept them for good reasons (subscriptions, the
  tool's own agent quality). Deferring them keeps the option.

## Consequences

- **The MVP needs an API key for the resolution step** (the Gemini free tier, OpenAI or
  Anthropic). The Codex subscription no longer resolves tickets. A CLI login still serves
  single calls: triage, lesson and `llm-check`.
- Specs 0004 (AC-8, T8, the evaluation cases), 0008 (AC-4, AC-7, AC-11 and the evaluation
  cases that used Codex) and 0009 (AC-4) are cut accordingly. The roadmap, the README, the
  architecture overview and `AGENTS.md` follow.
- The policy of ADR-0032 layer 3 (route suspicious tickets to the API loop) has nothing to
  route away from in wave 1, because the only runtime is the API one. It stays as the
  contract of the wave 2 CLI runtime (spec 0008 AC-11).
- Wave 2 gets the CLI runtime, the MCP server (open decision 2) and the layer 1 validation as
  one piece of work.
- **Before the agent-mode CLI runtime is released in wave 2, a budget per turn or per
  internal call is a requirement** (spec 0009 AC-7). The provider calls happen inside the
  tool, so the guarantee of spec 0009 for the native runtime (a check before each provider
  call) does not carry over. The mechanism is decided in wave 2. Possible forms: a turn
  limit of the tool itself, when it has one; the container timeout; or calls mediated by
  Kaizent (through MCP), where Kaizent sees each call. In wave 1, single CLI calls are
  bounded by one whole call and the container timeout.
