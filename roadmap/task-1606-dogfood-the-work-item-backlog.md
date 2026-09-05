# Task TC-1606: Dogfood the Work Item Backlog

Status: Planned

Roadmap stage: 16

Type: Verification and Documentation

Depends on: TC-1603, TC-1604, TC-1605

Unlocks: none

## Question

Use this repository’s real backlog to verify the new workflow and retire manually duplicated status tables.

## Outcome

Dogfood the Work Item Backlog for the repository-native issue/task workflow, with reproducible evidence.

## Work

- Migrate existing cards with a reviewed inventory preserving human IDs, titles, prose, dependency intent and historical evidence; generate the indexed view, preserve strategic narrative, document commands and add bounded regression/integration checks.
- Record the selected behavior and its limitations; each implementation owns its regression tests.

## Acceptance evidence

- All original cards plus this batch compile/link/render; migration preserves prose and records every metadata transformation. Clean-checkout authoritative verification, source-to-view rebuilds, negative cases and relative links pass with truthful evidence.
- Record exact checks and artifacts in a completion report; make a separate completion commit.

## Out of scope

- No implementation of requirement attributes, historical evidence rewriting, major-version milestone or external issue tracker synchronization.

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
