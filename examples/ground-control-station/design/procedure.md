# Procedures, execution and evidence design — TC-2410

The [procedure schema](../../../specification/schema/procedure-yaml-0.1.json), [native run schema](../../../specification/schema/run-0.1.json),
[manual observation schema](../../../specification/schema/manual-observation-yaml-0.1.json) and
[assessment schema](../../../specification/schema/assessment-yaml-0.1.json) define independent ownership boundaries.
The worked procedures are [stale telemetry](procedure-stale.yaml),
[combined mains/link loss](procedure-combined.yaml), and
[synthetic manual inspection](procedure-inspection.yaml).

## Facts and provenance

Existing plans own verification activities and requirement coverage. A procedure
owns an objective, method, selected baseline, requirement/activity/interface references,
clock model, ordered stimulus and expected observations. The first contract uses
simulation, a synthetic monotonic millisecond clock, explicit uncertainty, and bounded
integer event times. Each criterion names an ID, time window, observed field and value.

A native execution record owns its ID, exact compiled procedure hash, complete selected
subject pins, environment/clock, adapter name/version/build/runtime digests, execution
state, raw observations and limitations. Native JSON logs remain native resources;
a manual observation is authored in YAML and normalized by an explicit adapter.
Neither is embedded into the requirement statement or plan's coverage assertion.
Missing, altered or stale raw resources cannot support the selected revision.

A separate assessment owns reviewer and role claims, a reason, disposition
(adequate/insufficient/disputed) and the exact raw run ID/hash. It does not overwrite
the observations. An editable reviewer name is not trusted identity or approval.
The future assurance domain owns authorization and adequacy policy. The evidence
consumer must expose absent or disputed assessments, not infer them from a passing run.

## Worked timing and result rules

The stale procedure receives fresh telemetry and a heartbeat at time zero. At 480 ms,
a sample with 20 ms uncertainty reaches the 500 ms freshness bound despite the live
connection. Within 100 ms the observed state must be stale; a subsequent command
must be inhibited. The combined procedure sends intent, loses mains and then link,
observes command timeout and quarantines a late acknowledgement before a new session
clears queued intent. It separately reports the onboard response as unspecified
and aircraft-local. Acknowledgement never proves actual execution.

Observations satisfy a criterion only within its declared time window, including
the run clock's uncertainty at the deadline. A matching value is a pass for that
criterion; available but mismatching values fail it. Missing observations or incompatible
clock provenance are inconclusive. Execution skipped/interrupted stays distinct.
An overall pass requires all criteria. Conflicting repeated pass/fail runs remain
conflicting; neither last-writer wins nor “any pass” establishes current support.
Raw missing/tampered resources, wrong procedure/build/subject revisions and unknown
adapter versions cannot be treated as current evidence.

The manual inspection procedure's result is explicitly synthetic. It illustrates
capturing an observation and its provenance without claiming an actual installed
wiring assembly or replacing the later equipment/connectivity model.

## Interaction and implementation boundary

Author YAML and compile a procedure against exact imported revisions. Run a bounded
in-memory event simulator, or import an explicitly selected native/manual observation.
Preserve raw bytes and their hashes; recompute criterion outcomes when inspecting
compiled evidence. Query support for the exact procedure/requirement/configuration
selection, displaying missing evidence, conflicting runs and assessments separately.
Compile a new assessment against an exact raw run; it cannot retroactively edit it.

TC-2411 publishes procedure-yaml/procedure 0.1, native run adapter 0.1, evidence 0.1
and assessment source/compiled 0.1 through independently versioned procedure/evidence
commands. It separates source parsing from compiled evidence consumers. It must
record the real simulator implementation/runtime revision and exercise a deliberate
simulator defect to demonstrate that expected values are not echoed as observations.
There is no socket/radio backend, external test-runner replacement, flight execution,
large-log YAML authoring or automatic engineering approval.

## Acceptance

`build/schema-check-venv/bin/python examples/ground-control-station/design/check-procedure.py`
validates the three worked procedures and four closed schemas, then exercises nine
interpretation cases: pass, fail, missing observation, skipped, interrupted, uncertain
clock, late observation, conflicting repeated results and synthetic inspection.
These are design observations, not recorded executions. Actual adapters and tamper/
revision/publication tests belong to TC-2411.

## Maintained implementation

TC-2411 implements the [published contract](../../../specification/0031-procedures-and-evidence.md)
and [native workflow](../../../distribution/evidence.md). Implementation adds an
explicit heartbeat interface, selected runtime resource, adapter-generated manual
provenance and a declared simulator fault model. These close concrete ambiguities in
the draft without changing existing requirement or plan formats. The published schemas
replace draft copies; independent design interpretation checks remain regression inputs.
