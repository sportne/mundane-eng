# Safety analysis tooling

Build and exercise the reference workflow with the normal Java/GraalVM prerequisites:

```sh
make native-safety
make safety-verify
```

The workflow writes `build/gcs-safety/safety.json` and `safety.md`. Individual commands:

```sh
build/maintained/mundane-safety compile --root build/gcs-safety --imports safety-imports.json safety.yaml > build/gcs-safety/safety.json
build/maintained/mundane-safety analyze --root build/gcs-safety safety.json
build/maintained/mundane-safety query --root build/gcs-safety safety.json HZ-STALE
build/maintained/mundane-safety view --root build/gcs-safety safety.json
```

`check` revalidates compiled input. Command success means the requested analysis
completed; read its findings and riskAcceptance field. An unresolved risk is a valid
record to inspect, not a successful safety determination. The [contract](../specification/0030-safety-analysis.md)
and [design guide](../examples/ground-control-station/design/safety.md) explain the
scope, reference loop and limitations. CLI exit conventions match architecture.
