# Task TC-0901: Prioritize the Next Ecosystem Tool

```json
{
  "format": "mundane-work-source-0.1",
  "status": "Complete",
  "dependencies": [
    "TC-0603",
    "TC-0704"
  ],
  "relations": [],
  "planning": {
    "stage": "9",
    "type": "Portfolio decision",
    "condition": "TC-0603 and TC-0704",
    "unlocks": "TC-0904 only when this or a successor decision selects a tool; the\ncurrent no-tool decision does not unlock it",
    "statusNote": ""
  }
}
```

## Question

After the core suite and operational trial, which one independent capability removes the most consequential remaining friction?

## Outcome

An evidence comparison selects one next tool or explicitly decides that model research, documentation, or no additional tool has higher value.

## Work

- Collect friction from integrated and operational trials.
- Compare linter, renderer, semantic baseline comparison, model-specific analyzer, editor support, ReqIF conversion, and machine-readable output candidates.
- For each candidate, identify the workflow, authoritative inputs, disposable outputs, and capability already supplied by Git, forge, editor, or CI.
- Estimate conceptual and maintenance cost without designing full architectures.
- Select one bounded experiment and define its stop condition.

## Acceptance evidence

- Every candidate is tied to observed evidence rather than product imitation.
- The selected tool has one primary responsibility.
- No candidate requires owning requirement source or a persistent server by default.
- The decision explains why simpler documentation, policy, or existing infrastructure is insufficient.

## Out of scope

- Implementing the selected tool.
- Committing to the entire ecosystem list.
- Building a plugin platform for future candidates.

## Completion decision

Select at most one next tool. If no candidate materially improves a demonstrated workflow, do not build one.

Completed on 2026-08-29. [Research 0030](../../research/0030-next-ecosystem-tool-prioritization.md)
selects no additional tool and does not unlock TC-0904. Existing evidence does
not identify a candidate that materially improves a demonstrated workflow.
Prioritize the human-author trial TC-0706, then reopen this decision only if its
observations establish a concrete consumer and one primary responsibility.

## References

- [TC-0603](task-0603-run-the-integrated-toolchain-trial.md)
- [TC-0704](task-0704-measure-operational-scale.md)
- [Roadmap Stage 9](../0001-initial-roadmap.md#stage-9--add-focused-ecosystem-tools-only-where-use-justifies-them)
