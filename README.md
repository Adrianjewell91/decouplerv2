# Data Decoupler

A framework for migrating away from a monolith one transaction at a time, without downtime. Each transaction type is decoupled independently — there is no global cut-over, no big-bang migration, and no service interruption.

<img width="2031" height="853" alt="Screenshot 2026-05-07 at 1 17 46 PM" src="https://github.com/user-attachments/assets/35c36985-61d5-4c65-9803-35e5099a934e" />



---

## The Abstraction

### Why

A monolithic SQL database couples every consumer to a single schema. Changing a table's shape, moving its data to a purpose-built store, or scaling a specific access pattern independently is blocked by every other query that touches the same tables. The decoupler breaks that coupling one query at a time: once a transaction is fully decoupled, the data behind it is free to live anywhere.

### Core Abstractions

| Abstraction | Role |
|---|---|
| `Proxy` | Intercepts every `Transaction` and routes it according to its current decoupling phase |
| `Transaction` | A single unit of work — carries a stable routing `id`, a logic payload, and runtime parameters |
| `Monolith` | The legacy system being migrated away from — executes transactions directly |
| `Decomposition` | The replacement layer — executes transactions against `Fragment`s via an `Aggregator` |
| `Aggregator` | Knows how to satisfy one transaction type using a `Partition` of `Fragment`s |
| `Fragment` | Wraps a single data store (e.g. one table) and executes `Command`s against it |
| `Partition` | A named set of `Fragment`s and the `Command` types they support, produced by the `Monolith` |
| `Command` | A single operation against a `Fragment` — read, insert, update, or delete |

### Decoupling Lifecycle

Each transaction type moves through three phases independently:

```
MONOLITH  →  SHADOW  →  DECOUPLED
```

- **MONOLITH** — the `Proxy` routes the transaction exclusively to the monolith.
- **SHADOW** — the `Proxy` routes to both. The monolith result is returned to the caller; the decomposition result is compared silently. Mismatches are recorded in the inspection log.
- **DECOUPLED** — the `Proxy` routes exclusively to the decomposition layer. The monolith is no longer called.

Promotion between phases is operator-triggered, per transaction type, at any time.

### LLM-Driven Decomposition

Rather than manually rewriting each query, the system uses a language model to generate a `DecompositionPlan` — a directed graph of steps over `Fragment`s that together produce the same result as the monolith query. Each step specifies a fragment, an operation type, optional filter expressions, and an optional parent step whose results it depends on. The `Aggregator` executes this plan at runtime, traversing the graph and assembling the final result.

---

## Example Implementation

The `java2` directory contains a reference implementation backed by JDBC and a local LLM:

- **`SqlMonolith`** — executes arbitrary SQL against a JDBC `DataSource`
- **`SqlTableFragment`** — wraps a single database table; introspects its schema at init time
- **`SqlTransaction`** — carries a SQL string and positional parameters as the transaction logic
- **`SqlReadCommand` / `SqlInsertCommand` / `SqlUpdateCommand` / `SqlDeleteCommand`** — the four `Command` types; read returns rows, writes return an affected-row count
- **`PlanBasedAggregator`** — executes a `DecompositionPlan` via `PlanInterpreter`; the plan is generated once by the LLM and cached
- **`OllamaLlmClient`** — generates decomposition plans by calling a locally-running Ollama instance
- **`DefaultProxy`** — implements the three-phase lifecycle with an in-memory inspection log
- Integration tests use an H2 in-process database and cover both a read decoupling and a write decoupling end-to-end

---

## Assumptions

**Results are opaque.** The framework never inspects the content of a `Result`. Comparison in SHADOW mode is delegated entirely to the implementation — the `Proxy` only records whether results matched, not how they were compared.

**A `Partition` is a sealed snapshot.** Once created at init time, a `Partition` cannot be replaced or mutated. Both `fragments()` and `commands()` return unmodifiable lists. The only permitted structural change is extension: `withFragment(f)` returns a *new* `Partition` that includes the additional fragment while leaving the original untouched. This is how a purpose-built microservice fragment is composed alongside the replicated monolith fragments when building an `Aggregator`. The framework does not handle schema drift in a live `Partition`.

**`Monolith` and `Decomposition` are interchangeable.** The `Proxy` holds references to both behind interfaces. A different database engine, a REST-backed decomposition, or an in-memory stub are drop-in replacements — no framework code changes.

**Routing keys are operator-assigned.** A `Transaction` is identified by a stable `id()` set by the caller, not derived from query content. The operator decides which `id` maps to which decoupling phase.

**Decoupling is per transaction type, not per request.** Promoting a transaction advances every future request with that `id`. There is no request-level or percentage-based routing.

**The framework is a routing layer, not a replication platform.** Reads decouple cleanly with a point-in-time snapshot — a slightly stale fragment is still enough to validate the decomposition plan. Writes are harder: every write to the monolith after init immediately diverges from the fragment. The framework does not solve continuous replication; that belongs to existing tools (CDC, logical replication, event queues). The correct seam is `withFragment(f)` — a fragment backed by an externally-replicated store composes in alongside snapshot fragments without any framework changes. The lifecycle (MONOLITH → SHADOW → DECOUPLED) is how you validate and promote once replication lag is acceptable, not how you replicate.
