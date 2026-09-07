# Java editor bridge

This component coordinates requirement and work-item authoring for the VS Code
client. Its Java packages and `mundanereq.editor.EditorMain` entry point are retained
for compatibility; its physical location reflects its cross-domain responsibility.

Run `make test-editor` or `make native-editor` from the repository root. Production
and test dependencies, output paths and isolation rules are described in the
[component boundary guide](../../distribution/components.md). The
[VS Code client](../vscode/README.md) owns extension packaging and UI integration.
