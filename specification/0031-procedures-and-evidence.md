# Procedures and evidence 0.1

The [procedure schema](schema/procedure-yaml-0.1.json), [run adapter schema](schema/run-0.1.json),
[manual source schema](schema/manual-observation-yaml-0.1.json) and
[assessment schema](schema/assessment-yaml-0.1.json) publish the
[accepted design](../examples/ground-control-station/design/procedure.md).
Source/compiled/tool identifiers are declared independently in `versions.properties`.

## Procedures and selected subjects

A procedure selects compiled requirements, an existing plan activity, a baseline
and explicit telemetry, command and heartbeat interfaces. Its requirement and
architecture revisions must also be members of the selected baseline. The
configuration environment must match the procedure's simulation environment.
All references are typed and pinned using the domain import contract. Compilation
uses the standard closed domain envelope, source locations and numeric YAML profile.

Events are ordered by atMs (equal timestamps retain source order) and bounded to
1,000 stimulus events and 10,000,000 ms. Runs are bounded to 10,000 observations. Method simulation requires events; manual-inspection
forbids events. Criteria have unique IDs, afterMs, withinMs, a known observed field
and a correctly typed expected value. Clock kind/unit/uncertainty are explicit.

The in-memory simulator consumes event stimuli and the selected interface policies,
never expected values. It models freshness with age/uncertainty, independent heartbeat
health, unknown state after link loss/restart, command acknowledgement versus timeout,
late-ack quarantine, session/queue clearing and combined mains/link loss. Backup power
is an assumption; aircraft-local response is reported as unspecified. No physical
power model, radio/socket transport, aircraft dynamics, flight command or execution
completion is implemented. Existing pending intent is not automatically retried or
replaced by a second request. The suppress-stale fault model deliberately corrupts
displayed state to exercise negative verification while retaining observed provenance.

## Native runs and manual normalization

A run records the exact raw compiled procedure hash and all its import scope/hash
pairs, clock/environment, execution state, observations, limitations and adapter
provenance. The first adapter version is 0.1. BuildSha256 fingerprints the maintained
Java sources, schemas and version declarations; runtimeSha256 selects an actual
native executable resource beneath the root. Simulation and manual conversion check
that this resource matches the executing native tool. The compiled consumer rechecks
that resource and accepts only its known adapter build. These are provenance checks,
not signatures or proof that a claimed execution occurred.

Manual YAML supplies the selected procedure/subjects, synthetic observations and
execution context. The converter supplies its own runtime/build metadata and retains
the manual source path/hash in the native JSON run. Users do not author converter
metadata. Consumers check the original manual bytes without loading a YAML parser.
The first manual adapter is explicitly synthetic-manual-inspection.

`mundane-evidence-0.1` uses the common envelope with values containing exactly run
and resource (the raw run path/hash). It records the native raw source inventory and
the exact procedure import. Adapter-generated value locations identify the raw
resource as a whole at line/column 1; they are not field-level manual-source marks.
The consumer validates the native schema and raw bytes, source inventory, method,
subject/procedure/build/runtime pins and observation order, then recomputes outcomes.
No stored verdict field can override the evidence evaluation. Native executable
resources are bounded to 64 MiB; other snapshot and aggregate limits remain in force.

## Interpretation, repeat runs and assessments

A completed run passes only if every criterion has a matching observation inside
its window, including clock uncertainty at the deadline. Available observations with
no matching value fail the criterion. Missing observations and incompatible clock
provenance are inconclusive. Skipped/interrupted execution remains separate. The
simulator and evaluator are tested independently, including changed expectations
and a real injected simulator defect.

Analyze targets an exact compiled procedure revision. An old run cannot support a
new procedure with the same ID. Distinct raw runs remain distinct; duplicate raw pins
are counted once. Any nonpassing run prevents observedSupport. Pass/fail disagreement
sets conflictingRuns. Missing/tampered raw resources, unknown builds and mismatched
subject pins fail validation rather than publishing a successful analysis.

Assessments are separately compiled YAML claims against raw run ID/hash, with reviewer,
role, reason and adequate/insufficient/disputed disposition. Matching versus stale or
unavailable assessment subjects is explicit. ObservedSupport is a bounded criterion
observation result; adequacyEstablished remains false and authorization remains none.
It does not mean requirement satisfaction, accepted residual risk or trusted approval.

All tools retain exact input snapshot/output-failure behavior. There is no automatic
raw-log deletion, pin refresh, deployment or artifact upload. Package/editor expansion,
trusted review authority and cross-domain semantic invalidation remain later work.
