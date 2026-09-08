# Release and operational traceability 0.1

`operations-model` owns the [closed YAML schema](schema/operations-yaml-0.1.json)
and `mundane-operations-0.1` compiled domain contract; `operations` owns YAML and
CLI wiring. Existing domain versions remain unchanged. The
[GCS decision](../examples/ground-control-station/design/operations.md) describes
the six-step tabletop and limitations. Native build, provenance, signatures,
backup bytes and compiled work items retain their existing owners.

Collections are bounded to 1,000 records, with unique local IDs. Candidates select
an exact baseline, software scope/build digest, assurance scope, predecessor and
required commissioning plans. Candidate predecessors are acyclic, at most 64 links.
Assurance and software must select the same exact configuration artifact. Plans
select a procedure, explicit preconditions, expected outcome and responsible role.
Procedure configuration must match each execution's selected candidate.

Executions form one ordered chain through `previous`; start/completion times cannot
overlap their predecessor, actions are ordered within the interval and acceptance
cannot precede completion. Each records observed configuration/build digests,
precondition results, evidence scopes, actions, outcome and separate acceptance.
Unknown preconditions, absent actions, missing/nonpassing/stale evidence, wrong
observed build, drift, non-success outcome, or pending/rejected/wrong-role/future
acceptance block that execution. Backup/restore additionally require pinned native
backup bytes. Rollback requires an explicit target and compatible, evidenced exact
from/to records; any conflicting/unknown record blocks. The tool does not infer
native backup semantics or compatibility from a filename.

The caller supplies explicit ISO evaluation time and public JWKS to the existing
assurance adapter. Future executions do not satisfy commissioning. Each required
plan needs an observed ready execution; any retained observed unready execution
blocks that candidate. Retirement blocks active readiness. Selected software build
must match the candidate digest; assurance must independently report local readiness.
A failed predecessor does not permanently poison all descendants: a new candidate
must establish its own readiness. Failed history remains in the report.

Incidents select candidate/execution, requirement and a SHA-pinned existing compiled
work artifact/item. WorkArtifact validates that artifact; its item must explicitly
address the incident's requirement/scope. Closure additionally needs completed work,
a descendant candidate, a ready replacement execution, a distinct passing raw run
used by that execution, actor/time and rationale. Reusing the incident's native run
cannot establish replacement evidence. Open or invalidly closed incidents block
affected candidates and descendants. Closure does not erase any original failure or
grant the replacement candidate readiness without its independent checks.

Views link source records only when their source revision is available. Compiled
checks/queries need no YAML or work source parser. Repeated imports share captured
resource bytes per invocation, count once against the 128 MiB aggregate bound and
still enforce each reader's size limit and final mutation detection. Distinct native
resources remain bounded; no network, private-key discovery or deployment occurs.

Operator IDs and operational acceptance are authored assertions, not authenticated
field telemetry. The standard-backed assurance signature checks remain separately
scoped to caller-selected keys. `authorization` is always `none`; even a local ready
result needs external engineering, security and organizational acceptance. The
example models like-for-like replacement under one synthetic baseline; changed real
hardware requires new configuration/assurance inputs. Its combined-fault simulator
supports only its selected criteria, not real backup/upgrade or field repair claims.
