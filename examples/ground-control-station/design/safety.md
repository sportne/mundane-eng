# Safety analysis representation and interaction — TC-2406

Accepted bounded design for the simulation-only reference project. The
[draft YAML schema](safety.schema.json), [worked source](safety.yaml) and
[failure ledger](safety-cases.yaml) separate hazard, cause, control, failure mode,
verification obligation and residual-risk review. These are engineering records;
structural traceability does not accept residual risk.

## Worked scope and ownership

HZ-STALE concerns trusting stale telemetry despite a live connection. CTRL-FRESH
allocates freshness/time controls to GCS-STALE and GCS-CLOCK and the existing
ACT-STATE verification activity. HZ-COMMAND concerns ambiguous intent or outcome
while mains and link are lost; CTRL-ACK and CTRL-POWER allocate obligations to
existing requirements and ACT-STATE/ACT-POWER. Actual executions remain separate.

FM-HOST-POWER records the selected server's function, failure mode, causes, effects,
detection and mitigations. Its effects include both hazards. CAUSE-POWER explicitly
belongs to POWER-DOMAIN: the host and network's shared power is a common cause,
not independent events to multiply. The qualitative fault tree connects loss of
trustworthy supervision to delayed state, shared power loss and ambiguous acknowledgement.
AND/OR gates compose events; basic events reference causes. Cycles, unreachable
nodes, malformed gates and broken causes are rejected. No probabilities or failure
rates are admitted by this first contract.

The qualitative severity scale is project-owned and defined in the source. It is
not a standardized risk matrix. Likelihood remains unquantified; there is no implicit
likelihood score or risk acceptance calculation. The simulation assumption remains
unverified, and both residual-risk records retain an owner, reason and unresolved
or review-required state. Trusted acceptance and assurance reviews belong to TC-2418.

## Identity, configuration and changes

Safety owns hazards, causes, controls, assumptions, FMEA records and fault-tree
events. It selects compiled architecture, configuration, requirements and plans;
references use scope/kind/id. Local records use self. Configuration applicability
is pinned through the selected baseline. ReviewedAgainst separately records the
revision actually considered by a review; changing the selected architecture pin
leaves the old review pin intact and produces review-stale. This is a conservative
revision check, not TC-2423's future semantic invalidation policy.

A missing control reference is an invalid model. An empty hazard control set is an
explicit coverage gap. Orphan controls, unverified assumptions and unresolved risk
remain findings in analysis; a compiler's complete flag means a valid model, not
an acceptable safety case. Procedures, results and approval identities are not
embedded into hazard records.

## Interaction and implementation boundary

The engineer authors YAML, compiles and resolves selected revisions, then queries
hazard -> control -> requirement -> planned activity and inspects FMEA/fault-tree
views. Editing a selection requires a new pin; it must not silently refresh the
review record. Derived diagrams and tables never become competing authoring sources.

TC-2407 will publish safety-yaml-0.1 / safety-0.1 and a standalone safety CLI, with
separate source and compiled model components. It should reuse structural/provenance
infrastructure and domain adapters, with no source parser in compiled consumers.
Existing requirement/plan formats remain unchanged. Editor/distribution expansion
and actual risk acceptance remain separately owned.

## Acceptance

Run `build/schema-check-venv/bin/python examples/ground-control-station/design/check-safety.py`
after the configuration and GCS seed workflows. The independent checker resolves
real imported IDs and hashes, validates the closed schema, and exercises ten cases:
missing/broken controls, broken cause, unsupported probability, changed review
revision, wrong kind, cycle, missing baseline, forbidden accepted-risk claim and
visible unresolved risk. Evidence is rebuilt under `build/gcs-design/`.
