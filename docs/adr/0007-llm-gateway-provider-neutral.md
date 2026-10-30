# ADR-0007: Provider-neutral LLM access with cost tracking

- **Status:** Accepted
- **Date:** 2026-10-30

## Context

The maintainer uses OpenAI, Anthropic and Gemini. Contributors will have different
providers available. Some deployments need local models for privacy reasons. The
maintainer's development machine (16 GB of RAM) cannot run capable local LLMs, so
development relies on paid cloud APIs, where cost matters. A consumer chat subscription
does not include API access.

## Decision

- All model access goes through an **`LlmGateway` port**. The default adapter uses
  Spring AI.
- The provider and the model are **chosen by configuration**: Anthropic, OpenAI, Gemini
  and Ollama (local) are supported through Spring AI. The MVP is validated with at least
  one cloud provider.
- Models can be configured **per pipeline step** (for example, a small, cheap model for
  triage and a stronger one for resolution).
- Every run records **input and output tokens and the estimated cost**. A configurable
  **budget limit per run** stops runaway agent loops.
- API keys come only from environment variables or a local `.env` file that is never
  committed. `.env.example` documents the variables without values.

## Alternatives considered

- **A single hard-wired provider.** Simpler, but it creates lock-in and excludes
  contributors who use other providers.
- **An external LLM proxy (for example, LiteLLM).** Adds an extra service to run and a
  second language. Not needed while Spring AI covers the providers.

## Consequences

- Provider-specific features are accessed through the port's capabilities, or through a
  native-SDK adapter when Spring AI does not expose them.
- Local models (Ollama) work as a pluggable option. On small machines they are suitable
  mostly for simple steps such as classification, and for embeddings in wave 3.
- Pricing tables per model must be configurable, because providers change prices.
