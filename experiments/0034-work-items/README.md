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

TC-1604 adds strict typed imports and graph analysis. The analysis golden and
public tests cover requirement/plan/activity/work-item scopes, prerequisites,
supersession, invalid references/pins and execution without source-parser classes.

TC-1605 adds a deterministic Markdown view with revalidated findings and escaped
source-linked navigation. `golden/view.txt` stores expected Markdown bytes using
the analysis root as link base; it is not a standalone file-relative web page.
