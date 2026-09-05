# Task TC-1601: Define Work Item Ownership

Status: Complete

Roadmap stage: 16

Type: Decision

Depends on: TC-1102, TC-1203

Unlocks: TC-1602

## Question

Decide the difference between an issue reporting a problem and a task planning work, using the actual backlog.

## Outcome

Define Work Item Ownership for the repository-native issue/task workflow, with reproducible evidence.

## Work

- Record identity, lifecycle authority, dependency and typed relationship meanings; distinguish completion claims from satisfaction; preserve historical qualifications.
- Record the selected behavior and its limitations; each implementation owns its regression tests.

## Acceptance evidence

- A worked issue/task/requirement example and an inventory of existing cards support explicit include/defer decisions. Missing targets, cycles, supersession and evidence have stated meanings.
- Record exact checks and artifacts in a completion report; make a separate completion commit.

## Out of scope

- No syntax, implementation, automated approval or universal issue-management workflow.

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

Completed 2026-09-05. [Research 0054](../../research/0054-work-item-ownership.md) records the 72-card input inventory, task/issue ownership, lifecycle and relation meanings, worked example and scope limits. TC-1602 is Ready; no implementation is claimed.
