# Architecture tooling

Build with the existing [Java/GraalVM prerequisites](build-verification.md):

```sh
make native-architecture
make architecture-verify
```

The second command stages the GCS seed and writes `build/gcs-architecture/architecture.json`
and `architecture.md`. Reproduce individual steps from the repository root:

```sh
build/maintained/mundane-architecture compile --root build/gcs-architecture --imports imports.json architecture.yaml > build/gcs-architecture/architecture.json
build/maintained/mundane-architecture check --root build/gcs-architecture architecture.json
build/maintained/mundane-architecture view --root build/gcs-architecture architecture.json > build/gcs-architecture/architecture.md
```

Paths after `--root` are relative to that root. Imports are explicit compiled snapshots.
Exit 0 means success, 1 rejected content or invocation, 2 unavailable/changed input or
output failure. Consume stdout only after checking the exit status. `--version` and
`--help` are independent of an input selection.

Edit the interface's freshness quantity, compile and inspect the owning interface,
its endpoints and rationale. Recompile after deliberate input revision changes;
a pin mismatch never causes an automatic repin. Source links require the exact
selected source bytes. The [contract](../specification/0028-architecture-and-domain-boundary.md)
describes supported profiles and limitations.

These are standalone native commands. The existing three-command package and VS Code
extension do not yet include architecture authoring; editor and distribution extension
remain owned by the corresponding GCS cards.
