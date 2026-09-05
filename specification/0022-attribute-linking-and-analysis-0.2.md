# Attribute-aware linking and verification output 0.2

Normative experimental addendum to [linking 0.1](0014-local-artifact-imports-0.1.md)
and [verification 0.1](0015-verification-planning-0.1.md).

A resolver importing any requirement output 0.2 emits mundane-linked-0.2 with
link-cli-0.2. Its analyzer emits mundane-verification-0.2 with verify-cli-0.2.
All-old imports retain existing formats, metadata and seven-field findings.
The import JSON and verification-plan TSV contracts are unchanged.

Before linking, validate each complete requirement artifact's exact source/output
pairing, schema definition and declaration locations/provenance, present attributes,
requiredness, exact enum values, per-value name/value spans and source inventories.
Malformed, incomplete or unsupported imports publish no successful edges or analysis.
New schema/attribute fields in old requirement output are rejected, including empty
values, rather than treated as ignorable metadata. Other informational additions
retain the existing contract. Validation consumes serialized snapshots and pure
attribute rules only; source files and parser classes are not required.

Each import scope owns its independent snapshot, even if declaration names coincide.
Exact artifact pins still identify bytes. No schema merge, discovery or source reload
occurs. Requirement IDs remain human-authored identity, qualified by explicit scopes.

Comparison includes all seven normalized built-in values, present attributes, and
**the entire canonical schema definition**. Old output 0.1 is explicitly promoted to
empty attributes/null schema for this comparison. Old versus schema-free new can be
current. Selecting any schema versus no schema is review-stale. Changing optional
vocabulary, description, requiredness or any declaration is also review-stale for
every compared requirement in those scopes, even when its present values agree.
Invalid values against a revised schema fail import instead of producing stale rows.

Comment, source path, declaration path, digest, source-location, object ordering and
enum-member ordering changes alone do not alter comparison meaning. Enum values are
canonical sets; semantic strings remain exact. Conservative whole-schema comparison
can request more review than necessary; no per-attribute policy is implied.

Coverage rows keep existing fields and add:

- changedAttributes: sorted names whose present value differs (including addition or
  omission), empty for a declaration-only change.
- schemaChanged: true exactly when canonical definitions differ.
- changedFields: sorted differing built-in field names, plus attributes when
  changedAttributes is nonempty, and attributeSchema when schemaChanged is true.

State and possibleImpact retain their prior derivation from changedFields. Exit 1
with complete=true means stale/uncovered findings; failed linking yields incomplete
analysis with no findings and exit 2. No execution or satisfaction is inferred.
Old readers reject new output versions rather than silently using seven-field review.

The work-item analyzer also accepts requirement output 0.2 through this strict
serialized validator. Work analysis 0.1 and explicit typed relationship meanings are
unchanged; readers lacking support for the nested requirement version must reject it.
Attributes never synthesize work edges. This is a consumer capability addition, not
an authoring-language change for work items or verification plans.
