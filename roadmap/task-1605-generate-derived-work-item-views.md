# Task TC-1605: Generate Derived Work Item Views

Status: Ready

Roadmap stage: 16

Type: Implementation

Depends on: TC-1604

Unlocks: TC-1606

## Question

Present the backlog and artifact-to-task navigation without maintaining another editable status inventory.

## Outcome

Generate Derived Work Item Views for the repository-native issue/task workflow, with reproducible evidence.

## Work

- Render a deterministic source-linked view from validated analysis; show statuses, prerequisites and typed links; escape content, preserve authored strategic prose and reject incomplete inputs.
- Record the selected behavior and its limitations; each implementation owns its regression tests.

## Acceptance evidence

- Golden derived view, rebuilt identical bytes, source/navigation checks, hostile text and broken output tests pass. Generated status rows are clearly disposable and source cards remain authoritative.
- Record exact checks and artifacts in a completion report; make a separate completion commit.

## Out of scope

- No editing UI, publishing service, arbitrary Markdown execution or replacement of strategic roadmap narrative.

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
