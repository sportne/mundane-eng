# Task TC-0101: Strengthen Unicode Conformance Fixtures

```json
{
  "format": "mundane-work-source-0.1",
  "status": "Complete",
  "dependencies": [],
  "relations": [],
  "planning": {
    "stage": "1",
    "type": "Implementation and conformance",
    "condition": "None",
    "unlocks": "TC-0102 and TC-0103",
    "statusNote": ""
  }
}
```

## Question

Do the normative Unicode whitespace, C1 control-character, and Unicode-scalar coordinate rules have executable examples that distinguish conforming from nonconforming interpreters?

## Outcome

The 0.2 conformance corpus contains focused fixtures for every known Unicode edge condition exposed by formalization.

## Work

- Add invalid fixtures covering prohibited C1 controls and non-ASCII boundary whitespace.
- Add a diagnostic fixture whose reported column follows a supplementary Unicode scalar value.
- State the expected validity and coordinate behavior in the conformance README.
- Keep each fixture focused enough that one violated rule explains its result.

## Acceptance evidence

- Every new fixture is linked from the 0.2 conformance documentation.
- The fixtures fail against at least the known pre-repair behavior where applicable.
- Expected source coordinates are written independently of Java UTF-16 indexing.
- `git diff` makes the added character data reviewable, using escaped explanatory documentation where raw characters would be misleading.

## Out of scope

- Changing the language standard.
- Repairing the Java parser.
- Expanding diagnostics beyond the three known discrepancies.

## Completion decision

If a normative rule cannot be represented or reviewed reliably in a conformance fixture, reopen that rule explicitly. Otherwise use the fixtures as the acceptance boundary for TC-0102.

## Result

Completed with four focused invalid fixtures:

- standalone prohibited C1 control U+0080;
- leading and trailing U+00A0 scalar-boundary whitespace;
- a prohibited U+007F after U+1F600, with the normative diagnostic position
  recorded as line 2, column 10.

The 0.2 conformance README records the literal code points and expected
behavior so the intentional invisible characters remain reviewable. The
pre-repair reference probe accepts the first three fixtures and reports column
11 rather than column 10 for the fourth, demonstrating that the fixtures
distinguish the known nonconforming behavior.

## References

- [Roadmap Stage 1](../0001-initial-roadmap.md#stage-1--establish-an-exact-02-implementation-baseline)
- [Formalization review](../../research/0008-source-language-formalization-review.md)
- [0.2 conformance suite](../../conformance/0.2/README.md)
