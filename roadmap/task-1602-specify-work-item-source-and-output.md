# Task TC-1602: Specify Work Item Source and Output

Status: Ready

Roadmap stage: 16

Type: Decision

Depends on: TC-1601

Unlocks: TC-1603

## Question

Select a readable source carrier and explicit compiled boundary without creating duplicate status authority.

## Outcome

Specify Work Item Source and Output for the repository-native issue/task workflow, with reproducible evidence.

## Work

- Compare existing Markdown, structured Markdown and separate data; select a bounded profile, diagnostics, versioned output, imports, command behavior and migration mapping.
- Record the selected behavior and its limitations; each implementation owns its regression tests.

## Acceptance evidence

- Valid/invalid task and issue examples, a complete source/output contract, compatibility rules and lossless prose migration criteria precede implementation.
- Record exact checks and artifacts in a completion report; make a separate completion commit.

## Out of scope

- No compiler implementation or forced YAML platform convention.

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
