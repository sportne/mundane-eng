# Task TC-1603: Compile and Validate Work Items

Status: Complete

Roadmap stage: 16

Type: Implementation

Depends on: TC-1602

Unlocks: TC-1604

## Question

Make authored work items available as validated serialized artifacts.

## Outcome

Compile and Validate Work Items for the repository-native issue/task workflow, with reproducible evidence.

## Work

- Implement bounded explicit selection, source parsing, strict metadata validation, duplicate identity detection, source locations and complete-or-empty JSON output; add independent version declarations and native/JVM checks.
- Record the selected behavior and its limitations; each implementation owns its regression tests.

## Acceptance evidence

- Positive/negative source and artifact goldens, Unicode locations, malformed/oversized inputs, deterministic selection and actual output failure checks pass; requirements commands remain independent.
- Record exact checks and artifacts in a completion report; make a separate completion commit.

## Out of scope

- No artifact linking, generated backlog, remote service or broad Markdown interpretation.

## Compatibility and affected components

Human-authored IDs and source remain authoritative. Other artifact languages and
existing requirement/verification interfaces retain their contracts. Likely components:
roadmap cards and indices, work-item source/compiled specifications, a separate
engineering work-item component, public import validation and owning examples/tests.
No directory move is justified solely by this additional artifact kind.

## Completion decision

Stop or narrow scope if the selected workflow requires competing source copies,
automatic satisfaction judgments, or unbounded schema/relationship extensibility.
Unresolved source choices must be decided in TC-1602 before implementation. Preserve
conditional prerequisites as authored policy rather than pretend they are executable.

## References

- [Roadmap](../0001-initial-roadmap.md)
- [Ownership decision](../../research/0037-requirement-and-assertion-ownership.md)
- [Local imports](../../specification/0014-local-artifact-imports-0.1.md)

## Completion evidence

Completed 2026-09-05. [Research 0056](../../research/0056-work-item-compiler-verification.md) records the maintained compiler, versioned serialized boundary, semantic golden, 18 JVM groups and actual native/JVM malformed-input/output checks. Linking and source migration remain separate cards.
