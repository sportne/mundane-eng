# Task TC-1306: Link and Analyze Project Attributes

```json
{
  "format": "mundane-work-source-0.1",
  "status": "Planned",
  "dependencies": [
    "TC-1302",
    "TC-1305"
  ],
  "relations": [],
  "planning": {
    "stage": "13",
    "type": "Implementation",
    "condition": "TC-1302, TC-1305",
    "unlocks": "TC-1307, TC-1308",
    "statusNote": ""
  }
}
```

## Question

The maintained linker/analyzer accepts only current fixed-field requirement
artifacts and compares seven values. Attribute or schema-meaning changes would be
missed unless serialized validation and review comparison evolve explicitly.

## Outcome

Imported schema snapshots remain independent by scope, and changed attributes or
schema definitions produce an explicit review-stale finding.

## Work

- Extend strict serialized validation to the selected output 0.2 schema/attribute types, requiredness, completeness, provenance and locations; do not import parser classes or reload schema source.
- Preserve explicit scopes and exact pins. Reject unsupported/malformed/incomplete artifacts before publishing successful edges or results; same-name schemas from different scopes never merge.
- Compare eight value fields plus the whole canonical schema definition under the selected conservative policy; publish new linked/verification output identifiers wherever nested formats or result meanings require them.
- Support explicit promotion of old output 0.1 to absent attributes/null schema for cross-format comparison. Keep historical comparisons unchanged.
- Identify changed attributes/schema definitions in machine-readable findings sufficient for TC-1307's source-linked report; retain original assertion authority and provenance.

## Acceptance evidence

- Serialized tampering cases for undeclared keys, invalid enum, required omission, forged completeness, bad locations and unsupported schema format fail before analysis.
- Changing owner-team, enum vocabulary, description or requiredness yields the specified stale/invalid outcomes. Comment/reordering/path-only edits remain current; schema definition changes are not ignored as metadata.
- Equal old/schema-free new snapshots compare current; old/schema-selected new snapshots compare stale. Scope ambiguity, wrong pins and ID corrections retain existing failure meanings.
- Published output/CLI contracts, golden findings and migration notes explain the conservative whole-schema review policy and explicit format support. Readers requiring the old result format reject new meanings.
- Actual public JVM/native workflow checks and the authoritative gate pass; compilation/linking still makes no assessment, execution or satisfaction claim.

## Out of scope

- Configurable per-attribute comparison policies, schema merging/import registries, assessment inference, report implementation.

## Compatibility and affected components

Requirement, linked and verification formats evolve independently. The plan TSV
language and human IDs need no change. Likely components: artifact validation,
resolver/analyzer, version declarations, specification 0014/0015 successors and
owning tests. Requirements-only commands remain independent.

## Completion decision

Stop if old consumers could silently apply seven-field comparison to new values.
Whole-schema comparison may cause extra reviews; document this rather than narrow
it without evidence. Completion enables attribute-aware reporting and integration.

## References

- [Roadmap](0001-initial-roadmap.md)
- [TC-1302](closed/task-1302-decide-project-attribute-schemas.md)
- [Attribute design](../research/0052-project-attribute-schema-decision.md)
- [Worked cases](../research/0053-project-attribute-design-cases.md)
