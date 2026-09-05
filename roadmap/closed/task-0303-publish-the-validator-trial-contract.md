# Task TC-0303: Publish the Validator Trial Contract

```json
{
  "format": "mundane-work-source-0.1",
  "status": "Complete",
  "dependencies": [
    "TC-0302"
  ],
  "relations": [],
  "planning": {
    "stage": "3",
    "type": "Decision and documentation",
    "condition": "TC-0302",
    "unlocks": "TC-0601",
    "statusNote": ""
  }
}
```

## Question

Which validator behaviors is the project prepared to present as the first maintained tool interface?

## Outcome

The repository documents, versions, and identifies a trial release of `mundanereq-validate` without extending language stability promises to accidental CLI details.

## Work

- Document installation, invocation, exit behavior, supported source contract, and diagnostic expectations.
- Separate normative source conformance from provisional command-line behavior.
- Record limitations and unsupported policy checks.
- Tag or otherwise identify the reproducible maintained implementation baseline.
- Update README references away from the validation role of the experiment probe.

## Acceptance evidence

- A new user can validate a clean repository from the documentation.
- Language, tool, and Java API compatibility boundaries are stated separately.
- The native binary remains derived and reproducible.
- The release record links its complete test evidence.

## Out of scope

- Formatter or trace packaging.
- A stable 1.0 tool promise.
- Multi-platform binary distribution without demonstrated users.

## Completion decision

Publish as a trial tool only if its supported behavior and limitations can be stated more simply than the experiment it replaces.

## Result

The [validator trial contract 0.1](../../specification/0007-validator-trial-contract-0.1.md)
states the complete supported invocation, selection, diagnostic, and exit
behavior and separates source-language, command-line, and Java compatibility.
The interface is deliberately smaller than the historical experiment probe:
it validates explicit source selections and performs no inventory, formatting,
trace, policy, or interchange work.

`make validator-verify` remains the reproducible evidence gate. The
`validator-trial-0.1` Git tag identifies the reviewed source baseline, while
the executable remains a disposable native build product. The supported
behavior and limitations are sufficiently bounded to publish this as a
maintained trial, without making a stable 1.0 promise.

## References

- [TC-0302](task-0302-verify-validator-behavior.md)
- [Project README](../../README.md)
- [Roadmap Stage 10](../0001-initial-roadmap.md#stage-10--decide-whether-to-stabilize-10)
