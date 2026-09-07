# Configuration baseline tooling

Use the normal [build prerequisites](build-verification.md):

```sh
make native-configuration
make configuration-verify
```

The workflow rebuilds five authored GCS configurations, exercises failures and writes
`build/gcs-configuration/configuration.md`. Individual native commands:

```sh
build/maintained/mundane-configuration compile --root build/gcs-configuration configuration-sim.yaml > build/gcs-configuration/configuration-sim.json
build/maintained/mundane-configuration resolve --root build/gcs-configuration configuration-sim.json
build/maintained/mundane-configuration view --root build/gcs-configuration configuration-sim.json
build/maintained/mundane-configuration compare --root build/gcs-configuration configuration-sim.json configuration-replacement.json
build/maintained/mundane-configuration publish --root build/gcs-configuration configuration-sim.json published
```

`check` revalidates a compiled baseline. `publish` returns a retained root and compiled
artifact path; use those as the next command's root/input to inspect the retained
revision. The [contract](../specification/0029-configuration-baselines.md) explains
source mapping, missing resources, publication and comparison limits. CLI output/error
behavior follows the architecture command conventions. These commands are independently
built; they are not yet included in the three-command archive or VS Code extension.

The authored fixtures under `examples/ground-control-station/engineering` select real
hashes of the staged inputs. Changes to selected source or version declarations need
an explicit fixture baseline revision, including changed predecessor pins. Verification
never silently repins inputs to make mismatches pass.
