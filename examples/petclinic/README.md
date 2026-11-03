# PetClinic demo workspace

A Kaizent workspace (ADR-0027) for a support team that helps the users of
[Spring PetClinic](https://github.com/spring-projects/spring-petclinic). Kaizent reads it by
default (`kaizent.workspace.path=examples/petclinic`).

> **All data in this workspace is synthetic.** The tickets, the requesters' names and the
> conversations were written for this demo. They do not come from any real help desk, and
> they contain no real customer or personal data. Keep it that way when you add content.

## Layout

| Path | Content |
|---|---|
| `kaizent-workspace.yaml` | The workspace name, the default role and the four subjects |
| `roles/` | `support-agent` (owners, pets, visits) and `technical-support-engineer` (technical) |
| `skills/` | Four skills in the Agent Skills format (`SKILL.md`) |
| `knowledge/` | The knowledge base: product guides, FAQ, known issues and setup. It describes PetClinic at the commit named in `knowledge/product/overview.md`, and it is the source of truth for the demo |
| `knowledge/lessons/` | Lessons approved by reviewers (spec 0011) |
| `inbox/` | Twelve demo tickets, `PC-001` to `PC-012` |
| `outbox/` | Approved replies, written by Kaizent |
| `evals/cases/` | One evaluation case per ticket (ADR-0020) |

## Language

Everything is in English, except the body of `PC-003`, which is in Brazilian Portuguese to
check that replies follow the language of the ticket (ADR-0029). Its evaluation case
(`evals/cases/PC-003.yaml`) therefore expects Portuguese phrases.
