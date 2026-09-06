# Work-item editor contract 0.1

Status: Selected incremental design (TC-2001)

## Selection decision

A trusted VS Code workspace folder may configure `mundane.workProject` with the
relative path of an existing `mundane-work-set-0.2` JSON manifest. Its source is
`mundane-work-yaml-0.2`; it explicitly selects one YAML card per path. No new
selection language or discovery is needed. `mundane.project` independently selects
requirements in the same folder; both use `mundane.executable`. Empty settings
disable their respective project. A file selected in both domains is ambiguous:
providers must decline it and report configuration failure.

The editor accepts 1–128 selected cards, each at most 1 MiB, within the existing
16 MiB request/response and 64 KiB selection limits. Larger manifests remain valid
for the CLI but require a smaller explicit selection for this editor. Selected
files must exist beneath the workspace; open buffers override disk contents.

## Snapshot decision

The existing editor protocol gains the additive source discriminator
`mundane-work-yaml-0.2`. Requirements requests retain their shape and meaning.
An older bridge rejects the unknown source; use the matching packaged bridge.

```json
{"protocol":"mundane-editor-0.1","source":"mundane-work-yaml-0.2","files":[{"path":"tasks/a.yaml","text":"format: mundane-work-yaml-0.2\nid: A\nkind: task\ntitle: Example\nstatus: Planned\nbody: Review this change.\n"}],"schema":null}
```

Responses use the existing diagnostics, definitions, suggestions and hover fields.
Work-item requests forbid a schema and never read files. Work source parsing and
value validation are shared with the CLI. Diagnostics additionally check local
prerequisites and cycles through shared graph rules. Cross-artifact relations are
checked for source shape only: this is not a full imports/evidence analysis.
Semantic failures are normal protocol responses; malformed requests exit 2.
No compiled artifact is synthesized or persisted as authoring state.

## Assistance decision

Navigation is limited to parsed `dependencies` scalar tokens and unique selected
task IDs. Invalid projects yield no definitions. Completion uses parsed structural
positions for kind-specific status values, relation roles and local task IDs in
dependencies or work-scoped work-item relation targets. It excludes self targets,
already used dependencies, prose, comments, block scalars, unsupported constructs
and ambiguous syntax. Incomplete scalar values may receive suggestions if the YAML
structure can still be composed; arbitrary broken YAML gets no guessed assistance.
Suggestions quote inserted values. Hover describes recognized fields/targets using
literal text; it grants no command or HTML trust. Work items receive no formatter.

Snapshot caching, cancellation, Unicode coordinate conversion and error recovery
follow the [requirements editor contract](0024-vscode-editor-0.1.md), with independent
sessions for each domain in each folder. Selection edits clear the previous files'
diagnostics. Requirement and work-item source versions remain independent.

## Evidence required

Compare buffer parsing with public disk compilation; test multi-file duplicate,
missing-dependency and cycle errors, recovery, unsaved target movement and non-BMP
positions. Exercise both domains in one actual VS Code host, irrelevant YAML and
literal-body exclusions, plus an isolated installed VSIX with its matching bridge.
Dogfood the explicit repository backlog. Do not infer imported-target validity,
executed work, approval, or platform support from successful editor checks.
