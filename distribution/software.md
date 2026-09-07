# Software provenance and security commands

Build with the [maintained prerequisites](build-verification.md):

```sh
make native-software
make software-verify
build/maintained/mundane-software view --root build/gcs-software software.json
build/maintained/mundane-software query --root build/gcs-software software.json EXAMPLE-REPLAY-001
```

`compile --root ROOT --imports SELECTION.json SOURCE.yaml` emits a checked compiled
artifact. `check`, `analyze`, `view` take that artifact; `query` also takes an advisory
ID. Output goes to stdout and diagnostics to stderr with common exit semantics.

The [contract](../specification/0032-software-provenance-and-security.md) documents
the SLSA/in-toto and CycloneDX profiles. Author project selections and judgments in
YAML; retain standard SBOM, VEX and provenance bytes as native JSON resources.
The worked archive is built locally from synthetic source and never contacts an
aircraft or scanner. Inspection exposes an illustrative dependency advisory through
its synthetic deployed configuration and safety-control chain. Generated reports
and retained inputs are in `build/gcs-software`; rebuilding does not repair edited
checked-in fixture pins. Authentication and release authorization are not established.
