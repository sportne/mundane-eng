# Task TC-0904: Implement the Selected Ecosystem Tool

```json
{
  "format": "mundane-work-source-0.1",
  "status": "Complete",
  "dependencies": [
    "TC-0905",
    "TC-1204"
  ],
  "relations": [],
  "planning": {
    "stage": "9",
    "type": "Implementation",
    "condition": "TC-0905, TC-1204",
    "unlocks": "TC-0903",
    "statusNote": "Current disposition: Experiment 0024 supplied the repeated workflow evidence\nthat TC-0901 required and selected a focused verification-plan analyzer. TC-0905\nmust define and survive a bounded contract experiment before implementation is\nunlocked."
  }
}
```

## Question

Can the single selected ecosystem capability be delivered as another replaceable tool without expanding the core platform?

## Outcome

One evidence-backed tool is implemented, verified, and documented with an explicit relationship to authoritative source and existing infrastructure.

## Work

- Implement the bounded contract selected by TC-0905, using TC-1204's resolved
  references. Include the selected plan adapter/compiler boundary needed to
  consume authored plans; keep the serialized fixture contract independently usable.
- Reuse shared Java components only where semantic consistency requires it.
- Keep the executable or integration independently usable and removable.
- Ensure outputs are disposable or explicitly external interchange.
- Build with Java 21 and GraalVM Native Image when it is a core command-line tool.
- Run the workflow that justified selection.

## Acceptance evidence

- Published compiled requirement/plan interfaces suffice for a fixture consumer
  without parser-internal access. Golden outputs cover current, stale, uncovered,
  missing-reference, and invalid/partial-input cases with source-linked findings.
- Rebuilding after comment-only, unrelated, normative, ID, and file-move changes
  produces TC-0905's expected binding outcomes. Output failures cannot yield success.
- The tool solves the stated problem in the recorded workflow.
- Removing it does not make authoritative requirements unintelligible.
- It does not absorb unrelated validator, formatter, trace, forge, or editor responsibilities.
- Generated state is reproducible.
- Maintenance and compatibility boundaries are documented separately.

## Out of scope

- Implementing unselected Stage 9 candidates.
- A generalized plugin system.
- A monolithic umbrella command.

## Completion decision

Accept only if the recorded pilot/change fixtures demonstrate the focused
coverage and staleness behavior. Split or stop if unrelated execution, evidence
storage, or satisfaction semantics enter the implementation.

## References

- [TC-0901](task-0901-prioritize-the-next-ecosystem-tool.md)
- [TC-0905](task-0905-define-verification-analyzer-contract.md)
- [TC-1204](task-1204-implement-bounded-artifact-linking.md)
- [TC-0903](task-0903-run-a-derived-presentation-experiment.md)
- [Pilot decision](../../research/0032-end-to-end-pilot-decision.md)
- [Roadmap product direction](../0001-initial-roadmap.md#product-direction)

## Compatibility and affected components

Likely components: the selected verification plan adapter/compiler, analyzer,
artifact consumer, independent CLI boundary, and regression fixtures. Source 0.2
and existing tool interfaces stay intact. Analysis reports planned coverage and
staleness rather than approval or requirement satisfaction.

## Planning refinement

Reconciled on 2026-09-04: retain the selected analyzer implementation and add the
bounded linker dependency. Replace the obsolete TC-1001 unlock with the concrete
report experiment. This card implements the chosen workflow, not an umbrella
engineering platform; removing it must leave requirements tools usable.

## Completion evidence

Completed by [Research 0045](../../research/0045-verification-analyzer-implementation.md), mundane-plan and mundane-verify. `make verification-verify link-verify version-verify` passed with 15 JVM groups, native/JVM parity, the preimplementation plan fixture, 57 assertions, 13 invalid TSV cases, edit/rebuild and output-failure checks. Independent linker/analyzer version provenance was added explicitly to the output contracts. TC-0903 is Ready.
