# Infra

Executable case studies in distributed systems, backend architecture, and principal engineering trade-offs.

This repository is the code artifact layer for three tracks:

- `brick/`: reusable code bricks for system design case studies.
- `system-design/`: runnable Spring Boot case studies for distributed systems mechanics.
- `principal-engineering/`: runnable architecture cases focused on judgment, trade-offs, and operating constraints.

## Stack

- Java 21
- Spring Boot 4.0.6
- Maven Wrapper 3.9.9
- Maven multi-module project with dependency and plugin governance in the root POM

## Structure

```text
infra/
  brick/
    circuit-breaker/
    rate-limiter/
  system-design/
    README.md
  principal-engineering/
    README.md
```

## Build

```bash
./mvnw clean test
```

The root build enforces the Java/Maven baseline and dependency convergence. Each public module keeps an English `README.md`; local Vietnamese study notes use `<module>.md` and are ignored by Git.

## Validation

Run the relevant module tests before treating a pattern as reusable. Each pattern should include the failure mode it handles and the behavior it guarantees.

## Engineering Notes

This repo should stay focused on executable architecture patterns: clear constraints, explicit trade-offs, and small services that demonstrate one concept well.
