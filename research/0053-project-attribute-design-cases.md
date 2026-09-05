# Research 0053: Project attribute worked design cases

Date: 2026-09-05. Evidence for [TC-1302](../roadmap/closed/task-1302-decide-project-attribute-schemas.yaml)
and [Research 0052](0052-project-attribute-schema-decision.md).

Status: Reviewed design examples and expected outcomes, not implemented fixtures
or reported feature-test results. Each substitution below is independent and starts
from the complete JSON/YAML examples in Research 0052. That example's two IDs and
project vocabulary are illustrative; they do not change the checked-in pilot.

## Positive cases and selected representations

P01: compile the two-record YAML example with its JSON declaration explicitly
selected. Expected: SYS-001 has enum discipline software and text owner-team Logger
firmware; SYS-002 has discipline electronics and no owner-team value. One schema
snapshot covers both records. Both requirements retain their human IDs.

The new semantic portion for SYS-002 is exactly:

```json
{"attributes":{"discipline":"electronics"}}
```

This is a fragment of `values`, not a complete compiled artifact. The missing
optional text value is not null, empty, inherited or defaulted. The enclosing schema
explains that discipline is an enum and lists the allowed values. The seven existing
fields and locations remain required by the output contract.

P02: a schema-free YAML 0.4 file remains useful:

```yaml
format: "mundanereq-yaml-0.4"
requirements:
  - id: "REQ-1"
    title: "Record data"
    statement: "The logger shall record data."
```

With no schema option this is valid and compiles with `attributeSchema: null` and
`values.attributes: {}`. With the example schema option it fails because its schema
name is missing; a command cannot silently opt a file into a project vocabulary.

P03: with both example declarations changed to `required: false`, an opted-in
record may omit its entire `attributes` field. Its document still names
`logger-metadata`; the explicit option still selects that definition. Optional
omission never bypasses project selection.

P04: quoted Unicode text such as `"Équipe capteurs"` is a valid owner-team label.
Quoted `"7"` is also valid text; unquoted numeric `7` is invalid. A folded scalar
whose decoded value is `Logger firmware` is valid; one decoding to two lines is
invalid. No trimming, numeric coercion or normalization is performed.

P05: an enum may explicitly declare `"unknown"`; only then is that spelling a
valid value. It is distinct from omission. `"high"` and `"low"` may be legal labels,
but type validity does not establish an assessed safety level or rank them.

## Declaration cases

All invalid declaration cases report a primary schema diagnostic, suppress
attribute-value cascades, and prevent a complete semantic artifact or source write.
For missing keys, use the containing object start; otherwise use the offending
JSON token. The reader must preserve duplicate-key evidence before object creation.

| Case | Concrete independent change | Expected outcome |
| --- | --- | --- |
| D01 | Remove format, name or attributes | Invalid required root shape |
| D02 | Change format to `mundanereq-attribute-schema-0.9` | Unsupported declaration version; no fallback |
| D03 | Set name to `Logger`, `logger_metadata`, `-logger`, `logger--metadata`, empty, or 65 `a` characters | Invalid schema name |
| D04 | Rename owner-team to `Owner`, `owner_team`, `owner--team`, empty, or 65 `a` characters | Invalid attribute name |
| D05 | Rename owner-team to `id`, `allocation`, `attributes`, `attribute-schema`, `mreq-owner` or `mundane-owner` | Reserved-name conflict, not override |
| D06 | Repeat root name, an attribute name, or a declaration's type key | `attribute-schema-duplicate`, even if repeated values agree |
| D07 | Set attributes to an empty object, a list or null | Invalid declaration collection |
| D08 | Remove type, required or description from owner-team | Missing required declaration field |
| D09 | Set type to `integer`, `list`, `url`, `reference`, `boolean` or `Text` | Unsupported type |
| D10 | Set required to `"true"`, null or 1 | Requiredness is a JSON boolean |
| D11 | Set description to empty, padded or decoded multiline text | Invalid descriptive text |
| D12 | Remove enum values, or use empty list, string or null | Invalid enum declaration |
| D13 | Set enum values to `["software", "software"]` | Duplicate member, not duplicate removal |
| D14 | Add enum member empty string, numeric 7, null, padded string or decoded newline | Invalid member type/text |
| D15 | Add values to owner-team's text declaration | Forbidden key for text |
| D16 | Add default, pattern, imports, extends, `$ref` or another unknown key | Invalid, never ignored; no implicit values or schema composition |
| D17 | Add a JSON comment, trailing comma, BOM, malformed UTF-8 or omit final LF/CRLF | Invalid declaration input |
| D18 | Add escaped control or unpaired surrogate to description/value | Invalid decoded Unicode/text |
| D19 | Supply 129 distinct valid declarations, 257 enum members, over 1 MiB, or depth over 16 | Explicit bounded failure, never truncation |
| D20 | Rename schema to `logger-v2`, keep old YAML name | Mismatch until source is explicitly updated; no alias |
| D21 | Reorder keys/enum members or use CRLF instead of LF | Valid, equal canonical definition and changed exact byte provenance |

Implementation fixtures must exercise exactly-at-limit and one-over-limit inputs
for each independent bound with other constraints satisfied where possible. Depth
can fail before shape validity; it must still fail boundedly and without publication.
No capacity or performance measurement is claimed by these design cases.

## Requirement values and project selection

| Case | Concrete independent change | Expected outcome |
| --- | --- | --- |
| S01 | Delete SYS-001 discipline | `attribute-required` at its record start |
| S02 | Set discipline to `Software` or `thermal` | `attribute-value`: exact membership |
| S03 | Set owner-team to null, number 7, list, object, empty, padded or decoded multiline value | `attribute-value` or the owning YAML profile diagnostic; never coerced |
| S04 | Repeat discipline key with equal/different values | Existing `yaml-duplicate-key` at repeated key |
| S05 | Add priority with value high, or invalid name Owner | `attribute-unknown` at key |
| S06 | Use attributes null, list or empty object | Invalid attachment shape; omit instead of empty map |
| S07 | Remove the required discipline declaration and leave its source value | Unknown attribute; no free-form bag |
| S08 | Name logger-metadata but omit the schema option | `attribute-schema-required`, no directory search |
| S09 | Select schema but omit document name, set it to null, or use other-project | Invalid name/shape or `attribute-schema-mismatch`; no implicit selection |
| S10 | One of two selected documents names other-project | Fail selected set; compile separately if they are different projects |
| S11 | Omit schema option and name but keep attributes | Invalid schema-free attachment |
| S12 | Pass the same schema option twice | Invocation error 2, no output artifact |
| S13 | Pass a directory/nonexistent/unreadable schema path | Operational error 2, no complete model |
| S14 | Put schema outside compiler/SARIF root or behind an escaping symlink | Invocation/path rejection, not implicit root expansion |
| S15 | Use YAML 0.3 header or custom-0.2 selector with new fields/option | Reject mismatch/unsupported option; preserve old source contracts |
| S16 | Validate the single SYS-002 file with the matching schema option and name | Valid: no automatic requirement siblings needed when it has no references |
| S17 | Add decomposes ABSENT while schema/value checks succeed | Existing missing-target validation still fails |
| S18 | Add YAML anchor, alias, tag or unquoted scalar value | Existing YAML profile rejection, no attribute exception |
| S19 | Change schema file after successful validation, before formatter replacement | Stop writes on detection; preserve external schema edits and report any already completed file replacements |

For positive P03 with all declarations optional, omitting attributes succeeds;
S01 applies to the original required-discipline schema. Every failure above keeps
compiler requirements empty. Trace/formatter reject invalid input; SARIF preserves
primary source or schema locations and incomplete-analysis status. Invalid schema
is not the same as a valid schema with a missing required value.

## Comparison, import and derived-view cases

| Case | Baseline/current pair | Expected outcome |
| --- | --- | --- |
| C01 | Only owner-team changes | Same ID; row review-stale; old/new attribute values visible |
| C02 | Only schema description changes | Same explicit values; all selected covered rows review-stale; old/new definition visible |
| C03 | Add optional schema entry, omit everywhere | No synthesized values; definition changed and rows review-stale |
| C04 | Required changes false to true while absent | Invalid current input, no successful analysis/report |
| C05 | Source comment, attribute key order, JSON whitespace or enum order changes | Equal values/definition; current; hashes/provenance can change |
| C06 | Schema path/source file moves | Equal semantics, updated locations; no new requirement identity |
| C07 | Same schema name, different definitions in two scopes | Keep independent definitions, no merge; compared rows review-stale |
| C08 | New artifact claims complete but violates enum, requiredness or schema format | Serialized consumer rejects before linking |
| C09 | Recompile after a schema edit but retain the old exact artifact pin | Pin mismatch; recompiling is necessary but does not approve a new binding |
| C10 | Old output 0.1 versus schema-free output 0.2 with equal seven fields | Explicit promotion to empty attributes/null schema; current |
| C11 | Old output 0.1 versus schema-selected output 0.2 | Different definitions; review-stale even if all optional values absent |
| C12 | Report attribute contains HTML-looking text | Escaped literal display, no execution; readable source link/provenance |
| C13 | New source values sent to old ReqIF/migration path | Explicit unsupported input; no flattened export or silent attribute loss |
| C14 | Artifact is incomplete, unknown format or output stream fails | No usable linked result/report; caller must check exit and completeness |
| C15 | Assessed criticality changes in an independently owned assessment | Requirement can stay unchanged; do not manufacture an inline requirement edit |

## Review and conversion to executable evidence

The case table is a review checklist and input to successor tests, not a new test
runner or a schema implementation. Golden fixtures should separate exact diagnostic
codes from explanatory wording, exercise code-point locations including Unicode,
and assert strict publication/output failures. Formatting fixtures must preserve
inline, inter-field and leading comments and opaque math, retain attribute order,
prove idempotence and compare semantics before/after formatting. Seeded generation
and targeted mutations should exercise independently specified behaviors, including
unknown-value acceptance, requiredness bypass and ignored schema changes.

No production code, tests, schemas, version constants or command behavior changed
in this decision batch. JSON/YAML example syntax and documentation links are checked
separately; acceptance under the proposed attribute rules remains future work.
