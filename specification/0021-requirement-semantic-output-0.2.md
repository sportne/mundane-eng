# Requirement semantic output 0.2

Normative experimental addendum to [0012](0012-requirement-semantic-output-0.1.md).
YAML 0.4 emits mundane-req requirements output `mundanereq-requirements-0.2` and
compile-cli-0.2. Other source profiles still emit output 0.1 and their existing
producer/CLI metadata. Source and tool versions are independently declared.

## Representation

Output 0.2 keeps every field and meaning of 0.1 and adds attributeSchema to the
root, attributes to each values object, and attributes to each locations object.
SourceContract must be mundanereq-yaml-0.4. Complete output is deterministic UTF-8
JSON with sorted UTF-16 lexicographic maps, records and set-valued lists.

values.attributes maps present names to strings; omission becomes an empty map,
never defaults. locations.attributes maps each present name to {name: SPAN, value:
SPAN}. Built-in locations remain; an authored attributes mapping can also have its
whole field span under fields. The per-name locations exactly match present values.

attributeSchema is null for schema-free input, otherwise exactly:

- definition: the normalized definition from the validated YAML declaration, with maps sorted and enum arrays sorted
  as sets, preserving exact decoded strings.
- source: {path, sha256}, the exact declaration snapshot under the compiler root.
- locations: declaration names mapped to full declaration spans in that source.

Requirement sources remain in the existing sources array. The declaration has its
own provenance, not a competing entry in the requirement inventory. Locations use
one-based code-point positions and retained spans, without reparsing text. Empty
optional values never acquire invented value locations.

## Publication and compatibility

Interpret the selected declaration/source snapshots once. Before publishing a valid
0.4 result, bounded rechecks detect changed source/declaration bytes or file identity.
A detected change emits an incomplete artifact with no requirements and operational
exit 2 (input-changed or attribute-schema-changed). The final check is not a filesystem
transaction. Invalid schema emits attributeSchema null and no requirement records;
other invalid inputs may retain the valid schema snapshot but never usable records.
Output failures override to 2; a delivered prefix is unusable. Input and source errors
retain the 0/1/2 contract. Schema paths outside the explicit root are invocation errors.

New semantic fields must not be emitted under old output identifiers. Old readers
reject output 0.2; new consumers validate schema/requiredness/enum values and location
inventories before linking. A schema name never merges definitions across scopes.
Compiled declarations are derived snapshots; author declarations in source control.
Digests describe exact revisions, and human-authored requirement IDs remain identity.

Selected downstream comparison must include eight value fields and the complete
canonical schema definition; even a description or unused vocabulary change is
review-stale. Source/comments/path-only differences are provenance. Old output 0.1
can be explicitly promoted to empty attributes/null schema for comparison. Linking,
analysis and display support are delivered separately under TC-1306/1307.
