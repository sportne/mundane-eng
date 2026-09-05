# Task TC-0707: Test Bounded Diagnostic Presentation

```json
{
  "format": "mundane-work-source-0.1",
  "status": "Complete",
  "dependencies": [
    "TC-0704"
  ],
  "relations": [],
  "planning": {
    "stage": "7",
    "type": "Tool-interface experiment",
    "condition": "TC-0704",
    "unlocks": "A validator diagnostic-presentation decision",
    "statusNote": ""
  }
}
```

## Question

How should an engineer or CI job navigate a large valid set of diagnostics
without hiding errors or changing language conformance?

## Outcome

A focused experiment compares the complete terminal stream with bounded human
presentation and machine-readable output while retaining an explicit way to
obtain every diagnostic.

## Work

- Use the reproducible 1,200-error corpus from Experiment 0014.
- Compare complete text, an explicit limit/summary, and one conventional structured-output shape.
- Exercise local repair, CI attribution, and complete archival use cases.
- Keep diagnostic identity and source coordinates stable across presentations.
- Select at most one bounded validator-interface change or defer it.

## Acceptance evidence

- No error is silently discarded.
- Truncation or summarization is explicit and reports omitted counts.
- Ordinary text remains sufficient for the common small-error case.
- Structured output does not become authoritative requirement state.

## Out of scope

- A generalized reporting framework.
- Persistent diagnostic storage.
- Changing source-language validity.

## Completion decision

Add a focused output mode only if it materially improves the recorded workflow
without complicating the normal terminal interface.

Completed on 2026-08-29. [Experiment 0019](../../experiments/0019-diagnostic-presentation/README.md)
selects future `--max-diagnostics N` behavior while preserving unlimited text by
default, complete validation, exit status, explicit omission counts, and a
complete-stream recovery path. Structured output remains deferred.

## References

- [Experiment 0014](../../experiments/0014-operational-scale/README.md)
- [TC-0704](task-0704-measure-operational-scale.md)
