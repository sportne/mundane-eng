# Requirement trace command

From the repository root:

```sh
make native-trace
build/maintained/mundanereq-trace impact SYS-001 examples/yaml/vaccine-monitoring
```

YAML 0.3 is the default. Use a leading `--source=yaml-0.4` for project attributes,
with `--attribute-schema PATH` when the source names a declaration. Directory
selection includes `.mreq.yaml`; explicit files may use other names. `--` ends
options. `--help` and `--version` are standalone.

The [command contract](../specification/0009-trace-trial-contract-0.1.md) defines operations and exit codes.
[Source rules](../specification/0010-requirements-yaml-0.3.md),
[attribute rules](../specification/0020-project-attributes-yaml-0.4.md), and
[output and write safety](../specification/0011-tool-safety-and-yaml-commands.md)
apply. A failed output prefix is unusable. Source and human IDs remain authoritative.
