# Research 0064: Attribute formatting and trace

TC-1304 enables explicit YAML 0.4/declaration selection in formatter and trace.
The formatter preserves all authored bytes except CRLF normalization, including
attribute ordering, quotes and comments. Trace validates complete values/definitions
and retains its existing decomposition graph. The JSON declaration is never a write
target. Old source/command profiles remain unchanged.

Each replacement rechecks the declaration snapshot and source snapshot immediately
before rename, including fallback rename. A detected schema edit reports the schema
path, returns 2 and retains the completed/remaining-file report. The documented
final-check/rename race remains. A deterministic between-writes test changes the
schema after the first file; that write remains recorded, later sources remain
unchanged, the external schema edit survives and temporary output is cleaned up.

Public JVM/native checks exercise check/write modes, idempotence, comment and value
preservation, invalid-schema/value no-write barriers and trace parity. A separate
parse-format-parse check compares complete requirement values and schema definitions.
Existing output failure and source-change regressions remain active.

`make verify` exited 0 with all 20 JVM groups and authoritative native/integration
checks in the working checkout. [Recorded results](../experiments/0036-project-attributes/results/1304-verify.txt)
retain actual passing checks. No editor integration, JSON formatting, new trace
policy or filesystem transaction was added. [Usage](../distribution/attributes.md)
and specification 0020 describe the implemented flags, versions and limits.
