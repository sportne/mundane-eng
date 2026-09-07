# Artifact ownership and interaction decision — TC-2403

Accepted design direction, 2026-09-07. This document and the local draft examples
are design evidence. Subsequent cards implement architecture, configuration, safety,
procedures and evidence alongside the [seed](../seed/README.md). Their published
contracts and registered versions are linked from the [current guide](../README.md).

## Ownership and proposed version boundaries

| Facts and owner | Authoring / compiled contract decision | Existing consumer interaction or future owner |
| --- | --- | --- |
| Requirements and descriptive attributes | Keep current YAML 0.3/0.4 and attribute declaration 0.1; compiled requirement 0.1/0.2 | Existing requirements tools; attributes cannot substitute for identified domain records |
| Planned verification activities and coverage | Keep plan YAML 0.1 and compiled plan 0.1 | Existing plan/link/verify commands; execution remains separate |
| Tasks and issues | Keep work YAML/compiled 0.2 | Existing work compiler/analyzer; corrective tasks keep these IDs |
| Contexts, modes, functions, components, interfaces and rationale | Implemented architecture YAML 0.1 / compiled architecture 0.1 | One architecture owner for the bounded first workflow; distinguish typed record kinds inside it |
| Hazards, controls, failure analysis | Implemented safety YAML 0.1 / compiled safety 0.1 | Safety analysis owner, consuming architecture/configuration/requirement revisions |
| Configuration selections, baselines, proposed changes | Implemented configuration YAML 0.1 / compiled configuration 0.1 | Configuration owner; immutable published snapshots and separate resource resolver |
| Procedures and planned test criteria | Implemented procedure YAML 0.1 / compiled procedure 0.1 | Verification-procedure owner; reference existing activities without embedding executions |
| Runs, observations, raw evidence | Native runner/log formats plus a versioned evidence adapter 0.1; authored manual observations use YAML | Evidence owner records provenance and outcomes; never changes a requirement to satisfied |
| Parts, equipment instances and connectivity | Reserve equipment YAML 0.1 / compiled equipment 0.1 | Hardware owner; native CAD retained by adapters, BOM/wiring derived |
| Quantities, calculations and reliability models | Reserve analysis YAML 0.1 / compiled analysis 0.1 | Bounded domain evaluators, pinned inputs and assumptions; no formula language selected yet |
| Code/build/dependency facts | Keep native code, build metadata and SBOM formats; independent adapter contracts | Software provenance owner; native files do not become YAML copies |
| Threats, vulnerabilities and project assessments | Reserve security YAML 0.1 / compiled security 0.1 | Security owner separates native findings from project exploitability decisions |
| Claims, reviews, obligations and exceptions | Reserve assurance YAML 0.1 / compiled assurance 0.1 | Assurance owner; evidence adequacy and verified authority are separate checks |
| Release/commissioning/incident records | Reserve operations YAML 0.1 / compiled operations 0.1 | Operations owner selects exact baselines and evidence; work items own corrective tasks |
| Cross-artifact comparison and impact | Preserve current impact 0.1 semantics; independently version future semantic-change analysis | Adapters consume compiled values; no universal source parser or authoritative graph |

“Reserve” identifies an independent version boundary, not an accepted field schema.
The relevant domain design card must still choose its bounded record shapes.
Architecture/configuration/safety/procedure 0.1 schemas are now published and accepted
by their own commands. Existing requirement/work/plan readers continue rejecting
other domains as source. The evidence adapter preserves native JSON runs, and manual
observations and assessments use their separately specified YAML sources.
The existing component graph is extended with concrete domain owners when implemented;
no empty GCS components or broad shared domain model are justified now.

## Identity, references and revisions

Use `{scope, kind, id}` for new domain references. Scope is an explicit artifact alias,
not a vehicle identity or a project-wide magic namespace. For example, `gcs-arch` and
`aircraft-arch` may each contain component `CMP-SERVICE` without collision. Requirement
references use a separate `gcs-req` alias. Alias changes are explicit selection changes;
no implicit cross-project ID search or rename inference is allowed.

For new local references, `self` is reserved for the containing artifact and is
rebound to its selected alias when imported. External references use explicit aliases;
`self` cannot select an external artifact. TC-2404 applies this local-reference rule.

Current work relations retain their existing `target` field and roles. An adapter can
normalize it to a new domain reference; this is not a source-format migration. Each
domain owns which reference kinds and relation meanings it accepts. Typed references
must reject both absent IDs and IDs found only under the wrong kind.

Human IDs identify entities. Content digests identify revisions. Baselines pin exact
artifact/resource bytes with SHA-256 and record the artifact format. A resource path
is a locator, never revision identity. Pin failures, unknown formats, conflicting scope
selections and unavailable resources produce explicit diagnostics. Paths resolve beneath
an explicit root; absolute paths, traversal and symlink escape cannot silently expand
selection. External URLs require an explicit retrieval/retention policy, not automatic
network access during analysis.

Current complete-compiled-artifact hashes remain usable immediately. A future domain
may expose a semantic revision digest only with a specified canonicalization/version;
raw-byte digests must not silently be replaced by normalized YAML hashes. Formatting
changes can change raw pins without proving semantic change. TC-2422 owns later
change-classification policy.

## Compilation, source locations and claims

The proposed compiled envelope has independent format/tool/source-contract identifiers,
explicit source inventory and hashes, typed records, field/source locations, completion
and diagnostics. A record retains its source ID; a compiled graph or report never owns
it. The new domains may reuse envelope conventions, not an unrestricted record payload
that bypasses domain schemas. Unknown fields and versions are rejected explicitly.

Domain compilation owns local syntax and semantics. Cross-scope linking consumes
selected compiled records; native-resource adapters record producer and source revision.
Reports consume compiled records and analysis. Removing YAML readers must not prevent
those consumers from inspecting already-compiled facts. Source navigation may become
unavailable without invalidating the stored identity or forging a newer source location.

A run outcome is an observation; a reviewer decision is a separate authored assessment;
release readiness is a derived check against a selected configuration and policy.
Editable reviewer names are identity claims, not verified approvals. A baseline hash
proves selected content equality, not authenticity, adequacy or permission to deploy.

## Worked interaction loop

1. **Author.** Edit GCS-STALE using the current source and refresh `make gcs-seed-verify`.
   Future architecture authoring edits interface `IF-TELEMETRY` and navigates its linked
   requirement and rationale. IDs remain explicit and human-readable.
2. **Compile.** Existing compilers publish complete snapshots. Future domain compilers
   do the same under independent contracts, rejecting malformed modes/ports/baselines.
3. **Resolve.** Use consumer-specific imports. The seed proved that verification accepts
   only requirement imports and work reserves its local scope; do not force a shared
   universal manifest through existing commands. A future selection adapter can produce
   these bounded manifests from a baseline while preserving scope/pin decisions.
4. **Inspect.** Current work and impact views navigate to staged sources. Future context,
   interface and configuration views must expose missing allocations, ambiguous versions,
   unresolved resource availability and the exact subject being inspected.
5. **Compare.** A freshness change produces one current review-stale row today. A future
   server substitution compares selected resource revisions and reports affected support;
   graph reachability alone cannot claim evidence invalidation or acceptance.
6. **Review.** A reviewer sees the exact requirement/interface/configuration revision and
   supporting run. Conflicting or missing evidence remains visible; a newer working copy
   cannot inherit an older approval by keeping its ID.

These interaction requirements now have native commands for the implemented domains.
Their source locations provide the basis for later editor integration under TC-2424. Reports remain generated; TC-0807 stays
conditional until authored composition is demonstrably required.

## Pressure cases and acceptance decision

The [local reference cases](reference-cases.yaml) use the
[draft common schema](common.schema.json) and the
[design checker](check-common.py). Run with the pinned schema environment:

```sh
bash scripts/check-yaml-schema.sh
build/schema-check-venv/bin/python examples/ground-control-station/design/check-common.py
```

The cases resolve colliding IDs through different scopes, reject an absent scope,
wrong kind, missing target, ambiguous selection, changed resource pin, unknown format
and unsafe resource path. They use actual resource bytes and SHA-256 comparisons.
The checker is a design experiment, not a production resolver. Its bounded capabilities
must not be advertised as new maintained platform support.

Rejected alternatives: untyped string links (ambiguous ownership); IDs as revision
proof (miss changed content); every domain as requirement attributes (hidden independent
records); embedding native code/logs in YAML (duplicate authority); universal source
compilation (coupled tools); automatic approval from passing tests (wrong authority).
The seed and these failures support the selected boundaries. Remaining domain-specific
schemas, evaluator semantics and reviewer authorization are owned by their design cards.

Recorded acceptance on 2026-09-07: the draft common schema passed Draft 2020-12
self-validation; all five scoped-reference cases and six selection/resource checks
passed. The runnable seed's observed consumer limitations are recorded above and
remain unchanged. The independent owner/version matrix covers every family in the
reference plan; domain field schemas remain subject to their individual design cards.
