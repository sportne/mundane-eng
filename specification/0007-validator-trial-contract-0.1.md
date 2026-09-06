# Requirement validator contract

The command `mundanereq-validate [--source=yaml-0.3|--source=yaml-0.4] [--attribute-schema PATH] [--] INPUT...`
validates the selected source set. YAML 0.3 is the default. Directory selection
includes `.mreq.yaml` files; explicitly named files may use other names. Traversal
excludes symlinks and `.git`. Paths sort and deduplicate deterministically.

`--help` and `--version` are standalone. Human diagnostics use file, one-based line,
Unicode code-point column, rule and message. Success writes a summary to stdout.
Invalid source exits 1; invocation, read or output failure exits 2. Source is never
written. [SARIF](0017-sarif-validation-output.md) uses `--output=sarif --root DIR`.
[Source rules](0010-requirements-yaml-0.3.md), [attributes](0020-project-attributes-yaml-0.4.md)
and [output safety](0011-tool-safety-and-yaml-commands.md) also apply.
