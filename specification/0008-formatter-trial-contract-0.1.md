# Requirement formatter contract

`mundanereq-format [--source=yaml-0.3|--source=yaml-0.4] [--attribute-schema PATH] MODE INPUT...`
validates the full selected source set before formatting. YAML 0.3 is the default.
Modes are `--check`, `--write`, or `--stdout FILE`. `--` ends option processing.
Standalone `--help` and `--version` describe invocation and profile.

Formatting normalizes CRLF to LF and otherwise preserves bytes: comments, quotes,
order, block indicators and indentation. Invalid source is never rewritten.
Check exits 0 if unchanged and 1 if formatting is needed. Write/stdout succeed with
0. Invalid source, invocation, I/O or output failure exits 2. Atomic replacement is
attempted, with permission preservation and temporary cleanup. Detected intervening
edits stop the batch and identify completed and unprocessed paths. There is a race
between recheck and rename. [Safety](0011-tool-safety-and-yaml-commands.md) defines
failure behavior. Attribute declarations are validated but never formatted.
