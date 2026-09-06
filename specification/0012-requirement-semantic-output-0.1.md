# Requirement semantic output 0.1

Status: Selected experimental contract (TC-1201)

Format: `mundanereq-requirements-0.1`

This is compiled semantic output, never an alternative authoring format. Human
requirement IDs remain identity within an explicitly selected source set. This contract imposes no source notation on other engineering artifacts.

## 1. Command and failure boundary

```text
mundanereq-compile [--source=yaml-0.3] --root DIRECTORY [--] INPUT...
mundanereq-compile [--source=...] --help
mundanereq-compile [--source=...] --version
```

Source selection defaults to YAML 0.3 and must be the first option when supplied.
The existing selection rules apply, including suffix filtering for directories,
no symlink traversal, deterministic deduplication and no mixed-profile fallback.
INPUT and root are resolved against the invocation working directory. Root must
be an existing directory; all selected input paths must be lexically inside it.
The root is a provenance base, not implicit input discovery. Require at least one
INPUT; reject missing/duplicate --root, unknown options, unknown source selectors,
outside-root input and malformed paths as invocation errors.

Stdout contains exactly one UTF-8 JSON object followed by LF. Help/version are
text exceptions. No progress text is mixed into artifact output. Invalid source
returns 1 and a complete=false artifact with diagnostics; operational input errors
return 2 and such an artifact. Invocation errors return 2, text on stderr and no
artifact. Output/serialization failures return 2, with a best-effort stderr
message; a prefix may already have been delivered. Consumers require both exit 0
and a complete, parseable supported artifact before analysis. Closed stderr also
prevents successful completion. Success is interpretation, not domain approval.

## 2. Envelope and values

All listed fields are required, including explicit nulls. JSON duplicate object
keys are forbidden. Unknown additional informational fields may be ignored for
this exact format; they cannot introduce new normative meanings. Reject unknown
format/kind, unsupported source contracts, unknown semantic block kinds, or
incomplete input before analysis. Do not guess support from a numeric prefix.

| Field | Meaning |
| --- | --- |
| artifactKind | Literal `requirements` |
| format | Exact compiled-format identifier above |
| sourceContract | Selected maintained source contract (`mundanereq-yaml-0.3`) |
| compiler | Object: name `mundanereq-compile`, version `experimental-0.1`, contract `compile-cli-0.1`; independently declared domains |
| complete | true only when every selected input was read/interpreted and source-set validation passed |
| sources | Sorted list of `{path, sha256}` input snapshots |
| requirements | ID-sorted list of `{values, locations}`; empty whenever complete=false |
| diagnostics | Ordered list of diagnostics; empty whenever complete=true |

Each values object contains every current Requirement field:

| Field | Representation |
| --- | --- |
| id | Human-authored string, unique within this complete artifact |
| title | Nonempty string |
| allocation | Authored opaque string or null |
| statement | Ordered nonempty array of prose/math blocks |
| rationale | Ordered prose blocks or null |
| source | Authored citation string or null, distinct from physical provenance |
| decomposes | Sorted distinct array of target ID strings in this source set |

A prose block is `{kind:"prose", text:STRING}`. A math block is
`{kind:"math", language:"latex", payload:STRING}`. Math payload is opaque and
preserves exact semantic newlines; no formula interpretation occurs. Paragraph
and block order is significant. Cycles/self-links remain valid relationships.
Optional absence is null; an empty relationship set is `[]`. No untyped extension
bag or guessed attribute syntax is published. [Output 0.2](0021-requirement-semantic-output-0.2.md) carries typed attributes.

## 3. Locations and source snapshots

A span is `{path:STRING, start:{line:INTEGER,column:INTEGER}, end:POSITION_OR_NULL}`.
Lines and columns are one-based Unicode code-point coordinates, not UTF-8 bytes
or UTF-16 code units. Ends are exclusive. CRLF is one physical line break. Paths
are root-relative, normalized with `/`, with no leading slash or `..` segment.
Root itself is `.`. No checkout-specific absolute root, timestamp, Git discovery,
filesystem identity key or host name is emitted. Paths locate source; they do not
become IDs. The consumer supplies the source root when navigating.

locations contains:

- `record`: one span covering the requirement's source syntax;
- `fields`: map from each present source field (including id) to an array of spans;
- `references`: map from each decomposes target ID to its authored value span.

For YAML 0.3, record spans cover the parser mapping node (excluding the sequence
indicator). Field spans cover value nodes, including quotes or block indicators
and lexical content. A YAML sequence-valued field has one encompassing field span;
references have individual scalar-node spans. Mapping/collection end positions
may be the next line's column 1 according to YAML node boundaries. These are syntax
ranges, not a mapping of individual decoded string characters to source bytes.
Absent fields have no span entry. Requirement, field and reference spans have
known ends. Current diagnostics are point locations with end=null; no fabricated
underline length or inferred end is supplied.

Each source sha256 is lowercase SHA-256 of the exact selected source bytes (including
comments and line endings). It represents that input revision; content edits are
expected to change it. It is not stable requirement identity, not inserted into
authored files, and not a per-requirement semantic fingerprint. No canonicalization
precedes this byte hash. If a bounded oversized YAML read stops before the full
source is known, its sha256 is null and complete=false. Other unread inputs may
be absent from sources and identified by input diagnostics. Source snapshots describe
the read bytes, with no assertion the filesystem stayed unchanged afterward.

## 4. Diagnostics and completeness

Each diagnostic is `{ruleId, severity, phase, message, location}`. Severity is
`error` for current rules. phase is `input` for input-unavailable/no-source-files,
otherwise `source`. ruleId is the interpreter's stable code, defined in the
[rule catalog](0013-compiled-diagnostic-rules.md). Messages are explanatory and
may change (OS input messages may be platform dependent); automation uses ruleId,
phase and locations. Order is path, line, column, ruleId, message using Java string
lexicographic ordering for strings. All strings are emitted as valid Unicode JSON.

No partial semantic records are published in this version, including records from
otherwise valid files when another file fails. Editor consumers can display the
available diagnostics but cannot infer a complete inventory from them. The [recovery contract](0016-diagnostic-recovery.md) defines incomplete interpretation. Duplicate IDs and unresolved references are invalid input with
source-linked diagnostics, not silently chosen graph nodes. A compiler may not
emit complete=true with diagnostics or omit failed files to manufacture success.

## 5. Determinism and compatibility

Object keys are sorted lexicographically; compact JSON uses no insignificant
whitespace and ends in LF. Non-ASCII characters remain literal UTF-8. Quote and
backslash are escaped; control characters use lowercase `\u00xx`. Requirement
IDs, reference sets, map keys and paths use Java String.compareTo ordering (UTF-16
lexicographic order), independent of locale. Field occurrence arrays and semantic
block arrays retain source order. Input argument order does not change output.
Identical selected bytes, normalized paths, tool/contract declarations and runtime
error messages produce identical artifact bytes. No byte-identical native binary
claim is implied.

For semantic comparison use only each requirement's values object. This excludes
locations, paths, source bytes and compiler metadata; comment edits and file moves
therefore preserve equal values. Source citation/rationale edits do change values.
Verification comparison follows its own versioned contract.

| Evolution | Required action |
| --- | --- |
| Add explicitly informational provenance data | Retain format only if old readers can ignore it without changing interpretation |
| Change field type/meaning, comparison semantics, required values or supported block kinds | New format identifier, consumer rejection of unsupported formats, migration note with before/after examples |
| Add custom attributes | Use output 0.2; do not emit attributes as informational data in output 0.1 |
| New source profile mapping to identical values | Explicitly declare support, update source-contract acceptance and fixtures; no automatic inference |

[Worked examples](examples/requirements-artifact-0.1/README.md) are contract fixtures.
This version has present value for the verification consumer implemented by the independent plan/analyzer commands. No long-term compatibility promise is made.
