# Task TC-0104: Select the Maintained Implementation Lineage

```json
{
  "format": "mundane-work-source-0.1",
  "status": "Complete",
  "dependencies": [
    "TC-0103"
  ],
  "relations": [],
  "planning": {
    "stage": "1",
    "type": "Decision",
    "condition": "TC-0103",
    "unlocks": "TC-0201",
    "statusNote": ""
  }
}
```

## Question

Should the first maintained implementation evolve from the experimental probe or be constructed alongside it and compared through conformance evidence?

## Outcome

A short decision record selects the implementation lineage and states which experiment code may be reused, retained as an oracle, or retired.

## Work

- Assess the 796-line probe's coupling among CLI, source discovery, parsing, validation, model, inventory, and trace query.
- Assess whether incremental extraction can preserve test evidence more safely than a parallel implementation.
- Identify ReqIF probe duplication that should not dictate the core architecture.
- Compare the smallest transition paths against formatter and separate-executable needs.
- Record the selected path and rollback condition.

## Acceptance evidence

- The decision names the maintained source location to be created by TC-0201.
- Reuse is justified component by component rather than by implementation convenience alone.
- Historical experiment reproducibility is preserved.
- The decision does not claim a stable Java API.

## Out of scope

- Performing the extraction.
- Selecting generalized compiler frameworks.
- Changing executable behavior or source semantics.

## Completion decision

Favor evolution when behavior can remain continuously covered; favor a parallel implementation when extracting concrete syntax and tool boundaries would otherwise distort the experiment. Do not maintain two implementations indefinitely without a stated comparison purpose.

## Result

[Research 0011](../../research/0011-maintained-implementation-lineage.md) selects
a controlled extraction from the Experiment 0002 probe into one maintained
implementation. The probe remains a temporary behavioral oracle through
TC-0204 but will not become a product dependency or dictate class boundaries.

The decision assigns each probe concern to extraction, migration evidence, or
a later tool; keeps the ReqIF adapter outside the core; names `src/main/java`,
`src/test/java`, `build/maintained`, and the root `Makefile` as the maintained
project boundaries; and defines a rollback if controlled extraction would
create persistent duplicate parsers or distort requirement semantics. No Java
API or experimental option syntax is declared stable. A minimal cross-tool
exit convention is fixed, and annotated tag `reference-probe-0.2-audited`
pins the exact corrected oracle at commit `7cf9c58`.

## References

- [TC-0103](task-0103-audit-the-0.2-conformance-baseline.md)
- [Experiment probe source](../../experiments/0002-deterministic-interpretation/src/mundanereq/Probe.java)
- [Roadmap implementation hypothesis](../0001-initial-roadmap.md#first-implementation-hypothesis)
