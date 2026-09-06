# YAML work items 0.2

Status: Maintained normative experimental contract, implemented and verified
through TC-1608/1609.
This addendum replaces source authoring for new work with YAML. The work-item
meanings, import graph, limits, output safety and derived view rules in
[0018](0018-work-items-0.1.md) continue except as explicitly amended here.

## Source and data model

Use YAML 1.2 Core schema, exactly one document containing one item mapping.
Require UTF-8 without BOM, final LF (LF or CRLF source lines), no bare CR or NUL,
no malformed Unicode; maximum 1 MiB per selected source. YAML directives, anchors,
aliases, explicit tags, non-string mapping keys, duplicate keys (at any depth),
merge keys and collection nesting beyond 16 are invalid. No includes or evaluation.
Comments are allowed but are presentation, not compiled narrative or semantic facts.
Migration never edits YAML comments because its input is the prior Markdown profile;
no YAML formatter/re-emitter is introduced by this contract.

Required fields: format, id, kind, title, status, body. Optional dependencies and
relations default to empty lists. Optional planning defaults to an object containing
stage, type, condition, unlocks, statusNote, each an empty string; supplied planning
keys replace only their matching defaults. Unknown keys fail everywhere.

```yaml
format: mundane-work-yaml-0.2
id: TC-EXAMPLE
kind: task
title: Review the alarm
status: Ready
dependencies: []
relations:
  - relation: addresses
    scope: requirements
    kind: requirement
    target: SYS-001
body: |-
  ## Question

  Does the alarm explain the intended behavior?

      Sensor -> Alarm
  # and : remain ordinary content inside this string.
```

ID, title, kind, status, dependencies, relations and planning retain 0018 meanings.
Body must be a nonblank string; headings/lists/code fences inside it are opaque.
All field values are strings except arrays, mappings and the null scope of evidence
relations. YAML Core booleans/numbers/null in string positions fail, without coercion.
Quote values such as `"123"`, `"true"` or `"null"` when intended as strings.
Dates resolve as strings under this profile. Empty planning strings are permitted.

Literal `|` preserves internal newlines and relative indentation, retaining one
final newline; `|-` strips final newlines; `|+` retains trailing blank lines.
Common indentation is removed and physical CRLF is normalized to LF by YAML.
Folded `>` follows YAML folding rules; quoted scalars follow YAML escaping/folding.
The compiler preserves the resulting string exactly; it does not trim or reflow it.
A quoted `"line\r\n"` therefore preserves that decoded CRLF. Literal style is the
authoring recommendation for preformatted prose, not a new Markdown parser.

The [Draft 2020-12 schema](schema/work-items-yaml-0.2.json) specifies decoded data
structure. Compiler checks enforce presentation, Unicode, ID/title/path restrictions
and relation tuple uniqueness; analysis enforces resolution and graph rules. All
three layers are required for a valid linked backlog.

| Invalid example | Required rejection layer |
| --- | --- |
| Duplicate status or nested relation key | YAML node validation |
| Second document, alias, explicit tag or depth 17 | YAML profile |
| Unknown key, numeric ID, list-valued body, status Done | Structural schema/compiler |
| Issue with dependencies | Structural schema/compiler |
| Blank body, padded title, malformed resource path | Semantic compiler |
| Missing target, dependency cycle, bad imported pin | Analysis |

## Selection and compatibility

New selection is explicit JSON:

```json
{"format":"mundane-work-set-0.2","source":"mundane-work-yaml-0.2","files":["roadmap/task-example.yaml"]}
```

All selected files use that source profile. No extension inference, mixed profiles
within one manifest, fallback or directory discovery. Commands are unchanged.
Selection 0.1 (format/files only) continues to mean Markdown source 0.1. Unsupported
source/selection combinations fail with invalid-work-set; there is no auto-migration.

YAML produces `mundane-work-items-0.2`, sourceContract `mundane-work-yaml-0.2`.
Legacy selection still produces items 0.1 / source 0.1. Both are supported by current
serialized consumers; mismatched output/source pairs fail. All output value fields,
normalized array ordering and graph meanings remain unchanged. YAML comments,
field order and scalar style do not affect values when decoded strings are equal;
source digests and locations still describe each exact revision. Digests are not IDs.

Item location points to the YAML id value; metadataLocation points to the item
mapping start. Both are one-based Unicode code-point coordinates. Relation/domain
errors may point to the item declaration; syntax and node errors use actual marks.
These are declaration points, not fabricated individual relation spans. Old output
0.1 retains heading 1:1 and metadata 4:1 requirements. Every item still owns one
source path. Serialized validation checks positive coordinates and same-path
membership, without claiming to reconstruct source text.

Source errors use invalid-work-source and empty items, accumulating one failure per
independent source. YAML parser diagnostics carry their problem location. Bounded
input/output, read rechecks, stream errors and exit 0/1/2 remain as in 0018.
Analysis stays 0.1 and validates either embedded work output version; derived views
use the supplied source points. Consumers remain independent of source parsers.

## Repository authoring

Each card has one authoritative YAML source. Maintain its human ID, typed
relationships and literal body; list current paths in the explicit selection.
Generate `WORK-ITEMS.md` with the public work-item pipeline and check it with
`make work-backlog-verify`. Completed cards move into `roadmap/closed/` with
recorded evidence and repaired incoming links.

Markdown work-item sources remain an independent compatibility profile. The
work-item format makes no choice for safety, BOM or other future artifacts.
