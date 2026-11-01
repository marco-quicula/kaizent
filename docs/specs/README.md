# Specs

Kaizent uses **spec-driven development** ([ADR-0015](../adr/0015-spec-driven-development.md)).
Every delivery slice is specified, reviewed and approved before it is implemented.

## Where specs fit

```
vision.md → roadmap.md (waves) → spec per slice → ADR (when a decision arises) → code + tests
```

- The [vision](../vision.md) and the [roadmap](../roadmap.md) say *where we are going*.
- A **spec** says *exactly what one slice delivers and how*.
- [ADRs](../adr/README.md) record decisions that outlive a single spec.
- [docs/architecture.md](../architecture.md) is the shared technical picture that every
  plan builds on.

## Folder layout

```
docs/specs/
├── README.md               # this file: process and index
├── _templates/             # spec.md, plan.md, tasks.md templates
└── NNNN-short-name/
    ├── spec.md             # WHAT and WHY: stories, acceptance criteria, evaluation cases, out of scope
    ├── plan.md             # HOW: modules, ports, data, API, configuration, risks
    └── tasks.md            # ordered, small, verifiable tasks
```

## Lifecycle

| Status | Meaning |
|---|---|
| `Draft` | Being written or discussed |
| `Approved` | The maintainer approved `spec.md` and `plan.md`; implementation may start |
| `In progress` | Implementation has started; tasks are being checked off |
| `Done` | All acceptance criteria are met and merged |

Rules:

1. **No implementation before approval.** An implementation PR references the spec and
   the task IDs it covers (for example, `Implements 0005 T3–T5`).
2. **Acceptance criteria are testable.** Each `AC-n` maps to at least one automated test.
   LLM-dependent behavior is covered by **evaluation cases** (`EV-n`) instead, run on
   demand ([ADR-0020](../adr/0020-testing-and-evaluation-strategy.md)).
3. **Specs change with the code.** If implementation shows that a spec is wrong, update
   the spec in the same PR. A significant change in scope goes back to the maintainer for
   approval.
4. **Decisions become ADRs.** A decision that affects more than one spec, or that is
   expensive to reverse, gets an ADR. The plan links to it.
5. **Small slices.** A spec should be implementable in a handful of PRs. Split it if it
   is not.

## Wave 1 specs

Implement in this order. Each spec lists its dependencies.

| # | Spec | Status |
|---|---|---|
| [0001](0001-project-foundation/spec.md) | Project foundation | In progress |
| [0002](0002-walking-skeleton/spec.md) | Walking skeleton (end-to-end thin slice) | Approved |
| [0003](0003-petclinic-demo-workspace/spec.md) | PetClinic demo workspace and evaluation runner | Approved |
| [0004](0004-llm-access/spec.md) | LLM access: API, CLI and fake adapters | Approved |
| [0005](0005-problem-intake/spec.md) | Problem intake and canonical model | Approved |
| [0006](0006-triage-and-roles/spec.md) | Triage and roles | Approved |
| [0007](0007-skills-and-knowledge/spec.md) | Skills and knowledge | Approved |
| [0008](0008-resolution-and-proposal/spec.md) | Resolution and proposal | Approved |
| [0009](0009-trace-usage-and-budget/spec.md) | Run trace, usage and budget | Approved |
| [0010](0010-review-console/spec.md) | Review console | Approved |
| [0011](0011-lessons/spec.md) | Lessons | Approved |
| [0012](0012-subject-metrics-and-autonomy/spec.md) | Subject metrics and autonomy levels | Approved |

Specs 0003 and 0004 can be worked on in parallel after 0002.
