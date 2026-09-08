# Release and operations commands

Use the [maintained prerequisites](build-verification.md):

```sh
make operations-verify
build/maintained/mundane-operations view --root build/gcs-operations operations.json 2026-09-08T12:00:00Z trust.jwks.json
build/maintained/mundane-operations query --root build/gcs-operations operations.json 2026-09-08T12:00:00Z trust.jwks.json RC-CORRECTED
```

`compile --root ROOT --imports SELECTION.json SOURCE.yaml` emits a compiled artifact;
`check` takes that artifact. `analyze` and `view` additionally take evaluation time
and caller-selected public JWKS; `query` adds a candidate ID. Outputs and diagnostics
use stdout/stderr. Successful blocked analysis returns 0; contract/pin failure 1;
missing/unavailable resources 2. The [contract](../specification/0036-release-and-operational-traceability.md)
defines the bounded records and readiness rules. Editor integration remains separate.

The generated `build/gcs-operations/operations.md` and `analysis.json` retain ten
explicit tabletop executions: initial startup, backup, failed upgrade, rollback,
restore, queued-command restart, handover, replacement, anomaly and corrected startup.
The failed upgrade and anomaly-bearing candidate remain blocked. Corrective work
addresses GCS-RESTART; closure selects a descendant candidate and actual distinct
simulator run after an explicit session/procedure revision. The source, compiled
imports, native inputs and public trust selection are retained for reconstruction.

RC-INITIAL and RC-CORRECTED demonstrate local readiness using a synthetic time-scoped
assurance waiver. Removing the waiver, expiring reviews, changing observed build or
configuration, losing evidence, or leaving corrective work open blocks readiness.
No aircraft, host or deployment is operated. Operator actions/acceptance are authored
assertions; backup contents and actual field correctness are not established by the
fixture. Every report retains `authorization: none`.
