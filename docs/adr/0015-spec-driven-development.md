# ADR-0015: Spec-driven development

- **Status:** Accepted
- **Date:** 2026-11-01

## Context

Kaizent is built mostly with AI assistance across many sessions, and every session starts
without memory. Without an approved description of what to build, an assistant fills the
gaps with its own assumptions. The vision, the roadmap, the ADRs and `CLAUDE.md` already
cover principles and global rules. What is missing is a precise, approved description of
each piece of work before any code is written.

## Decision

- Development follows **spec-driven development (SDD)**, with a lightweight convention of
  our own, inspired by GitHub Spec Kit:

  ```
  vision.md → roadmap.md (waves) → spec per slice → ADR (when a decision arises) → code + tests
  ```

- Each delivery slice has a folder `docs/specs/NNNN-short-name/` with three files:
  - `spec.md`: **what and why**: user stories, acceptance criteria, evaluation cases,
    out of scope.
  - `plan.md`: **how**: modules, ports, data, API, configuration, risks.
  - `tasks.md`: small, ordered, verifiable tasks.
- Templates live in `docs/specs/_templates/`. The process is described in
  `docs/specs/README.md`.
- **The maintainer approves a spec (`spec.md` and `plan.md`) before implementation
  starts.** A spec's status moves `Draft → Approved → In progress → Done`.
- Acceptance criteria are written so they can become tests. For behavior that depends on
  an LLM, the criteria are expressed as **evaluation cases** (input → expected
  properties), because LLM output is not deterministic (ADR-0020).
- When implementation shows that a spec is wrong, the spec is updated **in the same pull
  request** as the code. Specs never describe behavior the code does not have.

## Alternatives considered

- **GitHub Spec Kit as a tool.** It generates many files and its own folder structure. We
  adopt the ideas without the tooling, and can revisit later. A repeated workflow can
  become a Claude Code skill in this repository.
- **Issues only, without specs.** Issues are good for tracking but poor as a versioned,
  reviewable source of truth for the *what*.
- **No specs (code first).** Too risky with AI-assisted development across sessions.

## Consequences

- Every implementation PR references the spec (and task IDs) it implements.
- Specs are the source of truth for *what*. GitHub issues, if adopted, track *execution*
  and link back to specs.
- There is some up-front writing cost, accepted in exchange for predictable sessions and
  an onboarding path for contributors.
