# VS Code requirements editor

This local extension uses the same Java interpreter as the requirement commands.
It operates on explicit YAML 0.3/0.4 projects. Other engineering artifact languages
keep their own contracts.

Build `make native-editor` with the Java/GraalVM environment in the
[build guide](../../distribution/build-verification.md). With Node.js 22 or newer:

```sh
cd editors/vscode
npm ci
npm run test:unit
xvfb-run -a npm test
npm run package
```

The host test downloads VS Code into the ignored `build/vscode-test` directory.
Linux needs the usual Electron libraries and Xvfb when no display is available.
Tests use a disposable workspace in `build/editor-test-workspace`.
Install `build/mundane-requirements-0.1.0.vsix` with **Extensions: Install from VSIX**.
The VSIX contains JavaScript, grammar and documentation; build the native bridge
for your host separately. No Marketplace publication or bundled platform binary
is implied.

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
