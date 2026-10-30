# ADR-0013: Spring PetClinic as the demo domain

- **Status:** Accepted
- **Date:** 2026-10-30

## Context

The first real use case is help desk tickets for a business system, but there is no
real system or ticket history available, and a public repository cannot contain real
customer data. The MVP still needs realistic problems to solve, and anyone who clones
the repository must be able to reproduce the cycle.

## Decision

- The [Spring PetClinic](https://github.com/spring-projects/spring-petclinic) sample
  application is the **"product being supported"** in the demo.
- The repository ships **synthetic demo data** written for PetClinic:
  - tickets (bug reports with stack traces, how-to questions);
  - a knowledge base (docs, FAQ, known issues);
  - skills;
  - role definitions.
- The demo data is clearly separated from the application code and is used for manual
  testing, automated tests and documentation.

## Alternatives considered

- **A fictional product with no code.** Less realistic for stack traces and bug
  diagnosis.
- **Real tickets from a company.** Privacy and licensing problems in a public repository.

## Consequences

- Demo tickets can reference real PetClinic code and errors, which makes diagnoses
  credible.
- Real deployments replace the demo data with their own configuration repository.
