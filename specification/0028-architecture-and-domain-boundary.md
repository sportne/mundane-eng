# Architecture and domain artifact boundary 0.1

TC-2405 implements the bounded [architecture YAML schema](schema/architecture-yaml-0.1.json)
and the accepted GCS [design](../examples/ground-control-station/design/architecture.md).
The independent identifiers and tool versions are declared in `versions.properties`.

## Authored and compiled contracts

Architecture owns context, components, modes, functions, allocations, interfaces,
transitions, deployments and rationale. Existing requirements retain their owner.
Only the documented synthetic MAVLink profile and GCS internal/power profiles are
accepted. This compiler does not implement a wire protocol or execute vehicle intent.

YAML follows the existing bounded reader: UTF-8, final LF or CRLF, one document,
string mapping keys, no duplicate keys, directives, tags, anchors, aliases or merges,
and maximum depth 16. The new opt-in numeric profile accepts finite JSON-style decimal
numbers and exponents, with precision/scale bounded to 1000. Quantities require an
explicit unit; timing is positive milliseconds and nominal power-interface voltage
is positive volts. Existing requirement/declaration/plan profiles remain unchanged.
The source limit is 8 MiB; arrays are bounded to 10,000 records.

`mundane-architecture-0.1` has exactly these envelope fields: `artifactKind`, `format`,
`sourceContract`, `compiler` (name/version/contract), `complete`, `sources`, `locations`,
`imports`, `values`, `diagnostics`. A complete snapshot contains one source path and
SHA-256, normalized domain values (the source fields without `format`), and a source
point for each value using JSON Pointer keys. Points contain path, line and column.
It has no diagnostics. Consumers validate the envelope, closed schema and domain
semantics again; a JSON file declaring itself complete is not sufficient.

Compiled values are an independently validated interface. The generated Java schema
constant is derived from the checked-in schema, not a second authored schema. The
bounded structural evaluator supports only the local keywords used by these schemas;
it is not offered as a general JSON Schema implementation or a universal domain model.
Independent Python JSON Schema checks exercise the source contract in verification.

## Imports, identity and publication

An explicit JSON selection uses `mundane-domain-imports-0.1` and an `imports` sequence.
Each entry has exactly `scope`, `kind`, `format`, `path`, `sha256`. Scope names are
unique and cannot be `self`. Architecture imports compiled requirements using their
existing validators. Other existing tools retain their own import contracts unchanged.

References have `scope`, `kind`, `id`; local architecture facts use `self`. Requirement
aliases are selected explicitly and may differ between projects. Missing IDs, wrong
kinds, duplicate IDs/ports, missing allocations, incompatible endpoint directions,
profiles and dimensions, invalid deployment types, unsafe initial command mode,
missing command-enable guards and unsafe reconnect/handover effects are rejected.

Inputs resolve beneath an explicit root, including symlink checks; no network fetch
or implicit discovery occurs. File/aggregate limits and snapshot rechecks reuse the
maintained artifact infrastructure. Compilation publishes a complete JSON result on
stdout only after validation and input rechecks. Errors publish diagnostics on stderr
and no successful artifact. Output/flush failures return nonzero; shell redirection
can still leave a partial destination, so callers must check exit status.

`check` and `view` consume compiled JSON and selected imports without YAML readers.
Views recheck source hashes before navigation and report unavailable source instead
of linking to changed bytes. Generated Markdown includes context, allocation,
interface, mode, transition, deployment and rationale tables and a derived Mermaid
connectivity diagram. Values are escaped and diagram identifiers are validated IDs.
The result describes engineering intent, not installed hardware or approved operation.
