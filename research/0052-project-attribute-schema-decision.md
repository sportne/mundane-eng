# Research 0052: Project attribute schema decision

Date: 2026-09-05. Task: [TC-1302](../roadmap/closed/task-1302-decide-project-attribute-schemas.yaml).

Status: Selected design for future implementation. Current commands, normative
schemas, source contracts and version declarations are unchanged. All new syntax,
format names and command options below are design targets, not available features.

## 1. Selected boundary and authority

Use a narrowly scoped **JSON project attribute declaration**, explicitly passed to
requirements commands. Attach its values to requirement records using the selected
requirements YAML representation. This independently chosen JSON configuration is
not a new artifact language or a platform-wide authoring mandate. Requirements,
verification plans and future assessments retain separate authoring decisions.

[Research 0051](0051-project-attribute-use-case-decision.md) selects descriptive
text/enum values, optional or required once, with no defaults. Assessed criticality,
variant allocation and verification claims remain independently owned. A schema
validates a label's form, not its truth, authority or safety significance. Human IDs
remain requirement identity; schema selection adds no requirement identity system.

| Authority | Responsibility |
| --- | --- |
| Requirements language reference and structural schema | Built-in model, YAML profile, attachment structure, names, types, selection and diagnostics |
| Checked-in project declaration | Which attributes exist in this project, their descriptions, types, requiredness and allowed values |
| Authored requirement source | Explicit values and the selected schema name; no generated defaults |
| Compiled output | Derived values, schema snapshot and provenance for downstream tools; never an editable source of definitions |
| Companion artifact | Its own assertions, contexts, revisions, evidence and assessment authority |

A future normative structural schema for the declaration validates its JSON shape;
prose specifies exact strings, cross-field rules, source selection and meanings.
It does not replace the existing requirements schema. Any combined schema generated
for an editor is a disposable aid, clearly marked as derived; semantic validation
remains with the requirements tool. Conflicting normative prose/schema is a defect
requiring correction, not an implementation fallback.

## 2. Why this declaration format

| Alternative | Decision |
| --- | --- |
| Embed definitions in each requirement document | Reject: repeated project definitions invite drift and ambiguous source-set merging |
| Separate YAML declaration using the requirements value profile | Viable but not selected: boolean requiredness would need a separate YAML scalar policy; reusing requirement syntax would falsely imply it is the same source kind |
| Separate narrow JSON declaration | Select: ordinary strings, booleans and objects; matches the existing explicit local JSON configuration workflow; no extra authored mini-language |
| Arbitrary JSON Schema as the project's attribute language | Reject: references, combinators, defaults, coercion expectations and conditional rules exceed the selected two-type model |
| Attribute declarations as imported compiled engineering artifacts | Defer: no current shared schema producer/consumer needs independent artifact identity, build graph or registry |

JSON is chosen for these declarations only. It does not constrain future safety,
procedure, product or evidence source representations. Do not silently accept both
JSON and YAML declarations. JSON has no comments here; required descriptions carry
reviewable meaning, and surrounding Markdown can explain project policy.

## 3. Declaration shape and lexical rules

Worked file: `project/requirement-attributes.json` (filename is a convention, not
discovery). It is checked in beside the project sources and changed through normal
review. The entire following object is a declaration; it is not JSON Schema.

```json
{
  "format": "mundanereq-attribute-schema-0.1",
  "name": "logger-metadata",
  "attributes": {
    "discipline": {
      "type": "enum",
      "required": true,
      "description": "Descriptive engineering discipline; does not assign approval authority.",
      "values": ["software", "electronics", "mechanical"]
    },
    "owner-team": {
      "type": "text",
      "required": false,
      "description": "Contact label supplied by the requirement author; not a product allocation."
    }
  }
}
```

Required root keys are exactly `format`, `name`, `attributes`. Format must match
exactly; do not accept future identifiers by numeric prefix. `name` is a human
project label matching `[a-z][a-z0-9]*(?:-[a-z0-9]+)*`, at most 64 ASCII characters.
It binds source to the intended declaration, not to a globally unique registry.
The same name may evolve in Git; it is not a digest or a compatibility promise.

Attribute names use that same pattern/length. Reserve `id`, `title`, `allocation`,
`statement`, `rationale`, `source`, `decomposes`, `format`, `requirements`,
`attributes`, `attribute-schema` and names starting `mreq-` or `mundane-`.
Reject conflicts rather than overriding built-ins. Future additions to reserved
names require a recorded compatibility decision; existing projects are not silently
reinterpreted. Do not reserve domain labels as a substitute for ownership review.

Each declaration requires `type`, `required`, `description`. `type` is exactly
`text` or `enum`; `required` is a JSON boolean, not a quoted string. `description`
is nonempty single-line decoded Unicode text with the existing title character and
boundary-whitespace rules. An enum additionally requires `values`, a nonempty array
of unique text strings with those same rules. Text forbids `values`. Every unknown
key, including `default`, `extends`, `imports`, `$ref` or `pattern`, is an error.
Duplicate object keys at any level are errors before map construction, even if the
values agree. Enum membership is exact and case-sensitive, without normalization;
array ordering does not assign rank or change meaning.

Select conservative implementation bounds: UTF-8 without BOM, LF/CRLF terminated,
no malformed Unicode, JSON comments or trailing commas; at most 1 MiB encoded bytes,
16 collection levels, 128 declarations and 256 allowed values per enum. The
attributes map must be nonempty. Decoded text follows YAML title rules; multiline,
empty, padded or control-containing values fail. Limit violations are explicit
schema errors, never truncated accepted definitions. These bounds are design limits,
not measured capacity claims; implementation must exercise their boundaries.

## 4. Source attachment and explicit selection

Introduce an explicit future `yaml-0.4` source profile with document format
`mundanereq-yaml-0.4`. Keep the rest of YAML 0.3's model, physical/scalar rules,
resource bounds and `.mreq.yaml` selection. New root field `attributeSchema` names
the selected declaration; new record field `attributes` maps declared names to
quoted/folded string values. Schema keys are case-sensitive, including that camel
case root key. Multiple requirements still share a single document envelope:

```yaml
format: "mundanereq-yaml-0.4"
attributeSchema: "logger-metadata"
requirements:
  - id: "SYS-001"
    title: "Record measurements"
    statement: "The logger shall record temperature measurements."
    attributes:
      discipline: "software"
      owner-team: "Logger firmware"
  - id: "SYS-002"
    title: "Supply power"
    statement: "The power supply shall power the logger."
    attributes:
      discipline: "electronics"
```

Values occur at most once; neither null nor an empty map is a presence marker.
Omit `attributes` when there are no values. All required declarations apply to
every selected requirement. Optional missing values remain absent. Keys/record
order are not semantic. Block scalar text is accepted only if decoded into a valid
single-line value. Existing `source` citations and `allocation` labels keep their
meanings; attributes neither replace them nor become implicit references.

Future invocation example (options do not exist yet):

```text
mundanereq-validate --source=yaml-0.4 --attribute-schema project/requirement-attributes.json requirements/
mundanereq-compile --source=yaml-0.4 --root . --attribute-schema project/requirement-attributes.json requirements/
```

- Accept one `--attribute-schema PATH` before `--`; reject repeated options, even
  if they name the same file. Paths resolve against invocation working directory.
  No directory traversal, environment fallback, file-local path, search path,
  network lookup, inheritance or automatic merging selects a schema.
- With that option, every selected document must have a matching `attributeSchema`
  name, even when all declarations are optional and it has no attribute values.
  Two names in one source set fail. A named schema without the option fails rather
  than trusting a nearby file. Name agreement guards mistakes; a name alone does
  not pin a particular revision.
- With neither the option nor a document schema name, YAML 0.4 accepts exactly the
  old built-in model; `attributes` is prohibited. Old profiles do not accept this
  option or these fields. There is no implicit upgrade or format guessing.
- Compiler/SARIF existing explicit roots must contain the declaration as well as
  requirement inputs. Reject outside-root paths and resolved symlink escapes.
  In commands without a root, resolve an explicit regular non-symlink declaration
  from the working directory; no implicit root is introduced. Explicit schema files
  are not requirement input discovery candidates or formatter write targets.
- Read the declaration once into a bounded snapshot before interpreting values;
  failures never cause a retry using a different schema. Record those exact bytes
  in compiler provenance. Before formatter write-back, recheck schema bytes and
  available file identity as well as requirement snapshots; detected schema changes
  stop remaining writes and report completed/remaining files under existing safety
  rules. The existing final-check/rename race is not claimed to disappear.

An isolated file with attributes needs the explicit declaration option and all its
normal decomposition targets selected, just as before. Without context, report a
missing-schema diagnostic; do not claim that attributes are valid. A schema-free
file works alone subject to the existing reference rules. Other schema projects can
be compiled separately and imported under explicit scopes.

## 5. Diagnostics and failure behavior

Use the following stable rule targets in the future diagnostic catalog. Required
stdout failures still override success with operational failure; do not invent
end spans where the parser retains only a source point.

| Condition | Rule target / location | Validator and compiler exit |
| --- | --- | --- |
| Duplicate option, unsupported source/option combination, outside-root schema | Invocation diagnostic on stderr; no machine document | 2 |
| Missing/unreadable explicit file | `attribute-schema-unavailable`, declaration path without fabricated region | 2 |
| Invalid JSON/shape/name/type/description/values/unknown key/bounds | `attribute-schema-invalid`, offending token or enclosing declaration for a missing key | 1 |
| Duplicate JSON declaration key | `attribute-schema-duplicate`, repeated key | 1 |
| Source names schema but option absent | `attribute-schema-required`, document schema name | 1 |
| Option supplied but document name missing/different | `attribute-schema-mismatch`, name or document start | 1 |
| Undeclared attribute / invalid name in source | `attribute-unknown`, authored attribute key | 1 |
| Wrong type, empty/padded/invalid string, invalid enum member | `attribute-value`, authored value node | 1 |
| Missing required value | `attribute-required`, requirement mapping start | 1 |
| Duplicate YAML attribute key | Existing `yaml-duplicate-key`, repeated key | 1 |
| Schema changes during formatter operation | `attribute-schema-changed`, schema path and completed/remaining write information | Formatter 2 |

Do not cascade unknown/missing-required errors when the schema is unavailable or
invalid; report the primary configuration failure. A valid schema permits bounded
record recovery under the current recovery rules. Schema failure makes semantic
analysis incomplete even if YAML syntax is otherwise recognized. No compiler
records, formatter writes, trace results or successful linked analysis may be
published from incompletely validated input. Formatter/trace preserve their current
invalid-input exit classes; SARIF reports the same rules and invalid/incomplete
state, with declaration locations relative to its explicit root.

## 6. Compiled representation and schema revision behavior

Select a new future requirement output identifier `mundanereq-requirements-0.2` for
YAML 0.4. Do not append attributes to the old format as ignorable information.
The seven existing value fields retain their meanings. Each record additionally
has `values.attributes`, a sorted map of present name-to-string values (empty `{}`
when absent). Types are supplied by the enclosing schema definition, not inferred
from strings. `locations.attributes` maps each present name to `{name: SPAN,
value: SPAN}`. Existing built-in locations remain intact.

The new envelope adds `attributeSchema`: null without a schema, otherwise an object
with `definition`, `source`, `locations`. `definition` is the validated authored
schema object with maps deterministically ordered and enum arrays sorted as sets.
Use the existing compiled-output UTF-16 lexicographic string ordering, independent
of locale, for both; preserve strings without normalization.
`source` is `{path, sha256}` for exact declaration bytes, using the existing root-
relative path and SHA-256 rules. `locations` maps each declared attribute name to
its declaration span. Required but absent values have no invented value location.
The requirement `sources` list remains requirement snapshots; declaration provenance
has its dedicated field. Informational locations are outside semantic comparisons.

An invalid/incomplete artifact has no requirement records. If its schema is not
valid, `attributeSchema` is null and primary diagnostics identify the declaration;
null does not authorize treating an incomplete artifact as schema-free. No digest
is added to authored requirements. Existing source/artifact hashes identify exact
revisions and change when their input bytes change.

Consumers validate the supported schema format/definition, all attribute values and
requiredness, locations and completeness at the serialized boundary. They must not
trust `complete: true` alone. They do not reload a project schema from disk: the
compiled snapshot carries the definition used for interpretation. Consumers import
whole requirement artifacts with explicit scopes under the existing import model;
there is no separately selected or implicitly overriding imported schema.

The same schema name in two scopes does not merge definitions. Preserve each
artifact's schema and compare their definitions. A wrong exact artifact pin still
fails before analysis. A copied compiled artifact is reproducible from retained
source plus declaration revisions; a pin is an integrity/revision binding, not
proof of assessment authority.

### Comparison policy selected for the current verification consumer

Evolve the analyzer's published comparison contract explicitly. Compare existing
seven values, present attributes, and the complete canonical schema definition.
Use conservative whole-schema comparison initially: description, requiredness or
allowed-value changes, even on an unused attribute, make selected coverage rows
review-stale. This costs extra review but prevents a changed meaning from being
silently treated as current. No custom project policy or per-field exception is
introduced. A future narrower policy requires its own evidence and contract.

| Edit, with the requirement's human ID unchanged | Validity / compiled semantics / review finding |
| --- | --- |
| Change owner-team value | New explicit attribute value; review-stale |
| Reorder YAML attribute keys / add a source comment | Equal values and schema; current, changed byte provenance |
| Reformat JSON / reorder enum members | Equal canonical definition; current, exact schema/artifact pins change |
| Change declaration description | Definition changed; review-stale even if values are identical |
| Add enum member, no value changes | New permitted vocabulary; valid but review-stale under conservative policy |
| Remove an enum member in use | Invalid source against new schema; no complete result or report |
| Add optional attribute, omit it everywhere | Definition changes; no value is synthesized; review-stale |
| Change optional to required, existing records omit it | Missing-required errors; author must add values explicitly |
| Add `default` | Invalid declaration; no implicit assignment |
| Move source or schema file, preserve decoded values/definition | Current; new source links and exact byte-artifact binding |
| Rename schema or attribute | Explicit source update required; changed semantic definition, no aliases |

An old requirements-0.1 snapshot can be compared with a new schema-free 0.2 snapshot
by explicit in-memory promotion to attributes `{}` and schema null. If the new
snapshot selects a schema, null versus that definition makes its covered rows
review-stale. No rewritten or duplicated authored source is needed. Consumers must
explicitly support both formats, not guess an upgrade. Changing verification result
meanings requires a new verification output/CLI contract and corresponding renderer
support, with migration notes; source, package and tool versions evolve separately.

## 7. Formatter and propagation contract

Preserve the current conservative YAML formatter: after full schema/source validity,
only CRLF-to-LF normalization is performed. Attribute order stays as authored;
comments, quotes, spacing, indentation, record order and scalar bytes survive.
**Deterministic attribute ordering applies to compiled maps, not source rewriting.**
This avoids assigning comments to guessed owners merely to sort fields. Schema JSON
is never reformatted by the requirement formatter. A future canonical YAML formatter
would need a separate comment-ownership decision.

| Tool / boundary | Selected support | Required observable outcome |
| --- | --- | --- |
| Parser / validator / text diagnostics / SARIF | Lossless value/schema validation | Same meanings, rule IDs and source points; invalid configuration cannot look like valid source |
| Requirement compiler / bounded linker | Lossless | Preserve typed definition, values and source provenance; reject unsupported/inconsistent artifacts |
| Verification analyzer | Lossless values; explicit conservative comparison | Attribute/schema change is visible as review-stale, not inferred satisfaction |
| Current trace operations | Validate full input; unchanged decomposition semantics | Attributes add no graph edges; invalid attributes block trace just like other invalid source |
| Existing experimental HTML report | Lossless semantic display, not source round-trip | Show present attributes, types/descriptions, schema provenance and changed definitions, escaped and deterministically ordered; link to source where supported |
| Existing custom/YAML migration command | Deliberately unsupported for new attributes | Preserve current migration behavior; no lossy reverse conversion or flattened source/allocation text |
| Experimental ReqIF adapter | Deliberately unsupported for attribute-bearing input | A future accepting path must fail visibly before emitting output until mapping/fidelity evidence justifies it |
| Editor diagnostics | Same semantic validation as CLI | Must select explicit schema and source set; do not let structural-only validation imply complete language validity |
| Editor names/enumeration completion and hover | Derived assistance | Read selected declarations, show description/type/requiredness, suggest declared names/values; no assessment or language-validity authority |

No best-effort interchange is selected in this iteration. If ReqIF work is later
justified under TC-0902, document exact text/enum mappings and report every unsupported
or lost value before accepting output. Existing self-roundtrip evidence cannot prove
external fidelity. Adding attribute display to the existing report is a bounded
extension, not a decision to maintain a general publishing component or view language.

## 8. Compatibility and migration

| Existing/new input | Selected future behavior |
| --- | --- |
| Historical 0.1-compatible fixtures processed through the current custom adapter | Unchanged; no new 0.1 selector is implied |
| Custom 0.2, default invocation or explicit custom selector | Unchanged syntax/model/output; no attribute option support |
| YAML 0.3 without project schema | Unchanged with explicit yaml-0.3; current structural schema remains valid |
| YAML 0.3 with new fields or schema option | Reject; no silent reinterpretation |
| YAML 0.4 without schema name/option or attributes | Valid built-in model; output 0.2 with null schema and empty attributes |
| YAML 0.4 with matching declaration and valid values | Valid under the selected project schema |
| YAML 0.4 standalone file naming a schema, no explicit option | Missing-schema error; not a partially valid compilation |
| Mixed 0.3/0.4 selection | Reject format mismatch; compile source sets separately |
| Unknown source, schema or compiled-format identifier | Explicit rejection; no numeric fallback |

Adoption is opt-in: retain old files/commands unchanged, or update the YAML header,
add the explicit document schema name and authored values, check in the declaration,
and add its option to project commands. A missing required annotation is an intentional
validity change only for the opted-in schema. A project cannot migrate by editing
compiled JSON. Old artifacts/readers retain their existing contract; new readers
advertise exact supported formats and comparisons. No current version constant is
changed by this design, and no long-term language freeze is promised.

## 9. Decision examples, verification obligations and risks

[Worked cases](0053-project-attribute-design-cases.md) instantiate the positive and
negative cases, standalone behavior, schema mutations and downstream comparison
outcomes. These are expected results for future checks, not executed feature tests.
The successor cards require golden source/schema/semantic/diagnostic/formatting
fixtures; property/mutation tests for chosen rules; parse-format-parse equivalence;
no-schema compatibility; independent serialized consumers; realistic multi-file
examples; and documentation aligned with actual implementation.

Remaining engineering choices are internal parser/source-location representation,
precise JSON Schema encoding of lexical constraints, and the eventual editor host.
They may not change the selected ownership, formats, presence, ordering, snapshot or
comparison semantics without recording a design revision. Stop implementation if
it cannot preserve comments or detect schema failures; do not hide loss. Reconsider
whole-schema review staleness only after concrete review noise is demonstrated.
Defer lists, schema imports, inheritance, network references, contextual rules,
automatic identities and assessment languages. New requirements YAML does not decide
any other engineering artifact's format.

## 10. Successor work

- [TC-1303](../roadmap/task-1303-validate-project-defined-requirement-attributes.yaml): normative source/declaration contracts, model, schema validation and diagnostics.
- [TC-1304](../roadmap/task-1304-preserve-attributes-in-formatting-and-trace.yaml): safe formatting and trace integration.
- [TC-1305](../roadmap/task-1305-compile-project-attribute-artifacts.yaml): versioned compiled values, definitions and source provenance.
- [TC-1306](../roadmap/task-1306-link-and-analyze-project-attributes.yaml): serialized validation, scoped imports and explicit comparison semantics.
- [TC-1307](../roadmap/task-1307-display-project-attributes-in-derived-reports.yaml): existing report display and bounded unsupported interchange handling.
- [TC-1308](../roadmap/task-1308-verify-and-document-project-attribute-workflows.yaml): integrated examples, compatibility, generation/mutation and migration documentation.

Each implementation owns its regression tests; TC-1308 extends integration rather
than postponing correctness checks. Editor diagnostics/formatting integration and
individual completion/hover cards should be scoped when an initial editor workflow
is selected; no implemented editor host currently exists. This design defines their
obligations without promising a generic LSP or bundling it into the attribute chain.

## 11. Recorded design verification

Positive fenced examples were parsed as ordinary JSON (Python standard library)
and YAML (Ruby Psych safe_load): two JSON objects/fragments and two YAML documents.
This verifies notation syntax only, not acceptance by a future attribute validator.
The first attempted YAML check used the existing schema-test Python environment,
which has no PyYAML installed; the installed Ruby parser supplied the successful
syntax check without modifying dependencies.

The repository planning check validates 72 unique/indexed task IDs, matching
statuses, existing dependencies without cycles and relative Markdown file links.
The batch diff is restricted to Markdown research, roadmap and README changes;
production, normative schemas, tests, versions, build and CI remain unchanged.
`git diff --check` passes. No dedicated Markdown lint target is provided by the
Makefile; no implementation test run is claimed for this design-only batch.
The final review confirms incremental planning: version/compatibility changes
serve the current attribute consumer and preserve explicit old-format behavior.
