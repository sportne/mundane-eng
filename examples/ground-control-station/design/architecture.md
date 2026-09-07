# Context, architecture and interface design — TC-2404

Accepted bounded design on 2026-09-07. The source candidate is
[architecture.yaml](architecture.yaml), checked by the
[published schema](../../../specification/schema/architecture-yaml-0.1.json) and
[independent design probe](check-architecture.py). TC-2405 now implements the
[native architecture commands](../../../distribution/architecture.md). Wire protocol
execution and new-domain editor support remain outside this implementation.

## Representation and ownership

A single architecture artifact owns typed context, mode, component, function,
interface, deployment, transition and decision records. This keeps the first
engineering slice reviewable without introducing eight independent source formats.
IDs are unique within a record kind. `self` references the containing artifact;
external requirements use the explicit `gcs-req` selection. The
[selection](architecture-selection.yaml) pins the actual compiled requirements
produced by the seed. Imports cannot infer matching IDs from unselected files.

The seven component records distinguish software, purchased-hardware design and
external autopilot responsibilities. Functions own their allocations and references
to required behavior; requirements retain their statements. Four software deployments
select the single illustrative server for the synthetic harness. These are desired
allocations, not evidence of actual installation. The
[configuration design](configuration.md) distinguishes designed, built and deployed
records using explicitly synthetic fixtures.

Each interface owns endpoints, named port selection, profile version and semantic
policy. Port direction, signal, units and profile must agree at both ends. The
illustrative 24 V power interface demonstrates electrical units and connectivity
ownership; it is neither a selected server rating nor a complete wiring diagram.
Detailed connector/pin/protection evidence belongs to TC-2412, not a free-text field
that pretends to provide those checks here.

Decisions own rationale and alternatives. DEC-INTERFACES explains why heartbeat,
freshness, command acceptance, observed completion and authority are separate facts.
A reference to a decision is not evidence that a reviewer accepted its adequacy.

## Authoring and inspection

1. Edit a mode, allocation, interface policy or rationale in the YAML source.
2. Run `make gcs-design-verify` from the repository root. With built seed outputs
   and the pinned schema environment, rerun only this probe with:

   ```sh
   build/schema-check-venv/bin/python examples/ground-control-station/design/check-architecture.py
   ```
3. Inspect `build/gcs-design/architecture.md`: function allocations, interface
   endpoint/profile table, all negative-case findings, a source link and a derived
   Mermaid topology. `architecture-evidence.json` records the actual input pins.
4. The [case ledger](architecture-cases.yaml) applies explicit edits to independent
   copies of the valid document; the original source is never mutated by the probe.
   Missing allocation and incompatible interface rows remain visible in the view.

All schema properties are closed; unknown fields, missing required command policy
and invalid primitives are rejected. The semantic probe additionally checks typed
references, port compatibility, positive timing dimensions, deployment kinds and
mode/session guards. The probe's implementation is local research scaffolding; it
is not an alternative production representation or a promise of complete validation.

## Four interaction walkthroughs

| Scenario | Authored facts to inspect | Expected engineering interpretation |
| --- | --- | --- |
| S01 Startup | OFFLINE initial mode; TR-START, TR-MONITOR, TR-ENABLE and FUN-GATE | Startup moves through preflight and monitoring. COMMAND-ENABLED requires configuration, identity, authority, freshness and trusted time. Removing those guards is rejected. |
| S02 Live heartbeat, stale telemetry | IF-HEARTBEAT versus IF-TELEMETRY, FUN-FRESH and FUN-DISPLAY | A recent heartbeat does not make data fresh. An age estimate of 490 ms with 20 ms upper uncertainty gives 510 ms and fails the 500 ms freshness boundary. New intent is inhibited immediately; display update is separately bounded at 100 ms. Unknown timing also prevents eligibility. |
| S03 Late acknowledgement | IF-COMMAND, GCS-ACK, FUN-COMMAND and DEC-INTERFACES | A request without acknowledgement at 1000 ms has unknown outcome. An acknowledgement at 1200 ms cannot retroactively prove completion or promote a different session's state. Inspect fresh observed state before renewed intent; only profile-declared idempotent requests in the same validated session may retry, at most twice. |
| S05 Authority handover | TR-HANDOVER and TR-ENABLE | Revoke existing authority, discard old intent and inhibit commands before returning to preflight. A new grant requires all enabling guards. No transition permits two simultaneous command authorities. |

These are tabletop interpretations of authored constraints, not executed simulator
runs. The draft command policy deliberately adds a synthetic session/request envelope;
MAVLink message identifiers are not claimed to provide this correlation. Real protocol
profiles, retries and execution observations require the actual autopilot/adapter
selection and tests. The [reference-system decision](../decisions/reference-system.md)
records those limits and protocol sources.

## Implementation decision and stop conditions

TC-2405 should publish independently versioned architecture source and compiled
contracts, local compile diagnostics and explicit external linking. Preserve the
current requirement/work source contracts. Use separate source-reader and compiled
model boundaries consistent with TC-1104: compiled consumers must not require the
architecture YAML parser or requirement source classes. Do not add draft support to
current requirement or work compilers. Diagnostic codes in this probe are candidate
semantics; production codes and field/source locations require an explicit contract.

The bounded first release needs these records and checks, not arbitrary behavioral
code in YAML, a graphical editor or a full systems-modeling language. It must support
the source-linked views and import failures demonstrated here. TC-2424 owns full
editor integration. TC-2422 owns revision-aware semantic invalidation; changing a
profile or threshold should produce review candidates, not an automatic safety result.

Stop and refine the design if a consumer needs independently owned model libraries,
multiple protocol revisions within one endpoint, unit conversion, or executable guard
expressions. None is silently enabled by this draft. Operating modes describe ground
intent policy; aircraft maneuver selection remains an external unresolved decision.

## Recorded acceptance

The Draft 2020-12 schema and thirteen cases passed on 2026-09-07. They include the
valid example, missing allocation, incompatible profile, wrong kind, missing requirement,
missing mode, unguarded enable, unsafe handover, timing dimension error, missing command
completion policy, unknown field, duplicate component and invalid deployment host.
The probe resolved actual pinned seed requirements and generated allocation, interface
and failure-case inspection output. Repeated runs produce identical design outputs.

## Maintained implementation

TC-2405 implements this representation in the independently built architecture
compiler and compiled consumer. Use the [architecture commands](../../../distribution/architecture.md)
and [published contract](../../../specification/0028-architecture-and-domain-boundary.md).
The original design-case checker remains independent regression evidence, using the
single published schema. The authored fixture now expands its former YAML aliases
to comply with the maintained no-alias profile. Configuration pins were explicitly
revised for that source change and the new version declarations.
