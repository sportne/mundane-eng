# Experiment 0018: Safety Classification Ownership

Status: Completed

Date: 2026-08-29

Roadmap task: [TC-0803](../../roadmap/closed/task-0803-test-safety-classification-ownership.yaml)

## Question

Is a safety-criticality level intrinsic requirement content, a bare membership
list, or a baseline-bound assessment assertion?

## Scheme and scenario

The experiment defines the **Watershed Monitoring Safety Classification Scheme,
revision 1 (`WMSCS-r1`)**. It is a named project scheme created for this study,
not an industry standard or certification framework. Its SC0, SC1, and SC2
levels have explicit project meanings.

Three unchanged requirements are classified in two deployment contexts. In
Baseline B, the bounded hazard-analysis source changes the credited alarm from
`supporting-control` to `required-control`; the corresponding assessment of
`SYS-ALARM-001` changes from SC1 to SC2 for `potable-water-control` while it
remains SC1 for `uncrewed-rural`. The requirement text and its digest do not
change. Assertions bind both the scheme ID and exact scheme-source digest.

## Candidates

The inline candidate places authoritative `safety-assessment` lines inside each
requirement record. Each line still needs an assertion ID, scheme, context,
level, authority, source, and rationale; a scalar `criticality: SC2` would omit
facts needed to interpret the value.

The external candidate records those arguments in `assessments.tsv` and binds
every assertion to the exact requirement-source digest. It is richer than a
file that merely lists SC2 requirement IDs. A disposable derived-inline display
restores local visibility without creating a second authoritative copy.

[`run.sh`](run.sh) uses the native validator, validates scheme levels,
requirement IDs, source digests, authority, uniqueness, and row structure. It
proves that both candidates carry the same facts, compares their ordinary
diffs, and runs requirement- and level-oriented queries.

## Results

Both candidates can represent the facts, but ownership differs:

| Concern | Inline assessment | Separate assertion |
| --- | --- | --- |
| Requirement text unchanged after hazard-analysis revision | Semantically obscured because the requirement record changes | Explicit: requirement source is byte-identical |
| Multiple contexts for one requirement | Repeated compound fields | Natural separate rows |
| Rationale, source, and assessor authority | Must expand the inline field into an assessment object | Belong directly to the assertion |
| Review ownership | Safety-analysis change edits requirement source | Diff separation permits a safety-owned review path (inference; no human review tested) |
| Local display | Immediate | Derived view required |
| One authoritative copy | Inline lines | Assessment rows; derived display is disposable |

Baseline B contains two simultaneous levels for `SYS-ALARM-001`: SC1 in one
deployment context and SC2 in another. Therefore neither value can be a single
context-free scalar of the requirement. The changed level is consequential
under the scheme and carries rationale and authority. A descriptive tag such as
`safety-related` may aid search, but it is not equivalent to this assessed fact.

## Decision

Prefer a separate, baseline-bound assessment assertion when classification has
a scheme, context, rationale, source, or assessor authority, or may change while
requirement text does not. The assessment asserts something about a requirement
revision in a declared project context; it is not meaningful in isolation, but
that does not make it intrinsic requirement content.

Do not add a safety field or generalized attribute mechanism to `.mreq`. Do not
standardize the experimental TSV carrier yet. A derived display or query may
show classifications beside requirements without duplicating authority. An
intrinsic field should be reconsidered only if a real workflow demonstrates one
context-independent authoritative value whose change is intentionally defined
as a requirement change.
