# Research 0046: Verification report decision

Date: 2026-09-05. Task: [TC-0903](../roadmap/closed/task-0903-run-a-derived-presentation-experiment.yaml).

## Decision

Retain a standalone experimental static renderer for the retention-change review
workflow. Its first-page findings identify SYS-009 and RDS-002 and link to the
changed requirement values and authored plan assertions. The coverage table,
activity details and provenance support inspecting why those rows need review.
The report's concrete value is bringing these otherwise separate serialized facts
into a navigable review artifact. This is a self-run engineering assessment, not
user feedback or evidence of operational adoption.

A maintained publishing component is not yet justified by this single workflow;
retain the renderer with its executable experiment and extend the integrated
corpus under TC-1501. A separately scoped renderer card should follow a second
concrete delivery need or maintenance demand. No authored sections, selection or
ordering were needed for this review. TC-0807 remains conditional; there is no
basis here for standardizing composition syntax.

## Evidence

[Experiment 0029](../experiments/0029-verification-report/README.md) consumes the
maintained analyzer's public output over the pre-implementation contract fixtures.
The clean input archive was commit `cc987eb` during the recorded run; each rerun
records its own input commit in disposable build output. No tag was created. This
narrow refinement replaces the card's tagged-checkout expectation with equally
immutable inputs and a separately identified installed tool revision.

The [recorded check](../experiments/0029-verification-report/results/verification.txt)
confirms 57 planned assertions, two stale statement bindings, 57 unique requirement
anchors, and 262 valid internal/source-file links. A delete/rebuild produces
identical analyzer and HTML bytes and matches the reviewed golden. Selecting the
current baseline clears the two stale findings. Missing input and five mutations
of completeness, format and row correspondence fail without HTML. HTML-looking
prose and opaque LaTeX remain escaped literals; closed stdout/stderr and a pipe
closed after a prefix return failure. These are actual tests, not claims of visual
usability or compatibility with external report systems.

## Boundaries

Source controls requirement identities and values; the plan controls assertions
and context. The report owns none of them. Exact compiled-input and source digests
identify revisions, not identities. The report records selected formats, tool
contracts, completeness and source locations. Relative link bases are explicit;
readable path/line labels remain when local browsers cannot interpret line anchors.
No test execution, evidence approval, satisfaction inference, custom attribute
syntax, publishing service or general view language was added.
