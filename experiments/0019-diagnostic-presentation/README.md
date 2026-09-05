# Experiment 0019: Diagnostic Presentation

Status: Completed

Date: 2026-08-29

Roadmap task: [TC-0707](../../roadmap/closed/task-0707-test-bounded-diagnostic-presentation.yaml)

## Question

How should an engineer or CI job navigate the 1,200-error corpus without hiding
errors or changing language conformance?

## Candidates and workflows

[`run.sh`](run.sh) recreates Experiment 0014's 1,200-file invalid corpus and
captures the native validator's complete deterministic stream. The
experiment-only [`present.sh`](present.sh) exercises the proposed limit without
changing the maintained validator. The study compares:

1. the current complete human-readable text;
2. a candidate first-20 presentation followed by an explicit total, omitted
   count, and instruction for recovering the complete stream; and
3. complete JSON Lines with sequence, path, line, column, category, and message.

The local-repair workflow corrects the first file and reruns validation. The
diagnostic count drops from 1,200 to 1,199 and every remaining field equals the
original stream minus that one diagnostic. The structured candidate is parsed
independently and equals all 1,200 canonical diagnostics. The archival workflow
redirects complete text into a compressed build artifact, retrieves it, and
verifies it byte-for-byte without making storage a mundane-req feature.

## Results

The default complete text remains appropriate for ordinary small error sets.
For the 1,200-error case, first-N text materially reduces immediate terminal
volume while saying exactly what was omitted and how to retrieve it. Validation
still processes the complete source set and preserves exit status.

The JSONL projection is mechanically complete and preserves coordinates and
ordering, but it would make provisional categories and message structure into a
machine interface. CI products also have competing native formats. The trial
does not show enough recurring integration need to choose JSONL, SARIF, or a
forge-specific shape now.

## Decision

Select one bounded interface change for a future validator trial:
`--max-diagnostics N`. Omission must be explicit, report shown/total/omitted
counts, preserve the validation exit status, and direct users to rerun without
the option for the complete stream. The default remains unlimited complete
text, so ordinary behavior and archival use do not change.

This card selects behavior; it does not implement or publish it in validator
trial 0.1. Defer structured output until a concrete CI consumer establishes a
stable field contract. Diagnostics and their projections remain derived state,
never authoritative requirement data.
