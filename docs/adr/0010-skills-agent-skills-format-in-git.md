# ADR-0010: Skills in the Agent Skills format, versioned in git

- **Status:** Accepted
- **Date:** 2026-10-30

## Context

Skills are versioned, reusable procedures that tell the agent how to handle a class of
problems. They must be easy for humans to read and review, versioned, and eventually
improved automatically, with human approval.

## Decision

- Skills use the open **[Agent Skills](https://agentskills.io) format**: one folder per
  skill containing a `SKILL.md` (YAML front matter with `name` and `description`, then
  Markdown instructions) and optional resource files.
- Skills are read from a **git repository** through the `SkillRepository` port. By
  default this is a folder inside the deployment's configuration repository, and it can
  be pointed at any git repository and path at setup time.
- **Git is the version history.** A skill's version is the commit that last changed it,
  and every run records the skill versions it used.
- Roles declare which skills they may use. The agent loads a skill's full content only
  when the skill is relevant (progressive disclosure).
- From wave 2, automatic skill improvements are proposed as **branches or pull requests**
  on the skills repository, so humans review them like code.

## Alternatives considered

- **A custom skill format.** No ecosystem, and more documentation for us to write.
- **Skills stored in the database.** Loses diff, review and history for free.

## Consequences

- Skills written for Kaizent are portable to other tools that support Agent Skills, and
  the other way round.
- The skills repository becomes a first-class, reviewable asset of every deployment.
