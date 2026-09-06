# Explicit editor imports and source mappings 0.1

Status: Selected incremental design (TC-2101)

## Selection and ownership

`mundane.workImports` optionally names a checked-in, workspace-relative JSON file:

```json
{"format":"mundane-editor-imports-0.1","manifest":"imports.json","sourceRoots":{"req":"vendor/device","other":"."}}
```

`manifest` selects the existing `mundane-imports-0.1` declaration. Its artifact
paths remain workspace-relative. `sourceRoots` maps every selected scope exactly
once to a workspace-relative directory; `.` explicitly means the workspace root.
A compiled source path is appended to that root. There is no implicit discovery,
Git checkout, network access, rebasing outside the workspace or automatic build.
The separate map adds editor location information without changing import meaning.
An empty setting preserves local-only assistance; it applies only to work items.

This batch accepts compiled requirements 0.1/0.2 and YAML work items 0.2. Each import
is validated with its published serialized validator. Scope `work` remains reserved
for the live local work selection. Duplicate/unknown fields, scopes, unsupported
kinds, dependency cycles, missing build dependencies and pin mismatches fail.
Pins bind exact compiled bytes. A null pin selects the current compiled snapshot.
Neither scope nor digest replaces an authored ID.

## Read-only wire extension

Editor protocol 0.1 gains an optional `imports` field on work requests only:
`{selection:{path,text}, manifest:{path,text}, artifacts:[{path,text}], sources:[{path,text}]}`.
The first two texts retain the exact authored JSON for strict Java validation.
Artifacts contain exact compiled JSON text; source paths are mapped workspace paths.
A null source text means the mapped file is unavailable, including missing files,
invalid UTF-8 or resolved paths outside the workspace. No source error is replaced
by different file content. The bridge reads no files and validates the complete
manifest and artifact inventory before exposing targets. Requirements requests
reject imports. Older bridges reject this added field; install the paired bundle.

Selection/manifest texts are bounded to 64 KiB each; at most 100 imports, 256 mapped
source files, 10,000 imported targets, 8 MiB per mapped source, and 16 MiB per artifact
and total encoded request/response. Source reads overlay open buffers. The client
watches the selection, manifest, compiled files and mapped sources, even missing
paths. Every relevant change invalidates work requests and their providers; the
independent requirements project remains unaffected by import-only edits.

## Resolution and source freshness

A relation resolves by its explicit scope, target kind and ID. Equal IDs in different
scopes are separate candidates. No fallback to a local ID or another scope is allowed.
Only `addresses`, `relates-to` and `supersedes` targets are considered, retaining the
work source's role/kind restrictions. Evidence and verification targets stay outside
this editor feature; the full CLI analyzer retains their existing behavior.

The compiled record supplies the title, status where present and exact artifact
revision for hover and completion. These remain available if mapped source is
missing or modified, clearly labeled as compiled data. A navigation target additionally
requires the current target file bytes to match its recorded source SHA-256 and its
structural ID origin to agree with the compiled location. This detects unsaved edits,
comment changes, relocation mistakes and forged/out-of-range origins. Checks apply
to the target's file; they do not certify every source file in the imported artifact.
A changed declaration or other file does not silently change compiled hover values.

Stale/unavailable/mismatched-origin targets produce warning diagnostics on the
relation and no jump. Missing scopes, wrong kinds or absent IDs produce errors on
the exact relation token. Invalid import configuration produces a configuration
marker and no imported assistance. Local prerequisite navigation remains available
when its own work snapshot is valid; import errors do not masquerade as local errors.
Without configured imports, relations retain the prior local-only editor behavior.

## Provider boundary and compatibility

Responses add `importDiagnostics`, `importNavigation` and `importTargets` to work
results. Import diagnostics carry severity; `valid` retains its local-source and
prerequisite meaning. Navigation entries map an exact authored reference span to
an exact mapped target span. Target descriptions carry explicit scope, kind, ID,
compiled digest and source state. Work origins gain typed relation spans only in
editor responses; compiled output formats do not change.

Completion requires a parsed relation mapping with a supported role, scope and kind.
It offers only targets from that scope/kind, with quoted insertions and literal hover.
It does not infer references from prose, suggest imported prerequisite dependencies,
change authored status, or save source. Current buffer versions and project generations
must still match before returning provider results. Existing requirement and local
work providers retain their behavior and independently versioned source contracts.

## Verification

Use checked-in sources compiled by the public commands, exact pins, two scopes
reusing IDs and both requirement output versions. Test source-root mappings, missing
and changed source, unsaved edits, origin tampering, output failure, invalid imports,
recovery, Unicode coordinates and prose exclusion. Repeat the workflow with an
isolated installed VSIX and extracted bridge, then run the authoritative gate.
