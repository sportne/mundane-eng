# Task TC-1303: Validate Project-Defined Requirement Attributes

```json
{
  "format": "mundane-work-source-0.1",
  "status": "Ready",
  "dependencies": [
    "TC-1302"
  ],
  "relations": [],
  "planning": {
    "stage": "13",
    "type": "Implementation",
    "condition": "TC-1302",
    "unlocks": "TC-1304, TC-1305",
    "statusNote": ""
  }
}
```

## Question

Projects currently cannot declare or validate the descriptive text/enum values
selected by TC-1301/1302. Free-form YAML keys are rejected, and accepting an untyped
bag would lose the selected vocabulary and ownership boundary.

## Outcome

An explicitly selected YAML source profile and project declaration produce a
fully validated requirement model and source-accurate diagnostics.

## Work

- Publish the normative YAML 0.4 and narrow JSON declaration contracts and structural schemas from Research 0052 before adding behavior. Preserve existing normative contracts; resolve any prose/schema contradiction explicitly.
- Implement explicit schema selection, exact document-name binding, bounded snapshot reading, declaration validation and the two selected value types. No discovery, imports, defaults or inheritance.
- Preserve valid-neighbor recovery, source locations, no-schema operation, requiredness and primary-error suppression when a schema is invalid.
- Integrate validator text and SARIF diagnostics, root/path handling and existing output failure behavior. Define version declarations for the newly supported source/validator contracts independently.
- Add owning positive/negative schema, source-selection, duplicate-key, bound, Unicode and standalone-file regressions in JVM/native commands. Other commands must reject the new selector until their owning cards implement it.

## Acceptance evidence

- Every declaration/attachment/selection rule in Research 0053 has an independently asserted expected result or explicit owning follow-up; required text and enum examples pass, invalid values and duplicate declarations fail with source points.
- Structural schema acceptance and semantic/profile validation agree on their shared constraints; profile-only cases are identified. Missing schema never triggers local-file discovery or silently valid source.
- Invalid/unreadable schemas suppress misleading attribute cascades. Validator exits 0/1/2 and SARIF incompleteness/operational failures match the selected contract, including attempted output failures.
- Existing custom and YAML 0.3 examples remain valid with unchanged invocation behavior; new syntax under old selectors is rejected. Record focused checks plus the authoritative verification result.
- Normative documents, valid/invalid fixtures, golden diagnostics and a completion record are checked in; no passing assessment or satisfaction is inferred from a valid annotation.

## Out of scope

- Formatter, trace, compiled output, linking, report or editor implementation; new attribute types; contextual assessment logic.

## Compatibility and affected components

Add an opt-in source profile, not a reinterpretation of YAML 0.3 or custom 0.2.
Likely components: source selection, YamlRequirements, Interpreter model/provenance,
validator/SARIF, specification/schema, version declarations and owning tests.
Other tool versions do not change merely because the source identifier changes.

## Completion decision

Stop if duplicate evidence is lost before validation, schema errors can yield a
complete model, or selection depends on ambient configuration. Internal model
choices remain open; changes to the selected meaning require a recorded design
revision. Completion enables formatting/trace and compiler work independently.

## References

- [Roadmap](0001-initial-roadmap.md)
- [TC-1302](closed/task-1302-decide-project-attribute-schemas.md)
- [Attribute design](../research/0052-project-attribute-schema-decision.md)
- [Worked cases](../research/0053-project-attribute-design-cases.md)
