# Experiment 0022: Reusable Trace Policies

Status: Completed

Date: 2026-08-29

Roadmap task: [TC-0806](../../roadmap/closed/task-0806-test-reusable-trace-policies.yaml)

## Question

Which trace checks are reusable project policy without becoming universal
source-language validity?

## Policies and applicability

The experiment exercises two policies over a six-requirement UAS slice:

- `P-DOWNWARD-001` requires selected higher-level requirements to have at least
  one incoming `decomposes` relationship. `SYS-008` is explicitly waived with
  rationale in [`downward-scope.tsv`](policy/downward-scope.tsv).
- `P-ACYCLIC-001` prohibits cycles among the complete source set listed in
  [`cycle-scope.tsv`](policy/cycle-scope.tsv) because the candidate assurance
  argument terminates at operational roots.

[`policies.txt`](policy/policies.txt) records each rule's scope, rationale, and
misapplication risk. The applicable baseline is the exact experiment source
and policy files in the containing Git commit; its named candidate directories
are the three assessed snapshots.

## Evidence

[`run.sh`](run.sh) validates a passing source set, a set where `SYS-009` loses
all incoming coverage, and a set containing a `GCA-001`/`SYS-005` cycle. All
three conform to source language 0.2. Focused policy checks return status 3 and
emit `policy/downward-coverage` or `policy/decomposition-cycle` diagnostics for
the latter two; they never relabel source as syntactically invalid. A misspelled
or incomplete complete-cycle scope also fails instead of hiding a cycle.

The maintained trace tool successfully reports the cycle as a structural
observation and returns status 0. A project policy can reject that structure
without changing trace navigation or source conformance.

Applicability is essential. Applying downward coverage to all 19 independently
authored FRET LPC-mini requirements reports 19 failures because that repository
slice contains no lower-level decomposition source. These are true violations
of a deliberately inapplicable policy, not checker false positives or defects
in the requirements. Universal “every requirement has a child” policy is
therefore unsound.

## Invocation and configuration result

The checks need small authoritative inputs—exact scope and explicit waivers—but
not a generalized rule language. Focused operations remain a plausible future
interface. The AWK functions are disposable evidence, not a maintained
implementation or selected CLI syntax. Sharing maintained parser/graph code or
choosing a separate executable remains an untested design option.

## Decision

Keep downward coverage and acyclicity outside source-language validity and the
validator. Downward coverage is grounded in Experiment 0003's observed risk
that `SYS-005` and `SYS-009` can lose every child without becoming invalid.
Acyclicity is a boundary-control candidate in this study, not an observed
recurring workflow.

Neither policy has recurred enough to select a maintained implementation. Do
not add policy fields to `.mreq`, an arbitrary rule language, plugin system, or
generalized configuration framework. Continue gathering workflows; if one
recurs, prefer an explicit focused check with separate policy diagnostics.
