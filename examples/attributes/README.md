# Project-defined requirement attributes

These checked-in sources demonstrate the opt-in YAML 0.4 requirements profile.
The narrow YAML declaration is authored project configuration, independently selected
from the YAML requirement language. Human requirement IDs remain authoritative.
The [language reference](../../specification/0020-project-attributes-yaml-0.4.md) and
[command guide](../../distribution/attributes.md) define the actual rules.

The small example contains two logger requirements, an enumerated discipline and
an optional owner-team string. These are descriptive labels, not hazard assessments,
verification results or product-variant allocation assertions. Empty optional values
are omitted; no default is manufactured. The new example is repository-authored
illustrative material under the root [BSD 3-Clause license](../../LICENSE).

```sh
make native-suite native-compile native-plan native-link native-verification
python3 experiments/0036-project-attributes/workflow.py
```

Run from the repository root. This compiles two explicitly scoped snapshots, with
an exact baseline pin, and builds `build/attribute-example/report.html`. The current
example changes one owner label and the declaration's description. Both requirements
request schema review; only SYS-001 reports a changed attribute value. The checked-in [YAML plan](plan/plan.yaml)
asserts planned coverage, with no executed review or satisfaction claim. The script
checks exit 1 as the expected complete stale result, then deletes all derived JSON
and HTML and proves identical rebuild. Generated sources under build are controlled
copies of this example, with the documented edit applied; author actual projects in
your own source tree. The two source directories make both report baselines navigable.

## Medium example and provenance

`medium/` contains all 57 requirements across four files from
[the maintained YAML vaccine-monitoring example](../yaml/vaccine-monitoring).
Built-in values, IDs and decomposition edges are unchanged. The only source changes
are the explicit 0.4 header, project name and illustrative attributes. The layer enum
matches the file's existing needs/system/device/remote grouping; an optional team label
is present on the first requirement of each file. These labels are demonstration data,
not additional engineering findings or organizational ownership claims.

The underlying source is the hypothetical
SVEMS pilot, with its
source register
and original attribution preserved. Repository-authored derived requirements remain
under the root BSD 3-Clause license. No external-document text, user feedback or
interoperability result was added. Historical pilot and YAML 0.3 files are unchanged.

```sh
build/maintained/mundanereq-validate --source=yaml-0.4 --attribute-schema examples/attributes/medium/attribute-schema.yaml examples/attributes/medium
build/maintained/mundanereq-compile --source=yaml-0.4 --root . --attribute-schema examples/attributes/medium/attribute-schema.yaml examples/attributes/medium > build/medium-attributes.yaml
```

Select the small source file or the medium directory explicitly: the examples use
different schemas and are separate compilation units. A broad `examples/attributes`
directory selection includes both and correctly fails schema-name matching.
