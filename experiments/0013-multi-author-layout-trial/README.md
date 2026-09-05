# Experiment 0013: Multi-Author and File-Layout Trial

Status: Completed controlled trial

Date: 2026-08-29

Roadmap task: [TC-0703](../../roadmap/closed/task-0703-run-the-multi-author-and-layout-trial.yaml)

## Question

How do subject files and one-record-per-file source behave under the same
concurrent Git changes, and does either behavior require file semantics or
special merge machinery?

## Method

`make multi-author-layout-trial` reconstructs eight temporary Git repositories:
four frozen scenarios in each of two layouts. The subject layout is the six-file
Experiment 0011 corpus. The test mechanically splits exactly the same records
into 60 one-record files and requires the frozen semantic fingerprint to remain
equal before any branch is created.

Each history has distinct `Author A`, `Author B`, and administrator Git
identities. Both authors branch independently from the same scenario baseline,
run formatter check, validation, and a relevant `higher` trace, and commit.
The administrator merges both branches, counts actual unmerged files and
conflict markers, resolves source with ordinary edits, reruns all tools, compares
the complete semantic fingerprint with an independently constructed expected
result, inspects `before..after`, and creates annotated `before` and `after`
tags. Global/system Git configuration, signing, hooks, line-ending conversion,
and hash-format selection are isolated.

The histories are controlled simulations with distinct Git authors, not human
effort measurements or live forge reviews. “Changed paths” and source operations
are mechanical observations. Human author/editor experience remains outside
this result and is retained in TC-0706.

The exact environment, tool checksums, invocation, and capture method are in the
[`trial record`](trial-record.md). The generated [`raw evidence`](raw-evidence.txt)
preserves branch/final tool output, merge failures, conflict hunks, resolution
commands, source operations, changed paths, complete ordinary diffs, and history.

## Frozen scenarios

1. **Separated:** independent statement edits to `SENSOR-LEVEL-001` and `AUDIT-QUERY-001`.
2. **Overlap:** competing 45-second and 30-second edits to the same bound in `SYS-ALERT-NOTIFY-001`; review selects 45 seconds.
3. **Move versus edit:** Author A relocates `EDGE-STORE-001`; Author B changes its storage statement; resolution retains both intentions.
4. **Retarget:** a scenario baseline contains `EDGE-ALERT-QUEUE-001`; Author A qualifies its statement while Author B retargets it from `SYS-ALERT-NOTIFY-001` to `SYS-RESILIENT-STORE-001`.

## Observed Git results

| Scenario | Layout | Baseline/final files | Changed paths | Conflicted files | Conflict hunks | Final semantics |
| --- | --- | ---: | ---: | ---: | ---: | --- |
| Separated | Subject | 6 / 6 | 2 | 0 | 0 | Equal |
| Separated | One record | 60 / 60 | 2 | 0 | 0 | Equal |
| Overlap | Subject | 6 / 6 | 1 | 1 | 1 | Equal after selecting 45 seconds |
| Overlap | One record | 60 / 60 | 1 | 1 | 1 | Equal after selecting 45 seconds |
| Move versus edit | Subject | 6 / 6 | 2 | 1 | 1 | Equal after retaining move and edit |
| Move versus edit | One record | 60 / 60 | 2 | 0 | 0 | Equal through Git rename/edit merge |
| Retarget | Subject | 6 / 6 | 1 | 1 | 1 | Equal after retaining statement and retarget |
| Retarget | One record | 61 / 61 | 1 | 1 | 1 | Equal after retaining statement and retarget |

The test pins these observations so a future Git-version or source-shape change
cannot silently rewrite the evidence. It also pins the final cross-layout
semantic fingerprint for every scenario.

## Interpretation

- Separated edits merged cleanly in both layouts because the subject layout also placed them in different files.
- Truly competing edits to one normative line conflicted in both layouts; file granularity cannot remove semantic disagreement.
- One-record layout gave Git a rename it could combine with the other branch's content edit. Moving one record between two multi-record files instead produced one ordinary conflict.
- Nearby statement and relationship edits to the same record conflicted in both layouts. One-record files isolate records from neighbors, not authors from one another.
- The tools accepted 6, 60, and 61 files without assigning path or file-count meaning. Validation counts and semantic fingerprints, not file placement, established equivalence.
- Both layouts used the same number of changed paths in these selected final diffs. One-record layout traded lower move/edit conflict incidence for ten times as many baseline files.

## Decision

Do not mandate a file granularity and do not add path semantics or semantic merge.
One-record-per-file is useful project guidance when record relocation and
independent record ownership are common. Subject files remain reasonable when
coherent reading and lower file count matter. Real overlap still requires an
engineering decision in ordinary Git either way.

No language or production-tool change is justified. Carry layout selection as a repository
convention, and use TC-0704 to measure whether the larger file count has any
material scan or startup cost.
