# Research 0055: Work-item source and interface decision

Date: 2026-09-05. TC-1602.

Historical source-carrier decision, replaced by [Research 0060](0060-work-item-yaml-decision.md)
under TC-1607. The original implementation evidence remains valid for source 0.1.

Select a Markdown heading plus one strict JSON metadata block, followed by opaque
Markdown prose. The heading alone owns ID/kind/title; metadata alone owns status
and typed relationships. JSON is selected for nested links and explicit arrays
without adding a YAML dependency to the work-item tool. Requirements remain YAML;
verification plans remain TSV. Neither choice dictates another artifact's format.

The old English metadata is pleasant to read but mixes executable prerequisites
with conditions and redundant unlocks wording. Parsing every ID mentioned in prose
would infer relationships. Separate JSON plus a duplicated Markdown card creates
competing status/title authority. All-JSON or all-YAML source would require escaping
or restructuring existing narrative. Structured Markdown preserves narrative bytes
while bounding exactly what the compiler understands.

[Specification 0018](../specification/0018-work-items-0.1.md) defines the complete
selected source, selection, output, relation, import, analysis and view contracts.
[Task](../examples/work-items/task.work.md) and
[issue](../examples/work-items/issue.work.md) examples are ordinary Markdown with
JSON, not a platform-wide metamodel. Invalid examples: duplicate metadata status;
unknown status; issue with dependencies; two cards with equal IDs; evidence using a
remote URI; unqualified typed reference; incomplete imported artifact; dependency
cycle; self-supersession. These fail, rather than silently default or infer intent.

Migration mapping: old heading preserved; Status split into its enumerated value
and statusNote only where qualified; explicit IDs in Depends on become dependencies
with the full original prerequisite wording retained as opaque condition; stage,
type and unlocks retain their original text. Prose beginning at the first level-two
heading is preserved exactly. Metadata removal/replacement is recorded per card.
Ordinary Markdown links stay ordinary; selected evidence/resource links may be added
explicitly with root-relative paths. No bulk inference of typed links from prose.

The selection manifest owns file selection only, not status. Generated tables must
replace redundant manually edited index rows during dogfooding. Historical planning
narrative stays authored. This allows this repository's own cards to be checked by
the maintained component without a database, remote service or invented identities.

Source syntax checks: the two examples' JSON metadata parses with the standard
JSON parser; expected malformed and graph cases remain implementation obligations.
This decision precedes implementation and does not claim compatibility with an
external tracker or semantic validity from a generic Markdown renderer.
