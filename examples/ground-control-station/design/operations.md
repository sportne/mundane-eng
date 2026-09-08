# Release and operations decision

TC-2420 extends the GCS example through commissioning and recovery. Human facts use
one closed YAML operations contract with separate collections for candidate
selection, operational plans, execution records, compatibility decisions and
incidents. This bounded model is cohesive: each collection explains the history
and readiness of an exact candidate. Existing compiled assurance, configuration,
software, procedure/evidence and work-item owners retain their meanings. No new
build, SBOM, signature, issue-tracker or backup interchange format is introduced.
Native backup bytes are pinned opaque resources; their semantics remain external.

A candidate selects a baseline, software/build, assurance artifact, predecessor
and required commissioning plans. A plan records operation kind, selected procedure,
preconditions, expected outcome and responsible role. A plan alone proves no action.
An execution selects candidate/plan, exact observed configuration/build, time
interval, each precondition result, retained evidence, explicit timed actor actions,
observed outcome and separate authored acceptance decision. An execution history
is a single time-ordered chain; candidate predecessors are acyclic. Duplicate and
missing references reject input. Later records cannot erase a failed execution.

The native interaction will be compile/check/analyze/view/query. Analyze/view/query
require explicit evaluation time and caller-selected public JWKS for the existing
assurance model. Source views link candidate, plan, execution and incident records.
The compiled model remains independent of YAML and work-item source readers.
`localReadiness` requires exact software/build/configuration, current assurance,
required commissioning evidence, met preconditions and explicit accepted actions.
Observed drift, wrong build, failures, unknown/missing observations, stale reviews
and open incidents block readiness. Planned operations and future records do not
count as execution. `authorization` stays `none`: operator names and acceptance
are transparent assertions; authentic field logging and release approval are outside
this profile. A computed blocked report is a successful query, not exit failure.

## Tabletop sequence and decisions

1. RC-INITIAL records inhibited startup, actual combined-fault simulator evidence
   and separate operator acceptance. The default unwaived assurance obligation
   blocks release; a synthetic scoped waiver permits only local example readiness.
2. RC-UPGRADE records a failed upgrade after explicit preparation. Retain both its
   failure and the previous candidate. Rollback explicitly names RC-INITIAL and a
   from/to compatibility record with evidence; unknown compatibility blocks.
3. Restore names pinned backup bytes and explicit operator restore actions. Backup
   selection is not proof of successful restore: passing selected procedure evidence
   and accepted execution are required. A missing backup is never inferred.
4. Unexpected restart with queued commands selects the combined-fault procedure.
   Its existing executed simulator evidence shows the queue empty after restart and
   ambiguous acknowledgments quarantined. No actual aircraft commands are issued.
5. Server replacement creates RC-REPLACEMENT, retains the old candidate and records
   maintenance/replacement observations. Configuration drift blocks it. The fixture
   can demonstrate a like-for-like replacement under the same bounded baseline;
   actual changed hardware requires new configuration and assurance selections.
6. A field-incident-shaped synthetic anomaly on RC-REPLACEMENT points to an existing
   compiled work item and requirement. Closure names a descendant RC-CORRECTED,
   a replacement procedure run, successful execution, responsible actor and rationale.
   The work item's completed status and requirement relation are checked by its owner;
   original incident and failed candidate remain visible. Reusing the incident run
   as its replacement cannot close it. The new candidate must satisfy commissioning
   and assurance independently; incident closure alone does not establish readiness.

Startup, handover, backup/restore, upgrade/rollback, restart/recovery, replacement,
maintenance and retirement use the same representation. This is a worked local
record/query profile, not a universal operating manual. Retirement records block a
candidate from active readiness. Operational adequacy, backup compatibility, actual
physical replacement, authentic actor identity and organizational acceptance remain
explicit limitations. Native implementation must exercise the tabletop and failure
cases against real local simulator output before TC-2421 can close.
