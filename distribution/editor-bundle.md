# Install the Linux YAML editor

This local bundle pairs a VS Code extension with its native `mundane-editor`
bridge. It is tested on Ubuntu 24.04 x86-64 with VS Code 1.109.5; VS Code 1.109+
is required. The bridge uses system glibc and zlib and has a glibc 2.34 symbol
ceiling. Other operating systems and architectures are not covered by this bundle.
No Java installation or compiler is needed to use the extracted bridge.

Verify the archive sidecar with `sha256sum -c <archive>.sha256` before extraction.
After extraction, run `sha256sum -c SHA256SUMS` from the extracted directory.
The checksums detect changed bytes; they are not a signature or publisher identity.

Install the VSIX from `extension/` using VS Code's **Extensions: Install from VSIX**.
Keep the extracted directory: `bin/mundane-editor` is the executable used by the
extension. Its executable permission is retained in the archive. Alternatively:

```sh
code --install-extension /absolute/path/to/bundle/extension/mundane-requirements-VERSION.vsix
/absolute/path/to/bundle/bin/mundane-editor --version
```

Replace VERSION with the version in `VERSIONS.json`. In workspace settings, set
`mundane.executable` to the absolute extracted bridge path and `mundane.project`
to a workspace-relative editor selection file. For example, `editor.json`:

```json
{
  "format": "mundane-editor-project-0.1",
  "source": "yaml-0.3",
  "files": ["requirements/system.mreq.yaml"],
  "attributeSchema": null
}
```

Files must exist inside the workspace. For declared attributes, select `yaml-0.4`
and the project-relative YAML declaration path in `attributeSchema`. Requirement
source must use the corresponding header and declaration name. No automatic file
or schema discovery occurs. Source files and human-authored IDs remain authoritative.

For task cards, set `mundane.workProject` to an existing YAML work-item manifest,
such as `roadmap/work-items.json` in this repository. It may coexist with the
requirements selection. The [authoring guide](../editors/vscode/README.md#yaml-task-cards-and-issues)
describes assistance, limits and the local-only analysis boundary. Work items do not
receive formatting edits. Optional `mundane.workImports` selects compiled targets
and explicit source mappings; the [import setup guide](../editors/vscode/README.md#imported-requirements-and-work-item-targets)
explains pins, revision warnings and recovery.

Trust the workspace before running its configured bridge. For WSL/Remote, install
the extension on the workspace side and use the Linux bundle only where that host
matches the supported environment. A Windows executable path is not a WSL path.

Use **Mundane: Validate Selected Project** after setup or replacement of the bridge.
Configuration and protocol errors appear on the selection file and in the Mundane
Authoring output channel. Correct the selected path, restore missing files, or
install the matching bundle, then validate again. Invalid project source blocks
navigation and formatting; syntax, reference and attribute diagnostics identify
what to repair. Formatting currently normalizes CRLF to LF while preserving layout.

`VERSIONS.json` records independent build/protocol/project versions.
`PACKAGE-INPUTS.json` records the exact bridge and VSIX hashes. Licenses and runtime
notices accompany the files. Assembly from identical inputs is deterministic; this
does not claim byte-identical native or VSIX rebuilds across toolchains. The bundle
does not install VS Code, alter source, publish a release or contact a marketplace.
