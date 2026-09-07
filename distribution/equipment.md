# Equipment commands

Use the [maintained prerequisites](build-verification.md):

```sh
make equipment-verify
build/maintained/mundane-equipment view --root build/gcs-equipment equipment.json
build/maintained/mundane-equipment bom --root build/gcs-equipment equipment.json
build/maintained/mundane-equipment wiring --root build/gcs-equipment equipment.json
```

`compile --root ROOT --imports SELECTION.json SOURCE.yaml` produces the compiled
artifact. `check`, `analyze`, `view`, `bom`, `wiring` consume it. Reports go to stdout,
errors to stderr, with the common 0/1/2 operation/content/availability status rules.

The [contract](../specification/0033-equipment-and-connectivity.md) describes profiles
and limitations. Modify one selected part or cable and regenerate both BOM and wiring.
The synthetic catalog is clearly labelled as assumptions, not approved manufacturer
or procurement data. `build/gcs-equipment` retains reproducible reports and inputs.
