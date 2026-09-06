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

A 200 ms debounce groups edits. Every change immediately clears previous diagnostics
and aborts pending requests. A generation check rejects responses overtaken by
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
