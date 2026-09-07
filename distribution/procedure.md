# Procedure compilation

Build `make native-procedure` with the normal Java/GraalVM prerequisites. Run
`make evidence-verify` for the complete native example. From the repository root:

```sh
build/maintained/mundane-procedure compile --root build/gcs-evidence --imports procedure-imports.json procedure-stale.yaml > build/gcs-evidence/procedure-stale.json
build/maintained/mundane-procedure check --root build/gcs-evidence procedure-stale.json
build/maintained/mundane-procedure view --root build/gcs-evidence procedure-stale.json
```

Procedures own expected observations and time windows; the selected baseline owns
resource revisions. Explicit heartbeat/telemetry/command references keep connection
health, data freshness and acknowledgement policy distinct. See the
[contract](../specification/0031-procedures-and-evidence.md) and
[evidence commands](evidence.md) for execution and interpretation.
