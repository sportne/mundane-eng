# Task TC-0704: Measure Operational Scale

```json
{
  "format": "mundane-work-source-0.1",
  "status": "Complete",
  "dependencies": [
    "TC-0702",
    "TC-0703"
  ],
  "relations": [],
  "planning": {
    "stage": "7",
    "type": "Operational verification",
    "condition": "TC-0702 and TC-0703",
    "unlocks": "TC-0901 and TC-1001",
    "statusNote": ""
  }
}
```

## Question

Do native startup, scan behavior, memory use, diagnostic volume, and trace output remain practical at the selected corpus scale?

## Outcome

Reproducible measurements identify whether the disposable in-memory architecture remains adequate and where usability, rather than raw performance, becomes limiting.

## Work

- Measure clean and repeated validator, formatter-check, and representative trace invocations.
- Record corpus size, file count, relationship count, platform, GraalVM version, and command.
- Exercise large diagnostic sets and deep or branching traces.
- Measure both file layouts where their I/O behavior differs.
- Compare against human workflow tolerances rather than inventing product-scale targets.

## Acceptance evidence

- Commands and input baseline reproduce every reported measurement.
- No cache or database is added before the evidence is reviewed.
- Output navigability is assessed alongside runtime and memory.
- Any threshold used for a decision is explained by a workflow.
- Measured bottlenecks are assigned to a bounded follow-up rather than generalized architecture.

## Out of scope

- Performance marketing claims.
- Distributed processing.
- Persistent indexing without measured need.

## Completion decision

Retain the simple in-memory model unless measurements show a concrete workflow cannot be served. If derived caching is justified, require it to remain optional and disposable.

Completed on 2026-08-29. [Experiment 0014](../../experiments/0014-operational-scale/README.md)
measured independent native processes over semantically equivalent six- and
60-file layouts. Ordinary repeated means occupied a 3.4–4.2 ms envelope on
the recorded host with no supported layout distinction. The 1,200-error case
remained computationally small and produced a 157 KB terminal stream whose
human navigability remains a TC-0707 hypothesis.

Retain the disposable in-memory model and add no cache, database, or path
semantics. TC-0707 owns the bounded diagnostic-presentation question.

## References

- [TC-0702](task-0702-run-the-independent-interpretation-proxy-trial.md)
- [TC-0703](task-0703-run-the-multi-author-and-layout-trial.md)
