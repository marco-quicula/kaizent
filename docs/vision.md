# Vision

## The problem

Support and engineering teams answer the same kinds of questions and fix the same kinds
of problems again and again. The knowledge needed to solve them is scattered across
past tickets, documentation, code and people's heads. Generic AI assistants can help in
the moment, but they do not remember what worked, do not get better at a specific
organization's problems, and cannot show whether they deserve to be trusted with more
autonomy.

## What Kaizent is

Kaizent is an agentic problem-solving tool. It **receives a problem, assembles the
right expertise for it, proposes a resolution and learns from the outcome**. Every
resolution is a PDCA cycle:

| PDCA | In Kaizent |
|---|---|
| **Plan** | Ingest the problem, identify its subject, select a role and decide which skills, tools and knowledge to use. |
| **Do** | The agent works the problem and produces a resolution with a confidence level and cited sources. |
| **Check** | The resolution is verified: by a human at first, later also by outcome signals such as a ticket that is not reopened, or a rating. |
| **Act** | The outcome feeds back: a lesson for the knowledge base, an improvement to an existing skill, a proposal for a new skill, and a curated example for future evaluation and fine-tuning. |

## Long-term capabilities

These capabilities are the destination. They are not the MVP, and they arrive in
[waves](roadmap.md).

- **Problem ingestion** from any channel (help desk tools such as Movidesk, Slack, chat,
  email, alerts) through specialized connectors that produce one canonical problem model.
- **A resolution agent** that takes on a configurable **role** (persona, allowed skills,
  tools and knowledge sources) based on the problem's subject.
- **Versioned skills** stored in git, in the open
  [Agent Skills](https://agentskills.io) format (`SKILL.md`).
- **Knowledge retrieval**: first Markdown in git, then RAG over a vector store, plus
  specialized MCP servers per role.
- **Memory of lessons learned**: corrections and successful resolutions become reusable
  knowledge.
- **Automatic skill improvement**: the system proposes changes to existing skills and
  suggests new ones, and a human approves them like code review.
- **Graduated autonomy** per subject (Junior → Mid-level → Senior → Specialist), earned
  from measured success rates.
- **Multi-domain**: support tickets and bugs first, then business and analytical
  questions, then operational incidents.
- **Fine-tuning**, eventually, on the dataset of human-verified resolutions that the
  system collects from day one.

## Principles

1. **Every wave works end to end.** Each wave delivers a complete, usable cycle, even if
   it is small. Nothing is built "for later" without being exercised now.
2. **Human in the loop until trust is measured.** Autonomy is earned per subject from
   data, never assumed. Humans can always override.
3. **Measure from day one, act on it later.** Every run records input, context,
   proposal, final answer, verdict, tokens and cost. Metrics exist before the decisions
   that depend on them.
4. **Ports and adapters everywhere.** Sources, sinks, skills, knowledge, tools and LLM
   providers sit behind ports. A new channel or technology is a new adapter, not a
   rewrite.
5. **Start with plain files; earn complexity.** Markdown in git comes before vector
   databases, and built-in tools come before MCP. Each new piece of technology has to
   justify itself with a concrete need.
6. **Provider-neutral.** No lock-in to a single LLM vendor. Cloud APIs and local models
   are interchangeable through configuration.
7. **Transparent reasoning.** A reviewer can always see why the agent answered the way
   it did: the role, the skills, the sources, the tool calls and the cost.
8. **Cost-aware.** Token usage and cost are visible for every run, and budgets are
   enforceable.
9. **Open by default.** The project is public and Apache-2.0 licensed, with all
   documentation in English. No secrets live in the repository, and anyone can clone it
   and run it with demo data.
10. **Privacy-conscious.** Tickets may contain personal data. The design must allow
    redaction before data leaves the system and must support local models for sensitive
    deployments.

## Core concepts

| Concept | Meaning |
|---|---|
| **Problem** | The canonical representation of an incoming request, independent of the channel it came from. |
| **Subject** | The classification of a problem (for example, "appointments", "login errors"). Metrics and autonomy are tracked per subject. |
| **Role** | A configured persona the agent assumes for a subject, with its allowed skills, knowledge sources and tools. |
| **Skill** | A versioned, reusable procedure (`SKILL.md` plus optional resources) that tells the agent how to handle a class of problems. |
| **Knowledge source** | Where the agent looks things up: a Markdown repository, a vector store, an MCP server. |
| **Run** | One execution of the resolution pipeline for one problem, with a full trace. |
| **Proposal** | The agent's output: diagnosis, suggested reply, confidence and cited sources. |
| **Verdict** | The human review outcome: approved as is, approved with edits, or rejected (with a comment). |
| **Lesson** | Knowledge derived from a verdict, proposed by the agent and approved by a human. |
| **Autonomy level** | Junior, Mid-level, Senior or Specialist for a subject, derived from verdict history. It determines how much human review is required. |

## Non-goals (for now)

- A replacement for help desk tools. Kaizent plugs into them.
- A general-purpose agent framework. Kaizent is an application built on existing
  frameworks.
- Training foundation models. Fine-tuning existing models is the most we plan to do.
