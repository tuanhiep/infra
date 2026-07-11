# System Design

Runnable modules for distributed-systems mechanics, explicit consistency boundaries, and
failure-oriented architecture trade-offs.

## Completed Case Study

### [Idempotent Payment Ledger](idempotent-payment-ledger/)

A retry-safe payment and double-entry ledger module with PostgreSQL-authoritative state,
optional owner-scoped Redis reservations, concurrent-writer recovery, overdraft protection,
post-commit cache recovery, and migration upgrade safety.

Its 32-test suite runs against PostgreSQL 16 and Redis 7 through Testcontainers. The public
review surface includes design and failure-mode documents, two ADRs, an operations runbook,
and an evidence gate. Deferred capabilities are stated explicitly in the module rather than
presented as implemented behavior.
