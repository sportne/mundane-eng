# YAML authoring policy and declaration/plan migration

Normative experimental contract for TC-2201. Requirements, work items, project
attribute declarations and verification plans use YAML. Each owns its fields and
semantics; compiled artifacts and explicit import/editor/work manifests remain JSON.

## Shared presentation policy

Use UTF-8, one document, string mapping keys, mappings/sequences and scalar values.
Comments, block/flow collections and quoted/plain/block strings are supported.
Reject duplicate keys even when values agree, merge keys, anchors, aliases, explicit
tags, directives and multiple documents. Do not coerce numbers or booleans to text.
A field's owning contract determines whether null or a boolean is allowed. Existing
requirement profiles retain their quoted/block-string style rules; work items,
declarations and plans also allow plain string values. Boolean
fields accept only unquoted lowercase `true` and `false`; text that YAML would type
as a number, boolean or null must be quoted. Semantic text rules still apply after
block folding. No defaults or hidden inheritance are introduced by YAML.

Declarations and plans reject BOM, malformed UTF-8, bare CR and missing final LF
(or CRLF). Collection depth is at most 16. Declarations are at most 1 MiB; plans at
most 8 MiB. Fail rather than truncate. Locations use one-based Unicode code-point
coordinates from the YAML parser, not guessed lines or byte offsets.

## Attribute declaration source

A declaration is an explicitly selected YAML file with exactly `format`, `name`,
and `attributes`. Its source format is `mundanereq-attributes-yaml-0.1`. The existing
text/enum, requiredness, descriptions, reserved names and size rules of
[0020](0020-project-attributes-yaml-0.4.md) apply. `required` is a boolean; other
scalar declaration values are strings. The normalized compiled definition keeps
`mundanereq-attribute-schema-0.1`; it is a decoded value contract, not an additional
authoring format. Old JSON declarations carrying that identifier are rejected.
No legacy parser, migration command or parallel JSON declaration source is retained.

## Verification plan source

The existing explicit directory argument selects exactly `plan.yaml`, whose root
contains exactly `format`, `plans`, `activities` and `coverage`. Source format is
`mundane-plan-yaml-0.1`. Each sequence holds at most 10,000 records. At least one
plan and activity are required by the existing compiled contract.

```yaml
format: mundane-plan-yaml-0.1
plans:
  - id: PLAN-LOGGER
    context: logger
    baselineScope: baseline
    currentScope: current
activities:
  - id: ACT-REVIEW
    method: review
    objective: Review logger requirements.
    expectedEvidence: Recorded observations
coverage:
  - planId: PLAN-LOGGER
    activityId: ACT-REVIEW
    requirementId: SYS-001
```

Plan scopes may be omitted or explicitly null; omission normalizes to null as in
the existing optional-scope model. Other fields are required and unknown fields
fail. IDs, methods, context/objective/evidence text and relationships retain
[0015](0015-verification-planning-0.1.md) semantics, including nonempty unpadded
single-line text. Coverage links to local plan/activity IDs; requirement IDs resolve
later through explicit imports. Reject duplicate IDs and coverage triples. Sort
compiled inventories deterministically. Source locations point to each YAML record.
Snapshots are rechecked before complete publication; failure publishes no records.
TSV source is removed. The compiled plan shape remains `mundane-plan-0.1` with the
new source contract; strict consumers accept the current declared source contract.

## Version and verification obligations

Increment plan tool/CLI declarations for the breaking source change and editor
bundle version for YAML declarations. Keep requirements source profiles and compiled
value shapes. Update schema fixtures, source-linked goldens, installed editor tests,
package notices and docs. Test comments, booleans, duplicate/unsupported YAML,
provenance, failed writes, source mutation and JVM/native parity. No TSV export or
JSON source adapter is required. Git retains removed representations if needed later.
