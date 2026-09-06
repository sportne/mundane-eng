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
project files. Initial responses echo `protocol` and the selected file count.
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
