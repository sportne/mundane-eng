# Change commands

Use the [maintained build environment](build-verification.md):

    make change-verify
    build/maintained/mundane-change analyze --root build/gcs-change change-procedure.json
    build/maintained/mundane-change query --root build/gcs-change change-procedure.json procedure-stale 16
    build/maintained/mundane-change view --root build/gcs-change change-procedure.json

Compile with compile --root ROOT INVENTORY.yaml; check the emitted artifact with
check --root ROOT INVENTORY.json. Inventory paths are root-relative and pinned.
Analyze emits nodes, dependencies and explicit unresolved coverage; query adds
bounded prospective paths from a logical scope. View renders matching provenance.
These are separate native targets, outside the three-tool requirements package.
See the [contract](../specification/0037-semantic-change-and-staleness.md).
