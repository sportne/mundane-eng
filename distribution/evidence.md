# Execution and evidence tooling

Use the normal [Java/GraalVM prerequisites](build-verification.md):

```sh
make native-procedure native-evidence
make evidence-verify
```

The workflow stages a copy of the executing native evidence tool as a selected runtime
resource, compiles procedures and writes native runs, compiled evidence and
`build/gcs-evidence/evidence.md`. Individual steps:

```sh
build/maintained/mundane-evidence simulate --root build/gcs-evidence procedure-stale.json runtime/mundane-evidence nominal > build/gcs-evidence/run-stale.json
build/maintained/mundane-evidence import --root build/gcs-evidence procedure-stale.json run-stale.json > build/gcs-evidence/evidence-stale.json
build/maintained/mundane-evidence view --root build/gcs-evidence evidence-stale.json
build/maintained/mundane-evidence analyze --root build/gcs-evidence procedure-stale.json evidence-stale.json
```

Use suppress-stale instead of nominal to deliberately suppress the stale-state
annunciation and observe failed criteria. This is an explicit simulator fault model.
The simulator does not read expected values to produce observations.

Manual and assessment examples, using the exact selected revisions:

```sh
build/maintained/mundane-evidence normalize-manual --root build/gcs-evidence manual-inspection.yaml runtime/mundane-evidence > build/gcs-evidence/run-manual.json
build/maintained/mundane-evidence import --root build/gcs-evidence procedure-inspection.json run-manual.json > build/gcs-evidence/evidence-manual.json
build/maintained/mundane-evidence assess --root build/gcs-evidence assessment.yaml > build/gcs-evidence/assessment.json
build/maintained/mundane-evidence analyze --root build/gcs-evidence procedure-inspection.json evidence-manual.json assessment.json
```

Analyze can accept multiple evidence and assessment artifacts. It preserves conflicting
runs, distinguishes unavailable assessment subjects and does not establish adequacy
or authorization. `check` revalidates compiled evidence. As with the architecture
commands, exit 0 means the requested operation completed, 1 rejected content/invocation,
and 2 unavailable/changed input or output failure. Read the analysis fields for results;
consume redirected output only after a successful command exit.

Runs use native JSON, manual observations and assessments use YAML, and compiled
interfaces remain JSON. Changing the selected procedure or runtime requires a new
explicit evidence revision; retained logs are never silently repinned. The
[contract](../specification/0031-procedures-and-evidence.md) documents provenance,
resource bounds and the strictly simulation/synthetic scope. The legacy three-command
archive and VS Code extension are unchanged; their GCS expansion remains planned.
