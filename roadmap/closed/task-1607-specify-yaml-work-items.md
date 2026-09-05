# Task TC-1607: Specify YAML Work Items

```json
{
  "format": "mundane-work-source-0.1",
  "status": "Complete",
  "dependencies": [],
  "relations": [],
  "planning": {
    "stage": "16",
    "type": "Design",
    "condition": "",
    "unlocks": "TC-1608",
    "statusNote": ""
  }
}
```

## Question

The structured Markdown carrier splits facts between a heading and JSON. A YAML document can expose the full record while retaining readable multiline prose.

## Outcome

Select a YAML-compliant work-item specification, structural schema and precise string semantics, replacing the source-carrier decision without widening the work-item model.

## Work

- Record literal, folded and quoted string semantics, YAML restrictions, valid/invalid examples, independent artifact-format ownership and explicit version selection.
- Define structural schema versus domain validation, migration mapping and compatibility with compiled consumers.

## Acceptance evidence

- Written specification and Draft 2020-12 schema describe independently checkable positive and negative cases.
- Decision identifies changes to source/output contracts and rejects silently parsing prose into semantic fields.

## Out of scope

No new work-item lifecycle, inferred prose relationships, requirement attributes, remote tracker, general artifact serialization rule or narrative-section metamodel.

## Compatibility and affected components

Human-authored IDs and source remain authoritative. Requirements and verification plans keep their independent formats. Affected components: work-item specification/schema, compiler, serialized validation, fixtures, native build and backlog authoring helpers. Preserve existing graph meanings and historical evidence.

## Completion decision

The design enables source implementation; implementation enables migration. Stop or revise if migration loses narrative strings, breaks explicit IDs or requires generated source authority. Diagnose unsupported formats explicitly. No future stability promise is implied.

## References

- [Roadmap](../0001-initial-roadmap.md)
- [Previous source decision](../../research/0055-work-item-source-decision.md)

## Completion evidence

[Research 0060](../../research/0060-work-item-yaml-decision.md) records the selected
source profile, worked syntax, independent schema and explicit compatibility.
The schema passes Draft202012Validator.check_schema; runtime checks belong to TC-1608.
