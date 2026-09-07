# Budget and reliability commands

Use the [maintained prerequisites](build-verification.md):

```sh
make budget-verify
build/maintained/mundane-budget calculate --root build/gcs-budget budget.json
build/maintained/mundane-budget view --root build/gcs-budget budget.json
```

`compile --root ROOT --imports SELECTION.json SOURCE.yaml` compiles YAML parameters,
formulas, checks and conditional reliability models. `check`, `calculate`, `analyze`
and `view` consume compiled inputs. `compare BEFORE.json AFTER.json` evaluates
retained revisions and marks an old result stale for changed inputs. Preserve both
revisions' native/compiled inputs so the comparison can check their exact identities.

The [contract](../specification/0034-budgets-and-reliability.md) defines units,
intervals, limits and evidence requirements. `build/gcs-budget/budget.md` reports
power/runtime/network/latency/compute/storage margins and conditional availability.
All current reference inputs are hypothetical assumptions. The workflow tests a
server substitution that exceeds power and runtime limits and exposes stale review.
Reports distinguish pass/fail/indeterminate/unavailable; none establishes approval.
