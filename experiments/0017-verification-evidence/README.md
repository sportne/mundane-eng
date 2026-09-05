# Experiment 0017: Verification Planning and Evidence

Status: Completed

Date: 2026-08-29

Roadmap task: [TC-0802](../../roadmap/closed/task-0802-model-verification-planning-and-evidence.yaml)

## Question

What minimum human-readable model distinguishes planned coverage, activity
identity, execution, evidence, and result for exact requirement baselines?

## Candidate model

The experiment uses small tab-separated companion artifacts, not `.mreq`
fields or a proposed general metadata facility:

- `activities.tsv` defines stable activities independently of requirements;
- each `plan.tsv` binds a plan to exact requirement and activity source
  SHA-256 digests;
- `coverage.tsv` explicitly relates a plan, activity, and requirement ID;
- `executions.tsv` records that an activity ran under a configuration;
- `evidence.tsv` records an evidence locator and integrity value for an
  execution; and
- `results.tsv` records an outcome and basis for an execution.

The TSV spelling is an experimental carrier. The selected result is the
separation and ownership of facts, not a standardized second language.

## Pressure workflow

Baseline A has three requirements, two planned requirements, one uncovered
requirement, and one planned activity that is not executed. One activity
execution has evidence and a passing result; a second execution has neither
evidence nor a result. Baseline B tightens the level-error statement, adds a
weather requirement, reuses the same activity definitions, updates coverage,
and deliberately leaves the new requirement uncovered.

[`run.sh`](run.sh) uses the native validator, validates every reference and
digest, renders coverage queries, reports execution states, and evaluates Plan A
against Baseline B. The old plan fails as stale even though relevant requirement
IDs remain unchanged. Ordinary diffs show the requirement, plan binding, and
coverage changes separately.

## Results

The model distinguishes these facts without inference from a single status:

| State | Evidence in source |
| --- | --- |
| Planned | Requirement/activity row in baseline-bound coverage |
| Executed | Execution with activity, plan, configuration, and time |
| Evidenced | Separate evidence row for that execution |
| Passing | Separate result row with `pass` and basis |

`EXEC-A-001` is planned, executed, evidenced, and passing. `EXEC-A-002` is
planned and executed but is neither evidenced nor passing. An uncovered
requirement is not planned. The experiment deliberately does not derive a
"passing requirement" state from a passing activity execution.

The requirement source is not touched when execution, evidence, or result rows
are added. Activities are reusable because both plans bind the same activity
source digest. Coverage becomes stale through an exact source-digest mismatch,
not through a manually maintained per-requirement revision field.

## Decision

Select the companion conceptual model: activity, baseline-bound plan, coverage,
execution, evidence reference, and result are separate objects or assertions.
Keep all of them outside requirement records.

The model is small and source-first enough to retain for further experiments,
but do not standardize these TSV files yet. A second workflow and a focused
verification analyzer should test whether the file boundaries and digest
bindings remain comfortable. Do not claim that an evidence locator proves the
evidence exists, that `pass` means a product is certified, or that one passing
execution universally satisfies a requirement.
