# Task TC-0301: Implement the Validator Executable

```json
{
  "format": "mundane-work-source-0.1",
  "status": "Complete",
  "dependencies": [
    "TC-0204"
  ],
  "relations": [],
  "planning": {
    "stage": "3",
    "type": "Implementation",
    "condition": "TC-0204",
    "unlocks": "TC-0302",
    "statusNote": ""
  }
}
```

## Question

Can one focused native executable answer whether an explicitly selected source set conforms to mundane-req 0.2?

## Outcome

`mundanereq-validate` exposes the maintained parser and source-set validator through a small, documented command-line interface.

## Work

- Accept explicit file and directory inputs according to source-selection rules.
- Emit plain source-positioned conformance diagnostics.
- Return documented success, invalid-source, and invocation-error exit statuses.
- Identify the implemented source contract.
- Optionally emit concise selection and count summaries without standardizing inventory as interchange.

## Acceptance evidence

- Valid source exits successfully with no hidden state.
- Invalid source exits unsuccessfully with useful file, line, and column information.
- Invocation failures are distinguishable from source invalidity.
- The executable builds and runs as a standalone GraalVM native image.
- No policy checks are presented as language failures.

## Out of scope

- Formatting, trace queries, linting, or ReqIF.
- A daemon or watch service.
- Stable machine-readable diagnostics unless a concrete integration requires them.

## Completion decision

Keep the CLI only as large as required for clean-checkout validation and CI. Defer convenience modes that conflate another tool's responsibility.

## Result

`mundanereq-validate` now accepts explicit file and directory inputs, invokes
the maintained strict interpreter, emits plain `file:line:column: category:
message` diagnostics, and identifies `mundanereq-source-0.2` in both successful
summaries and `--version` output. Exit status `0` means conforming source, `1`
means source nonconformance, and `2` means invalid invocation or incomplete
evaluation caused by an unavailable or empty input selection.

The executable has its own `native-validator` GraalVM target and contains no
formatting, trace, policy, inventory-interchange, daemon, or hidden-state
behavior. Focused JVM tests cover success, source failure, operational failure,
usage, help, and contract identification; TC-0302 owns exhaustive JVM/native
verification and repair-workflow evidence.

## References

- [TC-0204](task-0204-port-tests-and-prove-native-tool-boundaries.md)
- [Roadmap Stage 3](../0001-initial-roadmap.md#stage-3--deliver-mundanereq-validate)
- [0.2 trial contract](../../specification/0006-provisional-0.2-contract.md)
