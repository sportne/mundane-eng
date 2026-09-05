# mundanereq-format

Purpose: normalize LF line endings and collapse only comment-free blank-line
runs between requirement records in conforming `mundanereq-source-0.2` source.

    mundanereq-format --check FILE_OR_DIRECTORY...
    mundanereq-format --write FILE_OR_DIRECTORY...

Check mode does not modify source. Write mode is explicit. The formatter does
not reflow prose, reorder records or relationships, alter comments, interpret
opaque math, or repair invalid source.

Run `mundanereq-format --help` for all modes and `mundanereq-format --version`
for its independent tool/source versions. The complete formatting and safety
contract is packaged as
`docs/contracts/0008-formatter-trial-contract-0.1.md`.

## Explicit YAML requirements mode

A leading `--source=yaml-0.3` selects YAML 0.3; `--source=yaml-0.4` selects the
attribute-aware profile. Use `--attribute-schema PATH` to select its checked-in JSON
declaration when the source names one. The default remains custom source 0.2. The selector precedes other modes/operations. The current
source contract and safety addendum are included under docs/contracts in the
native package and under specification in the repository. Output delivery failure
returns non-success, including diagnostic-only paths. See examples/yaml/README.md
in the repository for validated authoring and migration commands.

YAML formatting changes only CRLF to LF; it preserves authored comments, order,
quotes and indentation. It does not collapse YAML blank lines or format the JSON
declaration. Source and declaration snapshots are checked before replacements.

The YAML 0.4 contract and both structural schemas are included under
`docs/contracts/0020-project-attributes-yaml-0.4.md` and `docs/contracts/schema/`.
Use `--` before input paths that begin with a hyphen.
