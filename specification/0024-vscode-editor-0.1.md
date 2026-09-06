# VS Code editor contract 0.1

Status: Incremental requirements editor interface

## Decision and boundary

Use direct VS Code language providers backed by one versioned, read-only Java
snapshot process. A separate language-server protocol adds no present consumer;
the snapshot bridge leaves another editor integration possible without moving
requirement semantics into JavaScript. VS Code supplies YAML token coloring via
a grammar include, document buffers, edit application and provider UI.

A configured workspace folder owns an explicit `mundane-editor-project-0.1` JSON
selection: `format`, `source` (`yaml-0.3` or `yaml-0.4`), `files` (1–128 unique
relative regular-file paths) and `attributeSchema` (relative path or null).
Reject unsupported fields, profiles, duplicate paths, traversal and files resolving
outside the folder. There is no directory discovery. Requirement and declaration
source contracts retain their own versions and human-authored identities.

The native `mundane-editor` executable accepts a single JSON request on stdin:
`protocol: "mundane-editor-0.1"`, `source`, `files: [{path,text}]`, and
`schema: {path,text}` or null. Paths are normalized folder-relative paths. Buffer
snapshots are authoritative for that request; the bridge never reads or writes
project files. Responses echo `protocol`, `valid` and `diagnostics` containing path, line, column,
code and message. Semantic errors use the interpreter's stable rules and point
locations. VS Code displays error markers without inventing token end ranges.
Invalid requests exit 2 with an `editor-request` stderr message; output delivery
failures also exit 2. Semantic diagnostics are successful protocol responses.

## Resource and freshness contract

Select at most 128 requirement files, 8 MiB each, a 1 MiB declaration and a 64 KiB
selection. Encoded requests and responses are bounded to 16 MiB; JSON nesting is
bounded by the shared parser. The extension starts no shell, bounds captured stderr
to 4 KiB, terminates requests after 15 seconds and supports cancellation. It executes
only the configured absolute local executable in a trusted file workspace.

Open buffer versions, selection and declarations participate in snapshot freshness.
An edit invalidates dependent results; newer snapshots supersede in-flight requests.
Providers must not apply results or edits from an older snapshot. Closed source
files are watched, while open buffers take precedence over disk notifications.

Interpreter positions are one-based Unicode code points, with half-open origin
spans. VS Code positions are zero-based UTF-16 units. Convert against the exact
snapshot text, including non-BMP characters. Decode disk UTF-8 strictly.

## Compatibility and tests

The interface is experimental and independently versioned. Unknown protocol IDs
fail explicitly. No requirement source syntax is added, no generated file becomes
source, and no behavior is inferred for work items, safety artifacts or BOMs.

Actual VS Code Extension Host checks cover activation and the bridge; owning cards
add tests for their providers. The [extension guide](../editors/vscode/README.md)
describes building, installing and exercising a local VSIX.

## Live diagnostics

A 200 ms debounce groups relevant edits within each project. A selected source,
selection or declaration change clears that project's diagnostics and aborts its
pending requests. Unrelated files leave results intact. A per-project generation
check rejects responses overtaken by
buffer, selection, declaration, configuration or selected-disk changes. Project
validation overlays all open buffers, so references and schemas are checked together.
Configuration, UTF-8, resource or process failures mark the selection and are logged
in the output channel. No request writes to disk. Unselected YAML files receive no
Mundane diagnostic or authoring operation.

## Definition navigation

Valid snapshot responses include definitions (human `id`, ID-value `location` and
explicit decomposition `references` with target IDs and spans). Go to Definition
only matches the reference token, never prose. It opens the unique selected target's
ID-value span, including unsaved origin changes. Invalid project snapshots provide
no definitions, so duplicate IDs, missing targets and incomplete parses cannot
produce a guessed jump. Navigation resumes after project errors are repaired.

## Formatting

Valid responses include `formatting` entries only for sources containing CRLF.
Each entry gives `path` and the exact text with CRLF replaced by LF. Invalid whole
project snapshots produce no edits. VS Code receives a text edit plus an LF end-of-line
edit; its normal undo/save workflow applies them. No schema formatting, field
reordering, quote changes, comment movement or rewriting of opaque text is added.
A changed document version or cancelled request cannot supply edits.

## Attribute completion

A request may include `cursor: {path,line,column}` using the same code-point
coordinates and a selected requirement path. The response includes `suggestions`
with label, insertion text, replacement location and required/optional type detail.
Assistance requires a valid explicitly selected declaration and matching YAML 0.4
header/schema name, even when requirement values are incomplete.

Java composes the cursor source with the same bounded YAML engine. It recognizes
only `requirements[i].attributes`, excludes already used keys, and quotes enum
insertions as YAML-compatible JSON strings. For an incomplete, space-indented key
line it may temporarily append `: null` (or use an empty-key placeholder) solely to
identify that structural path. It never validates, publishes or writes the repaired
text. Prose, block scalars, comments, unrelated mappings, ambiguous structures,
anchors, aliases and unsupported YAML constructs do not acquire attribute meaning.
Malformed structures that this one-line repair cannot resolve produce no suggestions.

## Attribute hover

Cursor responses include `hover` or null. A hover identifies the actual parsed
attribute name or simple scalar value token, the selected declaration's type,
requiredness, description, enum values and declaration source span. Unknown names,
invalid declarations and ambiguous syntax produce no hover. Hover never repairs
source. The extension converts the exact token range and displays all authored
content using literal Markdown text with HTML and command trust disabled.

## Version declarations

`versions.properties` owns `EDITOR_VERSION`, `EDITOR_PROTOCOL` and `EDITOR_PROJECT`.
The package version describes the paired extension/bridge build; the protocol and
project selection identifiers change independently. Java uses generated constants;
JavaScript reads checked-in generated `editors/vscode/versions.json`. Package and
lockfile versions are checked against the same declarations. Update them explicitly
with `python3 scripts/editor-versions.py --write`; ordinary verification fails on
stale metadata. `mundane-editor --version` emits the three declarations as JSON.
Protocol mismatches fail explicitly rather than accepting a differently shaped
response. Adding this metadata command changes no requirement source semantics.

## Project request reuse

Each configured folder retains one read-only snapshot per generation and at most
four response entries, including pending requests. Identical concurrent requests
share a promise; repeated cursor queries reuse their result. Different folders do
not cancel each other. Eviction aborts a pending evicted request; invalidation or
disposal aborts every pending request for that project. Selection, schema, selected
source, relevant settings, opening/closing selected buffers and folder removal
participate in invalidation. Explicit validation deliberately refreshes all selected
projects. A cancelled provider cannot apply its result even when another consumer
continues using a shared request. No cache is written to disk.
