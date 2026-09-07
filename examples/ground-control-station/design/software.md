# Software provenance and security decision

TC-2416 selects native **in-toto Statement v1 / SLSA Provenance v1** and
**CycloneDX JSON 1.6** for the first offline adapters. Project trust boundaries,
threat/control relationships, scan execution state and review applicability use
YAML. Native documents remain authoritative, retained byte-for-byte and SHA-256
selected. We do not create a competing SBOM, VEX or build-provenance format.

## Standards and alternatives

- [SLSA build provenance](https://slsa.dev/spec/v1.2/build-provenance) describes
  outputs, builder, recipe/build type and resolved inputs. The adapter accepts
  `https://slsa.dev/provenance/v1` inside `https://in-toto.io/Statement/v1`, ignores
  unrecognized fields, and requires selected binary, source and recipe digests for
  this engineering use case. Missing optional upstream data is not proof of absence.
- [CycloneDX](https://cyclonedx.org/specification/overview/) already owns component
  identity, dependency relationships, vulnerabilities and VEX analysis. JSON 1.6 is
  the deliberately pinned initial adapter contract, not a claim to support every
  current CycloneDX version. Its official schema is vendored with revision, hashes
  and license in `dependencies/cyclonedx` for independent offline fixture checks.
  Native consumers check the consumed projection; they are not full standard validators.
- SPDX is a valid alternative SBOM family, but a second adapter adds no exercised
  capability to this case. CycloneDX covers both dependencies and VEX in one family
  and works with the existing strict JSON boundary. No package manager or scanner
  is introduced. Additional versions/formats require an independently tested adapter.
- DSSE/Sigstore verification remains a separate future adapter. These unsigned
  fixture statements establish byte correspondence, not builder authentication,
  SLSA level compliance, or secure build-system operation.

## Representation and interactions

A `mundane-software-yaml-0.1` source selects a configuration baseline, native binary,
source archive, build recipe, SLSA statement and CycloneDX BOM. All resources use
local relative path + SHA-256. Baseline membership must match the exact binary.
A complete scan selects a separate native CycloneDX vulnerability/VEX document and
an explicit scanner identity/version. No scan, failed scan, and a complete scan with
zero findings are separate states; none authorizes release.

Trust-boundary records reference architecture interfaces. Threats refer to those
boundaries; project controls link threats to requirements and safety controls.
Native VEX analysis owns exploitability state. YAML review records select exact
BOM, scan and configuration hashes with a reviewer and rationale; stale subjects
produce a finding rather than silently transferring the judgment. A vulnerability
is distinct from a threat scenario and its possible safety consequence.

CLI compile/check/analyze/query/view will use an independent software model and
source wiring. Compiled consumers reread pinned native resources without a YAML
parser. Queries retain unknown scan states and identify the selected configuration
and stage; release authorization belongs to the later release domain. Reports link
back to source and preserve limitations. Editor support remains TC-2424.

## Reference evidence

`check-software-design.py` builds a deterministic fixture zip archive from actual
local source and a fixed recipe, emits native provenance and CycloneDX fixtures,
validates the native CycloneDX documents against the pinned upstream schema, and
checks changed-output, missing-source, unsupported-version and unknown-scan cases.
The illustrative advisory is not a real vulnerability claim. No live scan occurs.
Generated native results and design evidence go to `build/gcs-software-design`.

The producer records allowlisted inputs only: no environment dump, credentials,
private keys or authenticated URLs. External identifiers are identities, never
instructions to fetch or execute. Adapters do not execute recipes or contact URLs.
Imported third-party documents must already be suitable for project retention;
these adapters do not promise arbitrary secret discovery or redaction.
