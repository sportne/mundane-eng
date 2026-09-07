# Software provenance and security 0.1

The [source schema](schema/software-yaml-0.1.json) owns project relationships and
explicit native selections. The [design decision](../examples/ground-control-station/design/software.md)
explains standards reuse and intentionally unsupported adapters.

`mundane-software` implements compile/check/analyze/query/view with the common
local-root/import-selection/envelope conventions. Source format, compiled format,
producer version and command contract are independently declared in versions.properties.
Exit 0 means the requested operation completed, 1 rejected content/invocation,
2 unavailable input or output failure. Findings are data and never approval.

## Native adapter profile

SLSA accepts unwrapped in-toto Statement v1 with Provenance v1 predicate. Unknown
fields are ignored as the standard requires. Exactly one subject must match the
binary SHA-256. Source and recipe each require one URI and SHA-256 match in
resolvedDependencies. A nonempty builder ID and build type remain producer claims.
The adapter does not fetch URIs, execute recipes or establish signatures/SLSA levels.

CycloneDX JSON 1.6 accepts a flat component inventory with unique bom-ref values,
explicit type/name/version/SHA-256 and one metadata root matching the binary.
Every inventory member must have a dependency record; reachable dependencies are
traversed with cycle protection. Native vulnerability affects references must identify
inventory members; native analysis state/detail supply VEX judgments. Duplicate
advisory IDs, nested components and affects version ranges are rejected by this
bounded profile. This profile is stricter than the upstream schema; importing it is
not a claim that arbitrary unconsumed standard fields were validated. Raw native
bytes and their unknown fields remain retained in the selected files.

A complete scan must select a BOM with the same component identity projection and
reachable set as the selected SBOM. A complete empty scan, no-scan and failed scan
remain distinct. Scope/completeness and real scanner trust are not inferred from
empty findings. A scan may be a synthetic offline fixture and must identify its
producer/version accordingly.

## Cross-artifact checks and review

The exact binary path/hash must occur in the selected configuration's selected
members. Native resources are read through bounded snapshots and rechecked before
output. Threat boundaries reference real architecture interfaces; project controls
reference real requirements and safety controls. The selected safety control's
configuration import must match the software configuration revision.

A review remains current only if its advisory exists and its exact SBOM, scan and
configuration digests match. Changed dependencies or unavailable scan evidence do
not inherit old judgments. Queries return the selected configuration/stage and
advisory findings, plus project control relationships. These are potential exposure
and authored claims, never automatic exploitability, risk acceptance or release
approval. Release records belong to TC-2420/TC-2421.

Model classes and native format adapters depend only on compiled artifact owners;
source wiring owns YAML. Missing source YAML cannot block check/analyze/query.
Native resource absence or tampering does block them. Source-linked views verify
source bytes before offering navigation. All source/native/import limits come from
the existing bounded domain infrastructure (16 MiB per native input, 128 MiB total).
