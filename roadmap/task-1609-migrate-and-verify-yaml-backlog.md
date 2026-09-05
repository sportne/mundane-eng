# Task TC-1609: Migrate and Verify the YAML Backlog

```json
{
  "format": "mundane-work-source-0.1",
  "status": "Planned",
  "dependencies": [
    "TC-1608"
  ],
  "relations": [],
  "planning": {
    "stage": "16",
    "type": "Testing and Documentation",
    "condition": "TC-1608",
    "unlocks": "",
    "statusNote": ""
  }
}
```

## Question

The structured Markdown carrier splits facts between a heading and JSON. A YAML document can expose the full record while retaining readable multiline prose.

## Outcome

Make checked-in YAML cards the authoritative repository backlog with repeatable conversion and verification.

## Work

- Convert all cards and template, preserve IDs and narrative strings except recorded link retargeting; update explicit selection and generated index.
- Check conversion against immutable input, update examples/docs and run the authoritative verification command.

## Acceptance evidence

- Every original ID and semantic value survives except recorded source-path/link edits and ordinary completion evidence.
- All cards are selected once; source links and dependency graphs pass; generated view rebuilds identically.
- Independent schema checks, adversarial fixtures, seeded regressions and the authoritative verification command pass with recorded results.

## Out of scope

No new work-item lifecycle, inferred prose relationships, requirement attributes, remote tracker, general artifact serialization rule or narrative-section metamodel.

## Compatibility and affected components

Human-authored IDs and source remain authoritative. Requirements and verification plans keep their independent formats. Affected components: work-item specification/schema, compiler, serialized validation, fixtures, native build and backlog authoring helpers. Preserve existing graph meanings and historical evidence.

## Completion decision

The design enables source implementation; implementation enables migration. Stop or revise if migration loses narrative strings, breaks explicit IDs or requires generated source authority. Diagnose unsupported formats explicitly. No future stability promise is implied.

## References

- [Roadmap](0001-initial-roadmap.md)
- [Previous source decision](../research/0055-work-item-source-decision.md)
