# Experiment 0020: Allocation Model

Status: Completed

Date: 2026-08-29

Roadmap task: [TC-0804](../../roadmap/closed/task-0804-test-allocation-and-controlled-vocabulary.yaml)

## Question

When does an allocation label need controlled vocabulary, stable target identity,
or allocation-relationship semantics?

## Scenario and candidates

The experiment replays Experiment 0003's recorded `SYS-004` reallocation and
adds heterogeneous service, adapter, and human-role targets. It compares:

1. the current opaque `allocation` label;
2. the same label constrained by a separate project vocabulary; and
3. companion target and allocation tables with stable target IDs.

The recorded reallocation is observed evidence. The rename and multi-target
cases are deliberately synthetic model-pressure scenarios, not evidence that a
project has yet needed those capabilities. The plain-label baseline renames `Vehicle manager` to `Vehicle registry service`
on two requirements and reallocates `SYS-004` from `Flight-plan manager` to
`Authorization service`. The referenced candidate makes the rename a single
target-record change and the reallocation a single assertion change. It also
represents `SYS-012` with a primary adapter and a supporting service, which the
one-value source field cannot express without inventing compound text.

[`run.sh`](run.sh) proves that the normative statements are identical between
candidates, validates the `.mreq` files with the native validator, checks both
companion tables independently, and records ordinary diffs. A misspelled
`Authorisation service` remains valid source-language text but fails the
separate vocabulary policy, demonstrating that policy conformance is not
language conformance.

## Results

| Workflow | Plain label | Project vocabulary | Identified companion |
| --- | --- | --- | --- |
| Visible reallocation | Clear one-line source edit | Same, with allowed-value checking | Clear one-line assertion edit |
| Target display rename | Edits every affected requirement | Edits vocabulary and every source occurrence | Edits one target record; assertions remain stable |
| Typo detection | None by design | Detected without grammar changes | Detected as an unresolved target reference |
| Multiple responsible targets | Not represented by one field | Still not represented | Can store two separate assertions |
| Target continuity | Display text only | Vocabulary entry has no durable reference identity | Stable target ID distinct from display name |

The companion target is deliberately only an identified allocation endpoint:
ID, display name, and a small project policy type. It does not model component
structure, interfaces, behavior, deployment, or SysML semantics. The
`primary`/`supporting` roles illustrate readable rows; the experiment does not
select their semantics, uniqueness rules, or assertion identity.

## Decision

Keep `allocation` as an optional opaque label in source language 0.2. It is the
simplest readable representation for a singular display-oriented allocation,
and the recorded real reallocation remains a useful ordinary Git diff.

Use a separate project vocabulary when a repository only needs spelling and
allowed-value control. The synthetic cases show that a separate companion with
stable target references *could* preserve rename continuity and store multiple
allocation assertions, but they do not demonstrate that mundane-req needs it.
Defer that companion until an observed workflow establishes the need. Do not
standardize the experimental TSV carrier, target types, or allocation roles,
and do not add grammar syntax.
