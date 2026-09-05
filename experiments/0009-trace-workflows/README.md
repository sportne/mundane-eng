# Experiment 0009: Trace Graph and Workflow Verification

Status: Completed

Result date: 2026-08-29

Roadmap task: [TC-0503](../../roadmap/closed/task-0503-verify-trace-graph-behavior.yaml)

## Question

Are the four selected trace operations deterministic and useful across graph
edge cases, equivalent file layouts, and realistic requirement changes?

## Graph corpus

[`graph-one-file.mreq`](graph-one-file.mreq) and [`graph-split/`](graph-split/)
denote the same 14 requirements and 14 decomposition relationships with
different record, relationship, file, and input ordering. The graph includes:

- direct fan-in and fan-out;
- two equal-length paths whose whole-path lexical choice differs from reverse
  breadth-first first discovery;
- a disconnected requirement;
- a reachable three-requirement cycle;
- a reachable self-cycle;
- a separate unreachable two-requirement cycle; and
- two reachable cyclic components whose summaries require deterministic order.

The `expected/` files define complete output independently of Java traversal
implementation for all four operations, empty output, path ties, direct
self-relationships, and reachable cycle scope in both traversal directions.

## Automated protocol

`make trace-verify` builds the no-fallback native image and runs the maintained
verification harness. For each expected query it requires:

1. exact expected status, standard output, and standard error from the JVM;
2. byte-identical native process results;
3. equal output from the one-file and split layouts; and
4. equal output when explicit split files are supplied in opposite orders.

The harness separately checks missing IDs, invalid source, and all maintained
UAS and FRET trace results. No fixture, output, or index is generated during a
query.

## Workflow scenarios

The harness creates temporary before/after source snapshots and executes the
same maintained command for four change patterns from sustained authoring:

- **Addition:** adding `NEW` with `decomposes: PARENT` changes `children
  PARENT` from `(none)` to `NEW`.
- **Split and retarget:** inserting `NEW` between `LEAF` and `PARENT` changes
  the impact path from one direct edge to `LEAF -> NEW -> PARENT`, while `NEW`
  becomes the direct result.
- **Coordinated content change:** changing normative statements in two linked
  requirements without changing
  relationships leaves trace output byte-identical, demonstrating that trace
  is structural rather than a semantic impact oracle.
- **Retirement:** removing `LEAF` removes it from `impact PARENT`; successful
  source validation confirms no dangling relationship remains.

Every before/after result is compared between JVM and native execution.

## Manual comparison

For a direct incoming question, `rg '^decomposes: TOP$'` finds the two authored
edges but still requires opening records to recover `A-NEAR-TOP` and
`Z-NEAR-TOP`. Repeating searches from those IDs reconstructs the five-node
impact result. The trace command returns the same manually inspected edges in
one invocation, with distances and connecting paths.

For the sustained UAS corpus, manual exact searches reproduce six direct and
four second-level results for `impact OPS-001`. For the transferred FRET corpus,
source inspection confirms the expected empty result because it contains no
decomposition relationships.

The tool improves navigation by eliminating repeated reverse searches. It does
not determine whether a returned requirement must change, whether
decomposition is complete, or whether cycles violate project policy.

## Decision

The selected operations are deterministic across the tested graphs, file
layouts, input ordering, JVM/native boundary, and workflow changes. Cycles are
bounded structural observations rather than source errors. Advance exactly
these operations to a trial contract without adding stored indexes, generalized
relationships, coverage policy, or machine-readable output.
