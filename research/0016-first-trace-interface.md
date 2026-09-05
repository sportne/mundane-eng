# Research 0016: First Trace Interface

Status: Decided

Decision date: 2026-08-29

Roadmap task: [TC-0501](../roadmap/closed/task-0501-define-the-first-trace-interface.yaml)

## Question

Which small set of decomposition questions materially improves navigation and
change-impact work without introducing another relationship model, stored
reverse links, or a generalized graph language?

## Evidence

The sustained-authoring trial repeatedly required exact searches for
`decomposes: ID`, followed by opening each matching record and repeating the
search at the next level. This occurred when tightening a timing requirement,
splitting and retargeting behavior, checking the effect of retirement, and
confirming that newly added requirements had no lower-level decomposition.

The source-experiment plan's recorded “follow rationale and decomposition”
workflow asks an engineer to determine which higher-level requirement or
source justifies a selected lower-level requirement. An immediate parent is
visible in one record, but following a leaf such as `VM-002` through system to
operational requirements requires repeated record navigation. That is the
recorded workflow for transitive `higher`; it is not inferred from the incoming
query experiment alone.

[Experiment 0005](../experiments/0005-incoming-trace-query/README.md) showed
that an incoming transitive query with one shortest path materially reduced
that work over the UAS corpus. It also showed that a process-local reverse
index was sufficient. The experiment did not justify arbitrary relationship
queries, coverage policy, persistent indexes, or graph visualization.

## Relationship language

For a source relationship `LOWER decomposes HIGHER`:

- `HIGHER` is a directly higher-level requirement for `LOWER`; and
- `LOWER` is a directly lower-level requirement for `HIGHER`.

These terms describe navigation along the existing `decomposes` relationship.
They do not claim that one lower-level requirement completely satisfies its
higher-level requirement, or that every requirement must have links in either
direction.

## Selected invocations

The first trace interface has four operations:

    mundanereq-trace parents ID FILE_OR_DIRECTORY...
    mundanereq-trace children ID FILE_OR_DIRECTORY...
    mundanereq-trace higher ID FILE_OR_DIRECTORY...
    mundanereq-trace impact ID FILE_OR_DIRECTORY...

The names are concise command vocabulary. Their domain questions, rather than
the names alone, define their meaning.

### `parents`

Question: Which requirements are directly higher-level than `ID` because `ID`
has a `decomposes` relationship to them?

This answers the immediate source-navigation question visible in one
requirement record and provides a symmetric command interface for automation
and independent tools. Results are requirement identifiers in lexical order.

### `children`

Question: Which requirements are directly lower-level than `ID` because they
have a `decomposes` relationship to `ID`?

This replaces repeated exact reverse searches when checking immediate
decomposition, retargeting, or loss of lower-level trace. Incoming links remain
derived; no reverse field is added to source. Results are identifiers in
lexical order.

### `higher`

Question: Which distinct higher-level requirements can be reached by following
one or more `decomposes` relationships from `ID`, and what is one shortest path
to each?

This exposes the higher-level context of a lower-level requirement during
review and coordinated change. Each result contains distance and a path that
begins at `ID` and ends at the reported higher-level requirement.

### `impact`

Question: Which distinct lower-level requirements have a path of one or more
`decomposes` relationships ending at `ID`, and what is one shortest path from
each to `ID`?

This is the Experiment 0005 workflow: navigate the lower-level requirements
that may need inspection when `ID` changes. “Impact” is intentionally a
navigation result, not an assertion that every returned requirement must
change or that unreturned artifacts cannot be affected.

## Output semantics

Every successful output is written only to standard output and begins with the
exact header for its operation:

- `parents`: `Direct higher-level requirements for ID:`
- `children`: `Direct lower-level requirements for ID:`
- `higher`: `Higher-level decomposition paths from ID:`
- `impact`: `Lower-level impact paths to ID:`

`ID` is replaced by the query identifier. Direct operations then emit one
identifier per line. Transitive operations emit one line per result:

    DISTANCE ID -> ... -> ID

If there is no result, the output emits `(none)` after the header. The queried
requirement itself is excluded from transitive results, even when a cycle can
reach it again. Direct self-relationships remain visible in `parents` and
`children` because they are authored edges.

For each transitive result:

1. distance is the number of decomposition relationships in the path;
2. the path is simple and contains no repeated identifier;
3. only a shortest path is emitted; and
4. if several shortest paths exist, the lexicographically least complete
   identifier sequence is selected.

Transitive result lines are ordered first by distance and then by the reported
requirement identifier. “Lexical” and “lexicographical” comparison throughout
this interface mean ordinal comparison of the allowed ASCII identifier
characters by code-point value. Identifier sequences are compared element by
element; if one is a prefix, the shorter sequence sorts first. These rules are
properties of output, not a required traversal algorithm.

## Cycles

Cycles are valid source-language graphs. They do not make the trace invocation
fail and traversal shall terminate.

After transitive result lines, or after `(none)`, the tool writes each cyclic
strongly connected component to standard output in the operation's reachable
scope:

    Cycle observed among: ID [ID...]

A component is cyclic when it contains more than one requirement or contains
one requirement with a self-relationship. Identifiers within a component are
lexically ordered; components are ordered by their first identifier. The scope
contains the queried requirement and requirements reachable in the operation's
direction. The observation is structural only. Whether cycles violate project
policy belongs to a separate validator or linter and does not change exit
status.

Direct operations do not add component summaries; their complete edge results
already expose direct self-relationships and each authored edge participating
at the query boundary.

## Source and error behavior

The explicit inputs select one complete source set using the 0.2 selection
rules. Invocation and query errors have this precedence:

1. an unknown operation, omitted query ID, or omitted source input is an
   invocation error and prints usage;
2. a query ID that does not match the source-language identifier grammar is an
   `invalid-query-id` invocation diagnostic;
3. after a well-formed invocation, source selection and complete source-set
   validation occur;
4. only after valid source exists does lookup of the well-formed ID occur; an
   absent ID is a `missing-requirement` query diagnostic.

Thus malformed invocation is rejected before reading source, while a
well-formed but absent ID never hides source diagnostics.

- invalid or unavailable source produces shared diagnostics and no trace
  output;
- a well-formed query identifier absent from valid selected source produces a
  `missing-requirement` diagnostic and no trace output;
- unknown operations, malformed invocation, and missing inputs produce usage
  or invocation diagnostics; and
- valid graphs with no result produce successful `(none)` output.

The development interface uses status `0` for a completed query and status `2`
when invocation, input, source-set validation, query-ID lookup, or output
failure prevents a result. There is no status for coverage, completeness, or
cycle policy.

## Deterministic examples

For authored edges `C -> A`, `C -> B`, `A -> TOP`, and `B -> TOP`, where the
arrow means `decomposes`, `higher C` returns `A` and `B` at distance one and
`TOP` at distance two. The path to `TOP` is `C -> A -> TOP`, because it is the
lexicographically least of the two shortest paths.

For `impact TOP`, the corresponding path for `C` is `C -> A -> TOP`. Results
remain ordered by distance and reported ID, not by file or traversal order.

A deeper tie distinguishes complete-path ordering from first discovery in a
reverse breadth-first traversal. Given these two equal paths:

```text
D -> Z-NEAR-D -> A-NEAR-TOP -> TOP
D -> A-NEAR-D -> Z-NEAR-TOP -> TOP
```

the selected `impact TOP` path for `D` is:

```text
D -> A-NEAR-D -> Z-NEAR-TOP -> TOP
```

Although `A-NEAR-TOP` is the lexically earlier neighbor when traversing
backward from `TOP`, the selected displayed sequence is the lexicographically
least complete child-to-parent path. Porting Experiment 0005's first-discovery
implementation without this distinction would be nonconforming.

## Rejected alternatives

- **Every path:** path counts can grow combinatorially and Experiment 0005
  found one shortest path sufficient for navigation.
- **IDs without transitive paths:** this hides the requirements connecting a
  lower-level result to the changed requirement.
- **One operation with direction/depth flags:** fewer command names would
  require more interacting concepts and make the engineering question less
  visible.
- **Generic edge types or query expressions:** the language has only
  `decomposes`; generalization has no current workflow evidence.
- **Coverage or orphan failures:** absence of incoming or outgoing
  decomposition may be intentional and is not source invalidity.
- **Persistent reverse indexes:** the maintained corpora and Experiment 0005
  require no state beyond one process invocation.

## Decision

Implement only `parents`, `children`, `higher`, and `impact` over
`decomposes`. Build outgoing and incoming indexes from valid authoritative
source on each invocation, emit deterministic human-readable results, report
cycles as non-fatal structural observations, and retain no derived state.

The interface is a development decision until implementation and workflow
verification support a separately published trial contract.
