# Task TC-0503: Verify Trace Graph Behavior

```json
{
  "format": "mundane-work-source-0.1",
  "status": "Complete",
  "dependencies": [
    "TC-0502"
  ],
  "relations": [],
  "planning": {
    "stage": "5",
    "type": "Verification and workflow trial",
    "condition": "TC-0502",
    "unlocks": "TC-0504",
    "statusNote": ""
  }
}
```

## Question

Are trace results deterministic, complete for their stated operation, and useful during realistic requirement changes?

## Outcome

Graph fixtures and sustained-authoring scenarios demonstrate correct behavior for branching, convergence, cycles, missing IDs, and changed relationships.

## Work

- Add focused graphs with multiple parents, multiple shortest paths, disconnected nodes, self-cycles, and longer cycles.
- Define expected output independently of file traversal order.
- Replay addition, split, retargeting, coordinated-change, and retirement scenarios.
- Compare tool output with manual `rg` and source inspection.
- Check JVM and native output equality.

## Acceptance evidence

- Every selected graph operation has positive and edge-case fixtures.
- Results are deterministic across file layouts and input ordering.
- Cycle reporting does not redefine cycles as language-invalid.
- Workflow review identifies concrete navigation improvement over manual search.
- No output depends on a persistent index.

## Out of scope

- Decomposition completeness policy.
- Verification and assessment traceability.
- Organizational-scale performance.

## Completion decision

Nondeterministic or misleading path output blocks publication. Revise the interface rather than hide ambiguity behind traversal order.

## Result

Completed on 2026-08-29. [Experiment 0009](../../experiments/0009-trace-workflows/README.md) records independently expected results for branching, convergence, tied shortest paths, disconnected nodes, reachable and unreachable cycles, file-layout changes, and reversed input ordering. `make trace-verify` requires exact JVM/native agreement and replays addition, split/retarget, coordinated content change, and retirement workflows without a persistent index. The selected operations remained deterministic and materially reduced repeated reverse source searches, so they may advance unchanged to a trial contract.

## References

- [TC-0502](task-0502-implement-the-trace-executable.md)
- [Experiment 0003](../../experiments/0003-sustained-authoring/README.md)
- [Experiment 0009](../../experiments/0009-trace-workflows/README.md)
