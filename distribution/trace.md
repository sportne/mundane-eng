# mundanereq-trace

Purpose: navigate authored `decomposes` relationships in a conforming
`mundanereq-source-0.2` source set.

    mundanereq-trace parents ID FILE_OR_DIRECTORY...
    mundanereq-trace children ID FILE_OR_DIRECTORY...
    mundanereq-trace higher ID FILE_OR_DIRECTORY...
    mundanereq-trace impact ID FILE_OR_DIRECTORY...

Direct operations list immediate higher- or lower-level requirements.
Transitive operations report one deterministic shortest path per result and
reachable cycles. “Impact” identifies requirements to inspect; it does not
predict which requirement prose must change.

The command derives incoming links and graph state for one process invocation.
It does not modify source, retain an index, enforce decomposition or cycle
policy, or provide arbitrary relationship queries.

Run `mundanereq-trace --help` for the concise invocation summary and
`mundanereq-trace --version` for its independent tool/source versions. The
complete interface is packaged as
`docs/contracts/0009-trace-trial-contract-0.1.md`.

## Explicit YAML requirements mode

A leading `--source=yaml-0.3` selects YAML 0.3; `--source=yaml-0.4` selects the
attribute-aware profile. Use `--attribute-schema PATH` to select its checked-in JSON
declaration when the source names one. The default remains custom source 0.2. The selector precedes other modes/operations. The current
source contract and safety addendum are included under docs/contracts in the
native package and under specification in the repository. Output delivery failure
returns non-success, including diagnostic-only paths. See examples/yaml/README.md
in the repository for validated authoring and migration commands.

The YAML 0.4 contract and both structural schemas are included under
`docs/contracts/0020-project-attributes-yaml-0.4.md` and `docs/contracts/schema/`.
Use `--` before input paths that begin with a hyphen.
