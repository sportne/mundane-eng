# Experiment 0014: Operational Scale

Status: Completed

Date: 2026-08-29

Roadmap task: [TC-0704](../../roadmap/closed/task-0704-measure-operational-scale.yaml)

## Question

Do native startup, source scanning, memory use, diagnostics, and trace output
remain practical for the selected 60-requirement operational corpus, and does a
60-file layout materially change that result relative to six subject files?

## Method

[`run.sh`](run.sh) mechanically creates a semantically equivalent one-record
layout and a 1,200-file invalid corpus in a disposable directory. It executes
the three GraalVM native tools as independent processes on:

- formatter check over six and 60 files;
- validation over six and 60 files;
- a two-level higher trace over both layouts;
- a nine-result branching impact trace over both layouts; and
- validation of 1,200 independent source errors.

Each ordinary operation has one first measured process and 20 repeated
processes. The diagnostic case has one first and five repeated processes because
each run emits 1,200 lines. A single persistent Perl helper supplies
nanosecond-resolution monotonic timestamps to the shell around GNU `time` and
the complete child process. This avoids wall-clock adjustments and per-sample
timer-process startup. The elapsed value includes native launch and the small
GNU `time` wrapper. GNU `time` records maximum resident set size.

“Clean” means that every observation launches a new native process with no
retained mundane-req state. It does not mean a cold operating-system page cache:
the semantic and count preflight deliberately reads both layouts before timing,
and the script does not require privileged cache manipulation. The harness
requires expected exit statuses, exact 60/54 validator counts, byte-equal full
normalized semantic inventories, and byte-equal selected trace output between
layouts.

The exact environment, binaries, raw observations, summarized observations,
selected outputs, and checksums are committed under [`results/`](results/).
The source baseline is commit `9639e3dafab795cd70c0a49d5687f745c27d5a52`.

## Observations

On the recorded WSL2/Linux host:

| Operation | Repeated mean, 6 files | Repeated mean, 60 files | Largest regular RSS |
| --- | ---: | ---: | ---: |
| Format check | 3.63 ms | 4.19 ms | 15.3 MiB |
| Validate | 3.43 ms | 3.53 ms | 14.6 MiB |
| Higher trace | 3.38 ms | 3.71 ms | 14.5 MiB |
| Branching impact trace | 3.43 ms | 3.89 ms | 14.5 MiB |

Six first measured processes ranged from 3.2 to 4.6 ms; the first formatter and
first trace launches were approximately 31 ms. An independent rerun reproduced
that first-tool-launch effect, whose cause was not isolated. Repeated means were
3.4–4.2 ms and repeated maxima were at most 6.6 ms. Run-to-run variation and the
small wrapper overhead do not support a fine comparison between layouts; both
occupy the same low-millisecond envelope and do not support a file-layout
performance rule at this scale.

The bounded diagnostic run emitted exactly 1,200 diagnostics and 157,200 bytes.
It took 17–18 ms in the recorded observations and reached at most 21.1 MiB RSS.
The complete output is computationally cheap. Its volume motivates a hypothesis
that an undifferentiated terminal stream is difficult to navigate; TC-0707 must
test that hypothesis in concrete local-repair, CI, and archival workflows. A
ten-line normalized sample and exact output shape are preserved without
committing temporary absolute paths.

The deep trace is three lines and 165 bytes. The branching trace is ten lines
and 565 bytes. Both are compact and byte-identical across layouts; this trial
does not substitute that size observation for human usability evidence.

## Workflow interpretation

These are case measurements, not portable performance promises. On this host,
every ordinary command returns in a low-millisecond envelope. No recorded
process-level delay suggests that startup or full in-memory reconstruction
would block the selected editor/CI workflow, but TC-0706 remains responsible for
human experience. The measurement gives no reason to add persistent indexing,
caches, a database, or path semantics.

Large diagnostic sets suggest a possible output-navigation problem rather than
a processing problem. TC-0707 will test bounded presentation and
machine-readable diagnostic alternatives without weakening complete validation
or absorbing the issue into the source language.

## Decision

Retain independent processes and the disposable in-memory semantic model.
Retain non-semantic file granularity. Do not add performance infrastructure at
the selected scale. Re-measure before changing that decision when a real corpus
or workflow is materially larger.
