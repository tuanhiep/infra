# Autopsy of Distributed Systems

Executable case studies in distributed systems, backend architecture, and principal engineering trade-offs.

This repository is the code artifact layer for two blog tracks:

- `system-design/`: runnable Spring Boot case studies for distributed systems mechanics.
- `principal-engineering/`: runnable architecture cases focused on judgment, trade-offs, and operating constraints.

## Stack

- Java 21
- Spring Boot 4.0.6
- Maven multi-module project

## Structure

```text
autopsy-of-distributed-systems/
  system-design/
    rate-limiter/
  principal-engineering/
    graceful-degradation/
```

Each module starts small on purpose: code first, then README, ADRs, tests, benchmarks, and blog links as the series grows.

