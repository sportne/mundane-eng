# Work-item workflow verification

TC-1603 starts the maintained workflow with a strict Markdown/JSON compiler.
The source examples are illustrative project-authored material under the repository
license. `golden/compiled.json` records their derived values and exact source
provenance, never a second authoring source.

Run `make work-verify` with the documented Java/GraalVM environment. The public
JVM/native check covers task/issue values, Unicode declaration points, malformed
metadata/physical input, bounds, duplicate IDs/selection, CRLF body preservation and
closed/partial stdout. Source headers and JSON metadata are interpreted; body
Markdown remains opaque. Expected values and locations are asserted independently
before comparison with the recorded golden.

Linking, views and real-backlog migration follow in TC-1604–1606. No external tracker
or requirements attribute support is supplied by this compiler.
