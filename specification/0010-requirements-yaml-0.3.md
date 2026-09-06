# Requirements YAML Source 0.3

Status: Normative experimental source contract

Identifier: `mundanereq-yaml-0.3`

## 1. Scope and authority

This contract defines human-authored requirements in YAML. Other engineering
artifacts have independent representation decisions. The abstract requirement
model includes human-authored IDs, optional opaque
allocation/source values, ordered statement prose/math, prose-only rationale and
unordered decomposition edges. Generated artifacts remain derived.

The [structural schema](schema/requirements-yaml-0.3.json) is normative for keys,
types, cardinality and string patterns. The following sections are normative for
source presentation, domain meaning, selection and processing. YAML 1.2.2 defines
syntax and scalar decoding; JSON Schema Draft 2020-12 defines structural schema
interpretation. These sources have distinct responsibilities. A contradiction is
a specification defect requiring correction, not permission for implementation
fallback. Conformance tests exercise both structural and semantic rules.

Normative external references: [YAML 1.2.2](https://yaml.org/spec/1.2.2/) and
[JSON Schema Draft 2020-12 validation](https://json-schema.org/draft/2020-12/json-schema-validation).
MUST, MUST NOT and MAY express requirements, prohibitions and permission.

## 2. Physical source and YAML profile

Source MUST be UTF-8 without BOM, terminated by LF or CRLF. Bare CR, tabs, NUL,
C0 controls except LF/CR, and U+007F–U+009F are prohibited. The same control rules
apply after scalar escape decoding, except that math payloads may contain LF.
Unpaired surrogates are prohibited. Code points are compared exactly, without
normalization. Comments have no requirement semantics.

A file contains exactly one YAML document. Optional `---` and `...` markers are
permitted; directives, explicit tags, anchors, aliases and complex mapping keys
are prohibited. Mapping keys MUST be strings and unique before object construction.
Core scalar resolution applies. Block and flow collections are permitted.

Every scalar VALUE MUST be quoted or use a literal/folded block, including IDs,
format identifiers and `language: "latex"`. Keys may be plain. This prevents an
unquoted hash from silently truncating intended requirement text. Inline YAML
comments remain permitted after complete quoted values. No preprocessor changes
YAML syntax. An ordinary YAML parser can read conforming files as written.

Limits: 8 MiB encoded bytes per file, 16 simultaneously open collection levels,
10,000 records per file, and at most 100 primary diagnostics per file. A limit
failure is explicit invalid input, never a complete accepted model. Implementations
may stop that file after syntax/profile failure and MUST continue independent files.

## 3. Container and fields

The root mapping contains required `format: "mundanereq-yaml-0.3"` and a nonempty
`requirements` sequence. Each record has required `id`, `title`, and `statement`.
Optional fields are `allocation`, `rationale`, `source`, and `decomposes`. Unknown
keys at any level are invalid. Optional fields are omitted rather than null/empty;
a missing decomposition list means no outgoing edges. Defaults do not synthesize
values. Attribute extensions await their own decision.

| Element | Meaning and representation |
| --- | --- |
| `id` | String matching `[A-Za-z0-9][A-Za-z0-9._-]*`; sole authored identity |
| `title` | Nonempty single-line scalar; no boundary Unicode White_Space |
| `allocation` | Optional scalar with title's character/boundary rules; opaque label |
| `source` | Optional scalar with title's rules; opaque provenance text, not automatically a URI |
| `statement` | One paragraph string or nonempty ordered list of `{prose: string}` and `{math: {language: "latex", payload: string}}` |
| `rationale` | Optional one-paragraph string or nonempty ordered list of paragraph strings |
| `decomposes` | Optional nonempty list of distinct authored target IDs |

A prose paragraph is a nonempty decoded string without CR/LF. Its exact spaces are
semantic. `>-` folds ordinary wrapping and strips its terminal newline; blank lines
and extra indentation that create decoded newlines are invalid in a paragraph.
Use distinct list entries for distinct paragraphs. Only YAML scalar folding applies. Rationale strings containing math words stay prose.

Math is opaque LaTeX. Payloads are nonempty and contain at least one character
other than LF. All decoded payload characters, including leading/trailing spaces
and newlines, are semantic. Literal `|-` is convenient for most payloads; quoted
escapes can represent every allowed payload exactly. No mathematical evaluation
or validity is implied. List order is semantic; record and mapping order are not.

## 4. Source sets and domain validity

Each ID occurs once across the selected source set. `A` with `decomposes: ["B"]`
authors an edge from A to the higher-level requirement B; it does not assert that
B is satisfied. Every target MUST exist in that source set. Inverse relationships
are derived. Cycles and self-edges remain permitted; project coverage/acyclicity
policies remain separate from language validity. Allocation remains opaque.

A record's identity is unaffected by filename, position, mapping order, comments
or representation style. Semantic equality compares every model field, ordered
content values and relationship sets. Revisions and baselines remain controlled
by the surrounding project/version-control workflow; no generated identity is added.

## 5. Explicit selection and compatibility

Command invocation defaults to YAML 0.3. A leading `--source=yaml-0.3` selects
this contract explicitly; `--source=yaml-0.4` selects the attribute-aware profile.
Unknown selectors fail with exit 2. The selector precedes modes/operations, and
`--` ends filename-option processing. Version output names the selected contract.

YAML directory traversal selects only `.mreq.yaml`; arbitrary project YAML is
excluded. Explicit regular files may have any filename but MUST satisfy the
selected contract. Existing path normalization, sorting, duplicate-path removal,
.git exclusion and no-symlink traversal rules remain. Empty selection is operational
failure. The required document format identifier must match; missing/unknown
identifiers fail validation. Source format is never inferred by parser fallback.

One invocation interprets one source profile. The required document format must
match that profile. IDs must be unique across every selected file.

## 6. Diagnostics, formatting and commands

Diagnostics contain file, one-based line and Unicode code-point column, rule code,
and explanation. Syntax errors use parser marks; schema/domain errors use the
value or enclosing record start when a value is absent. Escaped/folded values use
the scalar's source start, not a fictional position inside decoded text. Exact
end ranges are outside this CLI contract. Duplicate/dangling diagnostics locate
the authored ID/reference. Invalid records do not enter valid results. Any file
failure makes the whole result invalid; cross-file dangling errors are suppressed
when missing parse data could cause a misleading cascade. Tools MUST reject
invalid/partial input for trace and formatting.

Validator exits: 0 valid, 1 invalid source, 2 input/invocation/output failure.
Formatter and trace retain their existing exit classes. All required stdout/stderr
output is checked; delivery failure overrides source/check status with exit 2.
A platform may terminate a process by SIGPIPE; that is non-success, never exit 0.

YAML formatting only converts physical CRLF to LF after complete validation. It
preserves all other bytes, comments, quoting, indentation, record order and scalar
content. Check mode reports differences; write mode uses snapshot checks and the
partial-write behavior in the [safety addendum](0011-tool-safety-and-yaml-commands.md).

## 7. Examples and conformance

```yaml
format: "mundanereq-yaml-0.3"
requirements:
  - id: "NEED-1"
    title: "Retain data"
    statement: "The logger shall retain measurements."
  - id: "SYS-1"
    title: "Storage duration"
    statement: >-
      The logger shall retain measurements
      for thirty days.
    decomposes: ["NEED-1"]
```

`id: 001`, `title: status # ready`, `statement: null`, duplicate keys and
`format: "unknown"` are invalid. Examples, mutation checks, exact semantic
equivalence and JVM/native conformance live in the maintained YAML tests and
`conformance/0.3`.
