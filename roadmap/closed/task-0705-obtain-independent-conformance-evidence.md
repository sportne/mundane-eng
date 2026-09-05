# Task TC-0705: Obtain Independent Conformance Evidence

```json
{
  "format": "mundane-work-source-0.1",
  "status": "Complete",
  "dependencies": [
    "TC-0103",
    "TC-0303"
  ],
  "relations": [],
  "planning": {
    "stage": "7",
    "type": "Independent implementation or review",
    "condition": "TC-0303 and TC-0103",
    "unlocks": "TC-1001",
    "statusNote": ""
  }
}
```

## Question

Can the written language contract guide an interpretation independently of the maintained Java implementation?

## Outcome

A second implementation, bounded parser, or rigorous independent implementation review compares semantic results and diagnostics against the conformance corpus.

## Work

- Select the smallest credible independent method without making another production toolchain.
- Provide the normative standard and conformance material without internal Java design guidance.
- Compare accepted source, rejected source, semantic inventories, and source coordinates.
- Record every disagreement as standard ambiguity, fixture gap, or implementation defect.
- Feed only evidence-backed clarifications into the specification.

## Acceptance evidence

- The evidence is independent of copying maintained parser logic.
- Every disagreement has a disposition.
- The written standard, not the reference CLI, remains authoritative.
- The activity does not create a second indefinitely maintained implementation by accident.

## Out of scope

- Feature parity with the native suite.
- A second formatter or trace tool.
- Declaring standards-body status.

## Completion decision

A material interpretation disagreement blocks 1.0 until the standard or implementation is corrected and the conformance evidence agrees.

Completed on 2026-08-29. [Experiment 0015](../../experiments/0015-independent-conformance/README.md)
froze an interpretation from the normative standard before comparing it with
the maintained tool. All 16 validity decisions and both complete semantic
inventories agree. Five construct-level diagnostic coordinates differ only
where the standard intentionally leaves the useful anchor to the implementation.

No standard, fixture, or maintained implementation change is required. The
result is bounded independent-conformance evidence, not human usability evidence.

## References

- [TC-0103](task-0103-audit-the-0.2-conformance-baseline.md)
- [TC-0303](task-0303-publish-the-validator-trial-contract.md)
- [Formalization review](../../research/0008-source-language-formalization-review.md)
