# ADR-0027: Workspace layout and file ownership

- **Status:** Accepted
- **Date:** 2026-11-01

## Context

A deployment of Kaizent is configured by its roles, skills, knowledge, inbox and outbox.
These are files (ADR-0010, ADR-0011, ADR-0014), normally kept in git. We need a standard
layout, and a rule for who commits changes to those files.

## Decision

- A **workspace** is a folder with this layout:

```
<workspace>/
├── kaizent-workspace.yaml   # name, description, subjects, default role
├── roles/<role-id>.md       # role definitions (front matter + persona)
├── skills/<skill-name>/SKILL.md
├── knowledge/**/*.md        # knowledge base; approved lessons go to knowledge/lessons/
├── inbox/*.md               # incoming tickets (ADR-0028)
├── outbox/*.md              # approved replies
└── evals/cases/*.yaml       # evaluation cases (ADR-0020)
```

- The workspace path is configured with `kaizent.workspace.path`. The demo workspace is
  `examples/petclinic/` in this repository. Real deployments point to their own
  repository.
- **Kaizent reads and writes plain files and never runs git commands** in wave 1. The
  people who own the workspace commit the changes, for example new replies in `outbox/`
  or approved lessons.
- When the workspace is a git working copy, Kaizent **reads** the current commit (with
  JGit, read-only) to record the version of each skill and knowledge file used in a run.
  Otherwise it records the file's content hash.
- Kaizent never modifies or deletes files in `inbox/`. Processed tickets are tracked in
  the database.

## Alternatives considered

- **Kaizent commits by itself.** It would need git credentials and authorship decisions.
  That is deferred to wave 2, together with skill improvement proposals as branches or
  pull requests.
- **Moving processed tickets to another folder.** Kaizent would then mutate input files,
  which is surprising in a git-managed folder. The database already tracks processing.

## Consequences

- The demo works on a plain clone of this repository with no git configuration.
- Version references are commit hashes when available and content hashes otherwise.
