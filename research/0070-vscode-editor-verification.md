# VS Code requirements editor verification

Status: Recorded repository checks

The full `scripts/run-ci-verification.sh` wrapper passed locally with the YAML-only
commands and VS Code editor integrated into `make verify`. The environment used
Linux x86-64, GraalVM CE 21.0.2, Node.js 22.18.0 and VS Code 1.109.5. The wrapper
records environment, output and exit status under ignored `build/ci-evidence/`.
This run records local results; it does not include hosted CI execution.

The gate passed twelve maintained Java test groups, YAML/schema/native-package
checks, version agreement, work-item and attribute workflows, impact graph
properties and four targeted impact mutations. Both deliberate CI faults were
rejected by their intended targets and their input files restored exactly.

Editor evidence includes:

- JVM/native serialized request parity, incomplete semantic results, unsupported
  protocols/cursors/selections and failed output delivery through `/dev/full`.
- Five Node tests covering explicit selection, process failure and cancellation,
  output bounds, buffer overlays, UTF-8/BOM handling, source containment, unpaired
  surrogates and code-point-to-UTF-16 conversion.
- Actual Extension Host activation and language registration; unsaved syntax,
  reference and declaration diagnostics; repair and overtaken-request rejection.
- Definition navigation across files, unsaved target movement, non-BMP source
  offsets, missing/duplicate targets and exclusion of prose.
- Actual Format Document application, project-wide invalid-input blocking,
  CRLF-to-LF preservation, idempotence and unchanged disk bytes.
- Partial attribute-key completion, used-key exclusion, enum insertion producing
  valid source, declaration changes and prose/comment/block-scalar boundaries.
- Hover token ranges, requiredness/types/values, unsaved descriptions rendered as
  literal text, invalid declaration clearing and prose exclusion.

`vsce` built `build/mundane-requirements-0.1.0.vsix`. Its contents are extension
code, grammar, manifest, license and guide. The native bridge is built independently;
no Marketplace publication or cross-platform validation is claimed. The
[editor contract](../specification/0024-vscode-editor-0.1.md) and
[installation guide](../editors/vscode/README.md) describe explicit selection,
resource limits and currently conservative navigation/formatting behavior.
