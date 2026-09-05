# Experiment 0010: Integrated Toolchain Trial

Status: Completed

Result date: 2026-08-29

Roadmap task: [TC-0603](../../roadmap/closed/task-0603-run-the-integrated-toolchain-trial.yaml)

## Question

Do the three independent native tools add separable value to a software-like
requirements change while ordinary source and Git remain authoritative?

## Controlled change

The baseline has one operational response requirement decomposed into ingest
and evaluation budgets, each with one software child. The proposed change
tightens the end-to-end response from 250 ms to 100 ms, revises both existing
budgets, adds a separate actuation budget, and adds its software child. The
ordinary source diff therefore exposes changed numbers, two complete added
records, and their authored relationships without specialized output.

The tracked [`baseline`](baseline/requirements.mreq) and
[`proposed`](proposed/requirements.mreq) directories are authoritative trial
inputs. `make integrated-toolchain-trial` reconstructs the following workflow
in a temporary Git repository:

1. commit and annotate `trial-baseline-1` on `main`;
2. create `change/timing-budget` and author the proposed source with CRLF line endings;
3. run formatter write, validator, and `impact OPS-RESPONSE-001`;
4. inspect the ordinary `main...change/timing-budget` source diff as the forge-style review surface;
5. commit, merge with a merge commit, and annotate `trial-baseline-2`;
6. check out the second tag and reproduce formatting, validation, and trace results;
7. delete a derived trace report, reproduce it byte for byte, and confirm a clean checkout;
8. install only formatter and validator, confirm trace is absent, and repeat the complete branch-to-baseline workflow.

System and global Git configuration are disabled; signing and hooks are
disabled; line-ending conversion and object format are fixed; and author
identity and timestamps are fixed. The complete and trace-absent runs must
produce identical branch and tag refs. Git state and all tool output exist only
below the temporary directory and are deleted after assertions. The committed
fixtures and this procedure are enough to reproduce the evidence; no generated
index or report is authoritative.

## Observations

| Observation | Owner | Decision |
| --- | --- | --- |
| CRLF authoring creates whole-file noise until formatting | Source/tool boundary | Run conservative formatting before review; do not change grammar |
| Tightened numbers and added records are obvious in the unified diff after formatting | Source representation | Retain the current record form |
| Validation establishes source and reference integrity but not timing-budget arithmetic | Tool scope/policy | Do not claim engineering consistency; a later policy/analysis experiment may test it |
| Impact output lists all lower-level paths but does not explain whether changed budgets are adequate | Trace-tool scope | Retain bounded navigation semantics |
| Branch, merge, annotated baselines, and reconstruction need no mundane-req Git abstraction | Git | Continue using ordinary Git directly |
| A forge review is simulated from its ordinary source-diff input, not through a live hosted pull request | Trial method | Operational trials should later include real independent authors and forge interaction |
| Repeating the complete workflow without trace loses navigation only; source reading, formatting, validation, diff inspection, merge, tagging, and reconstruction continue | Tool composition | Keep separate executables and shared implementation code |

No training issue was measured because the trial executor is an automated
protocol, not an independent author. TC-0702 owns that question.

## Decision

Proceed to Stage 7. The trial demonstrates additive, disposable capabilities:
formatter removes irrelevant line-ending noise, validator checks the complete
source set, and trace derives impact paths. None owns source, review, history,
or baselines, and removing one does not corrupt the remaining workflow. No
wrapper, database, new language feature, or forge integration is justified by
this trial.
