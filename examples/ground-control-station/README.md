# Ground control station engineering reference project

Status: reference-system decisions are recorded; subsequent deliverables follow
the authoritative card statuses in the generated backlog. The [runnable seed](seed/README.md) uses current tools; this directory does not
yet contain an executable GCS or maintained new artifact schemas. YAML sketches here are proposals for discussion through the
cards, not inputs accepted by current compilers.

Completed deliverables: [reference-system decision](decisions/reference-system.md)
and [current-tool seed](seed/README.md), plus the
[artifact ownership decision](design/ownership.md). New artifact designs remain separate from
maintained compiler support.

## Purpose and success criteria

Use one civilian inspection/survey UAV ground control station to develop the
missing engineering artifact and workflow capabilities in Mundane-Eng. The
example must connect operational intent to architecture, interfaces, safety and
security reasoning, hardware selection, verification evidence and the exact
configuration being reviewed or operated.

A successful example lets an engineer answer these questions from explicit,
versioned sources and reproducible derived results:

- What must the station do, in which mode, and who owns each responsibility?
- What can go wrong, which controls address it, and where are the remaining gaps?
- Which software build and installed hardware were actually evaluated?
- Which observations support a claim, and who assessed their adequacy?
- What becomes stale when a requirement, part, connection, assumption or build changes?
- Why is a particular release ready, blocked, disputed or subject to an exception?

The example is a design probe and regression fixture. Passing it demonstrates
support for the exercised workflows, not completeness for all UAV projects or
regulatory acceptance. Applicability and tailoring of engineering obligations
are outputs of TC-2401, not assumptions hidden in these sketches.

## Reference system and delivery boundary

The initial delivery is an engineering project around a simulated aircraft/link
and a small reference GCS harness. Physical installation, flight testing and real
aircraft command transmission are outside this initiative. Simulated results and
hypothetical hardware values must remain visibly identified as such.

```text
operator UI ---- GCS service ---- link adapter ---- simulated UAV/autopilot
                     |                |
              event recorder     external link
                     |
              purchased server ---- network switch
                     |                  |
                     +------ UPS -------+
                              |
                          mains input
```

The sketch identifies logical and power relationships; it is not a wiring design.
The project will select purchased server, UPS, switch and link equipment, record
ports/cables and installation assumptions, and calculate bounded resource budgets.
Detailed PCB, mechanical and radio design remain external native resources if
needed later. Map/time services are explicit external dependencies if the chosen
scenarios require them; they must not become implicit trusted inputs.

Responsibility boundary accepted for simulation in the
[TC-2401 decision](decisions/reference-system.md):

- Ground: operator authorization and command intent, vehicle/session identity,
  command lifecycle, trustworthy status presentation, event recording, updates
  and recovery. Start with one command authority and a read-only observer.
- Onboard: stabilization and execution of aircraft-local responses to lost links
  or invalid commands. Select and pin an autopilot/profile assumption; the ground
  station cannot guarantee delivery after link loss.
- Interface: specify command acceptance versus execution, duplicate handling,
  telemetry age, clocks, reconnect and authority handover. Do not assume every
  aircraft supports the same command or a universally safe return-home action.

Use MAVLink as a candidate external protocol, with a selected message/service
profile and version. The [command protocol](https://mavlink.io/en/services/command.html)
and [message signing documentation](https://mavlink.io/en/guide/message_signing.html)
are inputs to the interface/security design. Application completion criteria and
operator authority require explicit project decisions; wire-format support alone
is insufficient. No private signing keys belong in the example.

## Scenario coverage ledger

TC-2401 resolves numeric thresholds and responsibility assumptions. TC-2402 records
what existing tools can express. TC-2426 executes the full ledger after the new
capabilities exist. Each scenario needs a normal case, a negative or changed case,
expected state, input revisions, commands/review steps and retained evidence.

| Scenario | Engineering question and required observable result | Main owning cards |
| --- | --- | --- |
| S01 Startup and preflight | Are vehicle identity, selected configuration, operator authority and readiness known before commands are enabled? | TC-2404, TC-2408, TC-2420 |
| S02 Stale telemetry with live connection | Does the UI distinguish data freshness from connectivity and expose uncertain state under delayed or missing data? | TC-2404, TC-2406, TC-2410 |
| S03 Ambiguous command outcome | What happens on lost, late, rejected or repeated acknowledgements, and how is actual execution established? | TC-2404, TC-2410 |
| S04 Reconnect and restart | Can old queued intent be replayed, or status from a previous session mistaken for current state? | TC-2404, TC-2416, TC-2420 |
| S05 Wrong vehicle and authority handover | Are identities and active authority unambiguous across clients and sessions? | TC-2404, TC-2406, TC-2416 |
| S06 Combined mains and aircraft-link loss | Are ground behavior, onboard assumptions, shared power causes and degraded operation independently justified? | TC-2406, TC-2412, TC-2414 |
| S07 Overload, full disk and clock discontinuity | Can latency, evidence completeness or freshness become unknown without being reported? | TC-2410, TC-2414, TC-2420 |
| S08 Untrusted traffic and vulnerable dependency | Which trust boundary/control/configuration is affected, including safety consequences? | TC-2416, TC-2418 |
| S09 Server, UPS or cable substitution | Which interface, budget, reliability assumption, test or review needs reassessment? | TC-2412, TC-2414, TC-2422 |
| S10 Software update, failed commissioning and rollback | Which exact build was evaluated, and is the previous configuration still recoverable and supported? | TC-2408, TC-2410, TC-2420 |
| S11 Failed, missing or conflicting evidence and waiver | Can an incomplete claim or expired exception incorrectly appear ready? | TC-2410, TC-2418 |
| S12 Deployment drift, maintenance and incident | Can observed configuration and corrective work be traced into a newly reviewed baseline without rewriting history? | TC-2408, TC-2420, TC-2422 |

## Artifact ownership and interactions to develop

All new human-authored engineering records default to YAML, using the existing
bounded authoring conventions. Reuse existing requirements, declarations, plans
and work items. A requirement attribute can classify a requirement; it should not
hide an independently identified hazard, test execution or equipment assembly.

The family boundaries below are proposals. TC-2403 decides whether a family needs
its own compiler, several record kinds in one domain, or an adapter to an existing
native artifact. Every implemented family needs explicit selection, validation,
compiled provenance, typed linking and explainable inspection. Reports and diagrams
are derived; generated JSON remains appropriate for compiled integration.

| Family and authoritative facts | How an engineer should interact with it | Design / implementation |
| --- | --- | --- |
| Existing requirements, attributes, verification plans, work items | Author YAML, compile/link, inspect coverage and prospective impact; retain current semantics | TC-2402 |
| Context, modes, functions, components, allocations, interfaces and decisions | Author responsibilities/contracts/rationale; inspect mode, allocation and interface views; navigate a displayed connection to its owner | TC-2404 / TC-2405 |
| Hazards, controls, bounded FMEA and fault tree | Record causes and assumptions; inspect control coverage and unresolved residual risk; trace obligations | TC-2406 / TC-2407 |
| Configurations, baselines and change proposals | Select and pin artifacts/resources; compare revisions and as-designed/as-built/as-deployed states | TC-2408 / TC-2409 |
| Procedures, runs, observations and evidence | Author reproducible criteria; execute/import native results; inspect exact environment and supporting resources; assess separately | TC-2410 / TC-2411 |
| Equipment, part selections, instances and connectivity | Author assembly and connections; derive BOM/wiring; inspect rating evidence and substitutions | TC-2412 / TC-2413 |
| Quantities, budgets and reliability/availability analyses | Author units, input provenance and assumptions; evaluate bounded formulas; inspect margins, sensitivity and validity | TC-2414 / TC-2415 |
| Software/build provenance, threats, vulnerabilities and controls | Import pinned native build/SBOM/scan facts; author project judgments; query affected configurations and safety links | TC-2416 / TC-2417 |
| Assurance claims, reviews, waivers and obligations | Assemble reasoning with exact supporting revisions; inspect unsupported/disputed/stale claims and reviewer authority limitations | TC-2418 / TC-2419 |
| Releases, commissioning, operations, incidents and maintenance | Evaluate readiness, record actual deployment/execution, compare drift and connect corrective work to new evidence | TC-2420 / TC-2421 |
| Cross-artifact semantic changes and derived staleness | Compare baselines, explain affected support, record dispositions and rerun/review without overwriting history | TC-2422 / TC-2423 |

Code, protocol definitions, CAD, SBOMs and logs retain their native canonical forms.
Adapters identify their producer, version, resource location and content digest.
Do not copy native content into YAML just for visual consistency. Large evidence
may live outside Git with explicit retrieval/retention policy; an unavailable
resource is an observable condition, never successful evidence.

## Worked representation: trustworthy telemetry

These fragments deliberately have no production `format` identifier. They explore
ownership and linking, not a prematurely fixed schema. Angle-bracket values are
unresolved placeholders; production compilers must not accept them as evidence.
TC-2402 will first express the requirement and plan in their actual existing schemas;
TC-2403 and the domain design cards decide the proposed reference/revision syntax.

```yaml
# Proposed interface record; threshold selected and justified by TC-2401.
id: IF-TELEMETRY
producer: {scope: aircraft, kind: component, id: CMP-AUTOPILOT}
consumer: {scope: ground, kind: component, id: CMP-GCS}
freshness:
  maximum_age: {value: "<selected threshold>", unit: ms}
  clock_basis: "<defined timestamp and clock uncertainty policy>"
  on_unknown_age: mark-state-unknown
rationale: {scope: ground, kind: decision, id: DEC-FRESHNESS}
```

The requirement states the required behavior; the interface owns timestamp and
freshness interpretation. A decision owns the rationale for the selected threshold.
Changing that threshold should initiate reassessment of both control reasoning and
verification criteria, even if the interface's human ID remains unchanged.

```yaml
# Proposed safety records, shown together only to illustrate relationships.
hazard:
  id: HAZ-STALE-STATE
  context: operator issues intent using obsolete aircraft state
  control: {scope: ground, kind: safety-control, id: CTL-FRESHNESS}
control:
  id: CTL-FRESHNESS
  requirement: {scope: ground, kind: requirement, id: REQ-STALE-STATE}
  interface: {scope: ground, kind: interface, id: IF-TELEMETRY}
  verification: {scope: ground, kind: verification-procedure, id: PROC-STALE}
```

```yaml
# Proposed authored procedure, distinct from a verification-plan activity.
id: PROC-STALE
verifies: {scope: ground, kind: requirement, id: REQ-STALE-STATE}
environment: simulated-aircraft-link
steps:
  - action: deliver telemetry with increasing age while the session remains live
  - action: observe displayed freshness and command eligibility
expected:
  - criterion: state becomes visibly stale or unknown at the selected boundary
  - criterion: command eligibility follows the reviewed mode/control policy
boundary_cases: [just-before-limit, at-limit, beyond-limit, unknown-clock-age]
```

```yaml
# Proposed imported run metadata; these placeholders describe fields, not a real run.
id: RUN-STALE-001
procedure:
  ref: {scope: ground, kind: verification-procedure, id: PROC-STALE}
  digest: "<actual procedure content digest>"
configuration:
  ref: {scope: ground, kind: baseline, id: BL-SIM-001}
  digest: "<actual baseline content digest>"
producer: {tool: "<runner>", version: "<pinned version>"}
outcome: inconclusive
evidence:
  - uri: "<explicit log resource>"
    digest: "<actual log content digest>"
limitations: [simulation-only, acceptance-threshold-not-yet-selected]
```

A reviewer inspects the observations and applicability before deciding whether they
support a claim. The reviewer decision names exact subject/evidence revisions and
its identity assurance; typing a person's name into YAML does not prove approval.
A later passing run supplements the failed/inconclusive history rather than
rewriting it. Release readiness consumes those decisions and unresolved obligations.

## Worked representation: power and substitution

```yaml
# Proposed calculation inputs. All values are hypothetical design-probe data.
id: BUD-POWER
configuration: {scope: ground, kind: configuration, id: CFG-GROUND-HW}
inputs:
  server_peak: {value: 120, unit: W, basis: illustrative-assumption}
  switch_peak: {value: 15, unit: W, basis: illustrative-assumption}
  link_peak: {value: 10, unit: W, basis: illustrative-assumption}
  usable_stored_energy: {value: 150, unit: Wh, basis: illustrative-assumption}
  conversion_efficiency: {value: 0.85, unit: "1", basis: illustrative-assumption}
outputs:
  total_load: {formula: server_peak + switch_peak + link_peak, unit: W}
  runtime: {formula: usable_stored_energy * conversion_efficiency / total_load, unit: h}
```

The illustrative steady-load result is 145 W and about 0.88 hours. It is not a UPS
qualification: missing effects include transients, load-dependent performance,
battery aging/temperature, internal consumption and the chosen required duration.
The design task must define model validity and evidence for those assumptions.
A 200 W replacement server yields 225 W and about 0.57 hours under the same model;
that change must be visible in the budget, affected tests and review readiness.

Part records own rating evidence; equipment instances own installation identity;
connectivity owns actual port/cable relationships. The power analysis references
selected equipment and assumptions rather than duplicating its BOM. The wiring
view is generated from connectivity. A datasheet or native drawing remains a
pinned resource, with loss/change of that resource reported explicitly.

## Workflow and delivery order

1. **Establish the probe (TC-2401–TC-2403).** Resolve the boundary and obligations,
   build a runnable seed with existing tools, then decide ownership and interaction
   contracts using its observed limitations.
2. **Design bounded domains (even-numbered design cards TC-2404–TC-2420).** Work
   positive, invalid and changed examples before accepting schemas. Configuration
   design can proceed independently; later assurance/operations designs consume
   earlier decisions. Actual prerequisites are authoritative in the cards.
3. **Implement accepted domains (paired odd-numbered cards TC-2405–TC-2421).**
   Deliver source contracts, compiled adapters, checks and reference fixtures.
   Each card includes CLI inspection; editor/report integration follows separately.
   Split a card if design reveals independently deliverable contracts too large
   for one reviewable outcome, and preserve the original decision history.
4. **Connect change, authoring and review (TC-2422–TC-2425).** Semantic impact design
   can begin from accepted designs. Its implementation integrates domain outputs;
   editor and review views follow and can proceed independently of one another.
5. **Demonstrate, distribute and clean (TC-2426–TC-2428).** Execute the full scenario
   ledger, document actual support and installability, then remove superseded drafts
   and representations while retaining useful decision and verification evidence.

For each scenario, the target interaction is: author a change in YAML or its native
owner; validate/compile; resolve explicit imports; inspect the changed domain;
compare the baseline; see impacted support; execute/import verification; review
adequacy; produce a readiness view for an exact candidate. Required negative cases
must remain blocked or unknown until their actual missing information is supplied.
The command vocabulary is to be designed; no new CLI is implied by this document.

The existing authored-view card retains its condition: derived review views do not
by themselves justify a new view language. TC-1104 has established the
[component boundaries](../../distribution/components.md); new GCS implementations
extend those boundaries when their domain ownership is decided. Refer to the
[roadmap](../../roadmap/0001-initial-roadmap.md) for those decisions.

## Task cards

These cards are authoritative YAML work items selected by the repository manifest.
The [generated index](../../WORK-ITEMS.md) tracks current status; the table below
explains the planned decomposition and prerequisites, not a delivery estimate.

| Card | Outcome | Prerequisites |
| --- | --- | --- |
| [TC-2401](../../roadmap/closed/task-2401-bound-reference-system.yaml) | Define the GCS Reference System and Engineering Obligations | None |
| [TC-2402](../../roadmap/closed/task-2402-seed-existing-tool-workflow.yaml) | Build the GCS Seed with Existing Artifact Tools | TC-2401 |
| [TC-2403](../../roadmap/closed/task-2403-define-artifact-interactions.yaml) | Define Artifact Ownership and Common Interaction Contracts | TC-2402 |
| [TC-2404](../../roadmap/task-2404-design-context-architecture-interfaces.yaml) | Design Context Architecture and Interface Artifacts | TC-2403 |
| [TC-2405](../../roadmap/task-2405-implement-context-architecture-interfaces.yaml) | Implement Context Architecture and Interface Workflows | TC-2404 |
| [TC-2406](../../roadmap/task-2406-design-safety-analysis.yaml) | Design Hazard Control and Failure Analysis Artifacts | TC-2403, TC-2404 |
| [TC-2407](../../roadmap/task-2407-implement-safety-analysis.yaml) | Implement Safety Analysis and Control Traceability | TC-2406, TC-2405, TC-2409 |
| [TC-2408](../../roadmap/task-2408-design-configurations-baselines.yaml) | Design Configuration Baseline and Change Records | TC-2403 |
| [TC-2409](../../roadmap/task-2409-implement-configurations-baselines.yaml) | Implement Reproducible Configuration Baselines | TC-2408 |
| [TC-2410](../../roadmap/task-2410-design-verification-evidence.yaml) | Design Procedures Execution Results and Evidence | TC-2403, TC-2404, TC-2408 |
| [TC-2411](../../roadmap/task-2411-implement-verification-evidence.yaml) | Implement Procedures Runs and Evidence Queries | TC-2410, TC-2405, TC-2409 |
| [TC-2412](../../roadmap/task-2412-design-equipment-connectivity.yaml) | Design Equipment Parts and Wiring Artifacts | TC-2403, TC-2404, TC-2408 |
| [TC-2413](../../roadmap/task-2413-implement-equipment-connectivity.yaml) | Implement Equipment Selection and Wiring Views | TC-2412, TC-2405, TC-2409 |
| [TC-2414](../../roadmap/task-2414-design-quantities-budgets-reliability.yaml) | Design Quantities Budgets and Reliability Analyses | TC-2403, TC-2408, TC-2412 |
| [TC-2415](../../roadmap/task-2415-implement-budgets-reliability.yaml) | Implement Budget and Reliability Calculations | TC-2414, TC-2409, TC-2413 |
| [TC-2416](../../roadmap/task-2416-design-software-security-provenance.yaml) | Design Software Provenance and Security Artifacts | TC-2403, TC-2404, TC-2408 |
| [TC-2417](../../roadmap/task-2417-implement-software-security-provenance.yaml) | Implement Build Provenance and Security Traceability | TC-2416, TC-2405, TC-2409 |
| [TC-2418](../../roadmap/task-2418-design-assurance-reviews.yaml) | Design Assurance Arguments Reviews and Waivers | TC-2406, TC-2408, TC-2410, TC-2416 |
| [TC-2419](../../roadmap/task-2419-implement-assurance-reviews.yaml) | Implement Assurance and Review Readiness Queries | TC-2418, TC-2407, TC-2411, TC-2417 |
| [TC-2420](../../roadmap/task-2420-design-release-operations.yaml) | Design Release Commissioning and Operational Records | TC-2408, TC-2410, TC-2418 |
| [TC-2421](../../roadmap/task-2421-implement-release-operations.yaml) | Implement Release and Operational Traceability | TC-2420, TC-2409, TC-2411, TC-2419 |
| [TC-2422](../../roadmap/task-2422-design-semantic-change-impact.yaml) | Design Semantic Change and Evidence Invalidation | TC-2403, TC-2404, TC-2406, TC-2408, TC-2410, TC-2412, TC-2414, TC-2416, TC-2418, TC-2420 |
| [TC-2423](../../roadmap/task-2423-implement-semantic-change-impact.yaml) | Implement Cross-Domain Change and Staleness Queries | TC-2422, TC-2407, TC-2411, TC-2413, TC-2415, TC-2417, TC-2419, TC-2421 |
| [TC-2424](../../roadmap/task-2424-extend-editor-artifact-workflows.yaml) | Extend Editor Authoring for the Reference Artifacts | TC-2405, TC-2407, TC-2409, TC-2411, TC-2413, TC-2415, TC-2417, TC-2419, TC-2421, TC-2423 |
| [TC-2425](../../roadmap/task-2425-deliver-engineering-review-views.yaml) | Deliver Source-Linked Engineering Review Views | TC-2405, TC-2407, TC-2409, TC-2411, TC-2413, TC-2415, TC-2417, TC-2419, TC-2421, TC-2423 |
| [TC-2426](../../roadmap/task-2426-exercise-adversarial-gcs-workflows.yaml) | Exercise the Complete GCS Engineering Workflow | TC-2402, TC-2423, TC-2424, TC-2425 |
| [TC-2427](../../roadmap/task-2427-package-document-engineering-platform.yaml) | Package and Document the Extended Engineering Workflow | TC-2426 |
| [TC-2428](../../roadmap/task-2428-clean-reference-platform-material.yaml) | Clean Superseded Reference Project Material | TC-2427 |

## Planning validation

On 2026-09-07, the 28 new cards were added to the explicit work selection and the
99-card index was regenerated. `make work-backlog-verify` passed with the documented
GraalVM installation on PATH, including its 14 maintained JVM test groups, native
work-tool build, deterministic index rebuild and planning link/dependency checks.
`bash scripts/check-work-yaml.sh` passed its independent YAML/schema checks for all
99 cards and the template. `git diff --check` passed. These results validate the
planning artifacts and existing tooling, not the proposed engineering capabilities.
