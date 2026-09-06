# VS Code requirements editor

VS Code 1.109 or newer is required. This workspace extension uses the same Java interpreter as the requirement commands.
It operates on explicit YAML 0.3/0.4 projects. Other engineering artifact languages
keep their own contracts.

The [Linux bundle installation guide](../../distribution/editor-bundle.md) explains
how to install the paired VSIX and native bridge. Build that bundle with
`make package-editor` using the environment in the
[contributor build guide](../../distribution/build-verification.md). The archive,
checksums, versions and runtime notices are written under `build/editor-package/`.
The bundle does not publish to a marketplace or bundle VS Code itself.

For extension development, `make editor-verify` builds the bridge, runs Node and
actual Extension Host checks (including two-folder request traffic), and builds
the VSIX. Tests download the pinned VS Code build into `build/vscode-test` and use
disposable example workspaces. The VSIX contains no native platform code; configure
the paired bridge in the same extension host, including the workspace side of
Remote/WSL sessions. Standalone builds remain available with `make native-editor`
and `npm ci && npm run package` from `editors/vscode/`.

In VS Code settings, select `mundane.executable` as the absolute path to
`build/maintained/mundane-editor` and `mundane.project` as a workspace-relative
JSON project file. The compiler runs only in trusted, local workspaces. Multi-root
workspaces configure each folder independently. An empty project setting disables
that folder; there is no traversal or implicit file discovery.

```json
{
  "format": "mundane-editor-project-0.1",
  "source": "yaml-0.3",
  "files": ["requirements/system.mreq.yaml", "requirements/device.mreq.yaml"],
  "attributeSchema": null
}
```

For YAML 0.4 select `"source": "yaml-0.4"` and a project-relative JSON attribute
declaration path, or null for a schema-free project. Open unsaved buffers override
the selected disk files. Files must already exist inside the workspace. The explicit
manifest may also be edited without saving. Use **Mundane: Validate Selected Project**
to exercise the configured bridge; configuration/process failures appear in the
Mundane Requirements output channel.

See the [editor contract](../../specification/0024-vscode-editor-0.1.md) for
snapshot limits, coordinates and supported behavior. Generated results are never
written back as authoring source.

Syntax, semantic, reference and declaration diagnostics refresh after a 200 ms
pause in editing. Results from older snapshots are discarded, and repaired markers
clear immediately. Open declaration and selection buffers participate in validation.
Errors have the same rule identifiers as the command-line validator.

**Go to Definition** on a `decomposes` target opens its selected requirement ID.
Navigation uses current parser spans, including unsaved target edits. Repair project
errors first: invalid snapshots deliberately provide no navigation targets.

**Format Document** preserves source layout, comments, quoting and attribute order;
its current change is CRLF-to-LF normalization. It requires a valid complete project
snapshot and returns ordinary VS Code edits. Save and undo remain editor actions.

In a declared YAML 0.4 project's `attributes` mapping, **Trigger Suggest** offers
unused attribute names (with required/optional type detail) and allowed enum values.
Enum insertion includes quotes. Partial key lines are supported; other syntax errors
may need repair before the YAML structure can establish a completion context.
Prose and comments receive no attribute suggestions.

Hover over a declared attribute name or scalar value to see its type, requiredness,
description, permitted values and declaration location. Authored documentation is
rendered as literal text; embedded HTML and commands are not enabled. Hover refreshes
from unsaved schema changes and disappears when the declaration becomes invalid.

Editor package, bridge and protocol declarations are maintained in
`versions.properties`. After changing them, run
`python3 scripts/editor-versions.py --write` from the repository root. Normal
verification checks the generated metadata, package and lockfile without updating
them silently. `mundane-editor --version` reports the paired build metadata.
