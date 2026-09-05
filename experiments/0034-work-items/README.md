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

TC-1606 converts the real 78-card backlog using `migration.json` and the one-time
`migrate.py --write` recipe. The replay checks the immutable pre-conversion Git
revision, every heading/body suffix and explicit metadata transform. Exceptional
completion and current-disposition headers are retained in planning.statusNote.
The replay describes the conversion checkpoint; future card edits remain allowed.

`regressions.py` runs eight deterministic seeds (160600–160607), 4–24 tasks each,
with independent expected values/prerequisites and ID-correction failures. Replay
one with `--seed N`; failures retain inputs under build/work-regression-failure-N.
Each invocation has a 30-second timeout and the suite a 180-second budget. Three
isolated Java mutations must compile and differ from passing baseline witnesses:
partial record publication, ignored unfinished prerequisites and ignored exact pins.
`results/mutations.json` records actual results; noncompiling/crashing mutants are
failures of the experiment, not counted as killed. No general mutation-coverage
claim or unobserved minimized defect is made.

`make work-backlog-verify` compiles, analyzes and regenerates the complete selected
repository backlog, compares the checked-in derived view and deletes/rebuilds the
intermediate artifacts. The generated root WORK-ITEMS.md replaces manual status
rows; roadmap prose remains authored. Typed references to existing requirement,
plan and activity fixtures are exercised by the public boundary suite. Local-resource
links in the migrated cards exercise actual code/specification citations.
