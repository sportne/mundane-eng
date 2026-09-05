# Task TC-0703: Run the Multi-Author and Layout Trial

```json
{
  "format": "mundane-work-source-0.1",
  "status": "Complete",
  "dependencies": [
    "TC-0701"
  ],
  "relations": [],
  "planning": {
    "stage": "7",
    "type": "Git workflow trial",
    "condition": "TC-0701",
    "unlocks": "TC-0704",
    "statusNote": ""
  }
}
```

## Question

Do the source language and tool suite remain workable across concurrent changes and different non-semantic file granularities?

## Outcome

A recorded controlled two-author Git history compares subject files and one-record-per-file layouts through branches, conflicts, moves, ordinary diffs, and baseline comparison.

## Work

- Prepare semantically equivalent corpus layouts.
- Assign separated, overlapping, move-versus-edit, and relationship-retargeting changes to independent branches.
- Run formatter, validator, and trace analysis on each branch.
- Resolve conflicts using ordinary Git source operations and preserve forge-style ordinary diffs.
- Compare semantic inventories and mechanical source/file operations across layouts.
- Establish and compare annotated baselines.

## Acceptance evidence

- Equivalent layouts retain equal semantic models.
- Conflict behavior and file-operation overhead are recorded rather than inferred.
- No path or file-count semantics enter the tools.
- Ordinary Git resolution remains sufficient or a precise counterexample is preserved.
- Recommendations remain project conventions unless semantic evidence requires otherwise.

## Out of scope

- Semantic merge machinery.
- Mandating one file granularity.
- Benchmarking every forge.

## Completion decision

If one layout is operationally preferable, document it as guidance without changing the language unless file boundaries demonstrably need semantics.

Completed on 2026-08-29. [Experiment 0013](../../experiments/0013-multi-author-layout-trial/README.md)
reconstructs eight two-author Git histories across semantically equivalent
six-subject-file and 60-one-record-file layouts. Separated edits merged in both;
true overlap and nearby same-record retargeting conflicted in both; Git merged a
one-record rename with an independent edit while the equivalent multi-record
move/edit produced one ordinary conflict. Every resolved cross-layout semantic
fingerprint is equal.

Keep file granularity non-semantic and project-selected. Do not add semantic
merge, path meaning, or a required layout. The harness records mechanical Git
behavior rather than human effort; TC-0706 retains that gap.

The frozen protocol's exact tool identities, statuses, validator and trace
output, conflict hunks, resolution commands, ordinary diffs, and authored source
operations are preserved in the experiment's
[`trial record`](../../experiments/0013-multi-author-layout-trial/trial-record.md).
Live forge review and human author-effort evidence were not silently treated as
complete; both remain explicitly assigned to TC-0706.

## References

- [TC-0701](task-0701-select-a-larger-corpus-and-trial-protocol.md)
- [Experiment 0001 merge review](../../experiments/0001-source-representations/concurrent-edit-merge-review.md)
