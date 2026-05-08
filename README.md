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
  system-design/
    rate-limiter/
  principal-engineering/
    graceful-degradation/
```

## Build

```bash
./mvnw clean test
```

The root build enforces the Java/Maven baseline and dependency convergence. Each public module keeps an English `README.md`; local Vietnamese study notes use `<module>.md` and are ignored by Git.
