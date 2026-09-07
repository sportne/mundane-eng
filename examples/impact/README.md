# Logger change-impact example

These checked-in files are authoritative example source: requirements YAML 0.4,
a project attribute declaration, an independent YAML verification plan, and YAML
work items. The [workflow](../../experiments/0037-impact-analysis/README.md) copies
them into a disposable analysis root, compiles them, pins the imports and renders a
report whose source links resolve to those copied snapshots.

Changing the selected SYS-001 recording requirement reaches DEV-001, its storage
verification activity, the logger plan, two addressing tasks and a dependent task.
It does not reach the independent power activity or the context-only issue. The
completed storage task remains Complete; it can still be a review candidate.
Baseline/current imports deliberately have equal bytes but separate scopes.

`work-items.json` selects paths under `source/` in the staged root. Run the workflow
from the repository root; do not invoke that manifest with this directory as root.
Edit examples here and regenerate. Files under `build/impact-example/` are disposable
copies and outputs, not a second maintained source.
