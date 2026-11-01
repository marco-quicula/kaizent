# ADR-0023: Run execution model

- **Status:** Accepted
- **Date:** 2026-11-01

## Context

Each ticket goes through several steps, some of them slow (LLM calls, CLI processes),
with a human pause for review. Runs must survive restarts, be visible in the console
while they progress, and never be processed twice.

## Decision

- **One user, one instance** in wave 1. Runs are processed **one at a time** by a single
  worker.
- The queue is the `run` table in PostgreSQL. The worker claims the oldest runnable run
  with `SELECT ... FOR UPDATE SKIP LOCKED`, which stays safe if more instances are added
  later.
- A run follows this **state machine**:

```
RECEIVED → TRIAGING → RESOLVING → AWAITING_REVIEW ─┬→ APPROVED → DELIVERED
                                                   └→ REJECTED
any processing state ─→ FAILED (reason: ERROR | BUDGET_EXCEEDED | TIMEOUT)
FAILED ─(manual retry)→ RECEIVED
```

- Every state change and every step event (LLM call, tool call, error) is written as a
  **trace event** in the same transaction as the state change.
- Cross-module reactions (for example, a recorded verdict triggering lesson drafting)
  use **Spring Modulith application events** with the JDBC **event publication
  registry**, so events are not lost on restart.
- Persistence uses **Spring Data JDBC** (aggregates without the JPA session model) and
  **JSONB** columns for structured payloads such as proposals and trace details.
- The console receives live updates through **Server-Sent Events**.

## Alternatives considered

- **A message broker (RabbitMQ, Kafka).** An extra service for a single-user MVP.
- **In-memory queues.** Runs would be lost on restart.
- **JPA/Hibernate.** Workable, but heavier than needed for a few aggregates with JSON
  payloads.

## Consequences

- Throughput is one ticket at a time, which is enough for the MVP. Parallelism is a
  configuration change later.
- The trace is durable and complete, which is what the console, the metrics and the
  evaluation need.
