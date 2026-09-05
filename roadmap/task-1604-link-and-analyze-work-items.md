# Task TC-1604: Link and Analyze Work Items

Status: Ready

Roadmap stage: 16

Type: Implementation

Depends on: TC-1603

Unlocks: TC-1605

## Question

Resolve authored dependencies and supported artifact relationships rather than infer them from arbitrary prose.

## Outcome

Link and Analyze Work Items for the repository-native issue/task workflow, with reproducible evidence.

## Work

- Validate serialized work items and explicit local imports; resolve supported targets, check exact pins, missing references and cycles; report unfinished prerequisites and reverse navigation with authored provenance.
- Record the selected behavior and its limitations; each implementation owns its regression tests.

## Acceptance evidence

- Fixture requirements/plans/work items link via public serialized interfaces. Wrong kind/format, duplicates, invalid completeness, missing targets and cycles fail without partial successful results; closed tasks do not establish requirement satisfaction.
- Record exact checks and artifacts in a completion report; make a separate completion commit.

## Out of scope

- No universal resolver rewrite, source parsing in consumers, automatic closure or interpretation of code/document content.

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

- [Roadmap](0001-initial-roadmap.md)
- [Ownership decision](../research/0037-requirement-and-assertion-ownership.md)
- [Local imports](../specification/0014-local-artifact-imports-0.1.md)
