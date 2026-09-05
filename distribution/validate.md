# mundanereq-validate

Purpose: determine whether explicitly selected requirements files form a
conforming `mundanereq-source-0.2` source set.

    mundanereq-validate FILE_OR_DIRECTORY...

The command reads source and reports diagnostics; it does not modify files,
format source, answer trace questions, or enforce project policy. Status `0`
means conformance, `1` means readable source did not conform, and `2` means an
invocation or input failure prevented validation.

Run `mundanereq-validate --help` for the concise invocation summary and
`mundanereq-validate --version` for its independent tool/source versions. The
complete interface is packaged as
`docs/contracts/0007-validator-trial-contract-0.1.md`.

## Explicit YAML requirements mode

A leading `--source=yaml-0.3` selects YAML 0.3; `--source=yaml-0.4` selects the
attribute-aware profile. Use `--attribute-schema PATH` to select its checked-in JSON
declaration when the source names one. The default remains custom source 0.2. The selector precedes other modes/operations. The current
source contract and safety addendum are included under docs/contracts in the
native package and under specification in the repository. Output delivery failure
returns non-success, including diagnostic-only paths. See examples/yaml/README.md
in the repository for validated authoring and migration commands.

## SARIF diagnostics

The current validator adds an explicit machine-readable mode:

```sh
mundanereq-validate --source=yaml-0.3 --output=sarif --root . requirements/ > validation.sarif.json
```

Retain the process exit status: 0 means valid source, 1 means nonconformance,
and 2 means invocation/input/output failure. Valid JSON does not imply valid
requirements. A consumer supplies the same source root for relative artifact URIs.
Diagnostics use Unicode code-point start positions; no unmeasured end range is
invented. The package includes `docs/contracts/0017-sarif-validation-output.md`;
the source checkout keeps that contract under `specification/`. Default text output remains available without the selector.

The YAML 0.4 contract and both structural schemas are included under
`docs/contracts/0020-project-attributes-yaml-0.4.md` and `docs/contracts/schema/`.
Use `--` before input paths that begin with a hyphen.
