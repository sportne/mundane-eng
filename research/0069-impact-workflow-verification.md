# Research 0069: Cross-artifact impact workflow verification

This dated record retains useful impact/performance evidence. Current source formats
and full-gate results are in [YAML authoring verification](0075-yaml-authoring-verification.md).


## Result and scope

TC-1701–1705 deliver the [impact contract](../specification/0023-cross-artifact-impact-0.1.md),
a parser-free scoped graph consumer, bounded explanatory queries, and a validated
Markdown report. The [logger workflow](../experiments/0037-impact-analysis/README.md)
connects checked-in requirements, a verification plan and work items without changing
their source languages, compiled formats, human IDs or authored status.

The logger example produces six review candidates for `req:requirement:SYS-001`:
DEV-001, ACT-STORAGE, PLAN-LOGGER, TASK-STORAGE, TASK-PROCEDURE and TASK-PLAN. The power
activity and context-only issue are excluded. Equal baseline/current bytes retain
separate scopes. A completed task remains Complete even when selected for review.

## Recorded verification

On Ubuntu 24.04.1 Linux x86-64, GraalVM CE 21.0.2, Javac 21.0.2 and Python 3.12.3:

```sh
export JAVA_HOME=/home/jack/.sdkman/candidates/java/21.0.2-graalce
export PATH="$JAVA_HOME/bin:$PATH"
scripts/run-ci-verification.sh
```

The wrapper completed with exit 0 against the implementation working checkout
based on TC-1704 commit `2b7529187b09abb87a406e98df019d41bbfa2514` plus TC-1705 changes.
This is local working-checkout evidence, not a hosted or independent clean-checkout
run. Disposable environment, gate, failure-injection and status logs are under
`build/ci-evidence/`. The initial targeted native command could not find
`native-image` on the shell's default PATH; using the documented GraalVM installation
resolved that setup issue. No verification target was skipped.

The authoritative gate passed all 24 maintained JVM groups, existing native
command/package and compatibility checks, and the new impact targets. New evidence:

- JVM/native query and report parity, deterministic shortest explanations, cycle
  termination, explicit depth truncation, unknown seeds and malformed inputs.
- Strict scoped import validation, exact pin rejection, detected read changes and
  suppression of incomplete query/report publication.
- Source-to-compiled-to-query-to-report goldens from actual compilers; YAML 0.3 and
  attribute-aware requirement output have the expected identical topology.
- Consumption with source files and source-parser classes absent, retaining strict
  attribute validation through the compiled boundary.
- Sixteen reproducible cyclic graphs (random seed 1705), checked against an independent
  fixed-point distance oracle, including shortest path length and record-order invariance.
- More than 10,000 nodes fail without partial results. Literal option-like filenames,
  real closed OS pipe, Java closed/prefix/flush stream errors and source links pass.
- Report checks reject forged nodes, edges, query results, numeric boolean tampering,
  unsupported formats and incomplete output; escaping and empty/bounded results pass.
- Four compiled mutations are detected: generic-link propagation, ignored depth,
  ignored import pin and bypassed report recomputation. Mutations use temporary
  classes and leave production files unchanged.
- Both CI injections fail at the expected authoritative targets (`test` for the
  invalid example and `yaml-schema-verify` for the altered schema), and both original
  inputs are restored byte-for-byte.

After recording this evidence and closing TC-1705, the backlog rebuild, migration
checks, source-card dependency analysis, documentation links/indexes and
`git diff --check` were rerun. No production change followed the full gate.

## Boundaries retained

The query is prospective reachability under six selected directional rules, not an
automatic source-diff classifier. It returns one shortest path per candidate, with
a configurable 1..64 depth bound and an explicit truncation flag. Generic relations,
supersession, evidence resources, narrative links and inferred attribute dependencies
do not propagate. Source roots are explicit and scope identity is never inferred
from path or digest equality.

Work relation links use metadata-block coordinates supplied by existing compiled
work output; requirements and plans retain more precise reference/row locations.
The report verifies internal consistency, not authenticity of embedded provenance.
Live source links may show newer bytes; the staged example keeps source snapshots
beside its disposable report. No field usability, verification execution, approval,
satisfaction, safety conclusion or maturity claim is established by these checks.
