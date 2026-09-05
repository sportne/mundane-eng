# Cross-artifact impact analysis 0.1

Status: Adopted experimental contract (TC-1701)

## Question and authority

Given a selected compiled snapshot and a scoped node, which selected requirements,
verification activities/plans and work items might need review if that node changes?
This is prospective reachability under the rules below, not a source diff or proof
of invalidity, execution, satisfaction or approval. Authored status never changes.
Existing artifact source languages and compiled contracts remain unchanged.

## Inputs and scope

`mundane-impact query --root DIRECTORY --from SCOPE:KIND:ID [--depth N] IMPORTS`
reads a `mundane-imports-0.1` manifest. It selects 1..100 imports of requirements
(output 0.1/0.2), verification-plan (0.1) or work-items (0.1/0.2), using existing
strict serialized validators. Each entry has `scope`, `path`, `kind`, nullable
`sha256` exact-byte pin and `dependsOn`. Paths are explicit root-relative local
files; CLI IMPORTS is relative to the invocation directory. No directory discovery,
source parser, schema rereading, network access or build execution occurs.

Scopes are unique and global within this selection, including references authored
inside imported work items and plans. Same IDs in different scopes are distinct.
Even equal bytes in two scopes remain separate nodes. Digests identify selected
revisions and do not replace human IDs. All artifact source locations are interpreted
relative to the declared root, qualified by scope; consumers never merge source
facts because paths or IDs happen to match. Import build dependencies must resolve
and be acyclic; they do not become impact edges.

A plan's baseline/current scope must name a requirement import. A null scope resolves
only when exactly one requirement import exists. Every coverage requirement must
exist in both resolved snapshots. Decomposition resolves within its requirement
scope. Work references retain their explicitly authored scopes. All selected
references are validated, including nontraversed relations; missing targets fail the
whole query. Work dependency/supersession cycles keep their existing validation
rules. Requirement decomposition cycles are accepted and bounded by visited nodes.

## Traversal policy

Edges below point from the possible change to the possible review consequence.
Their source locations cite the declaration that supplies the relationship.

| Declaration | Impact edge | Reason |
| --- | --- | --- |
| Child requirement decomposes parent | parent → child (`decomposes`) | A changed parent may require reviewing its refinement. |
| Coverage row in baseline scope | requirement → activity (`coverage-baseline`) | A referenced baseline revision affects review of that binding. |
| Coverage row in current scope | requirement → activity (`coverage-current`) | The selected current requirement is covered by that activity. |
| Coverage row's activity and plan | activity → plan (`activity-plan`) | Changes to an activity may require reviewing the plan that uses it. |
| Work item addresses typed target | target → work item (`addresses`) | Review the work explicitly addressing the changed target. |
| Task depends on local task | prerequisite → dependent (`depends-on`) | Review work relying on a changed prerequisite. |

Each coverage edge includes the owning plan node as `context`; other edges have
null context. A plan change does not propagate to all its activities in this first
policy: coverage is an authored binding, not an activity's dependency on every
plan using it. No reverse decomposition, inferred attribute edges, `relates-to`,
`supersedes`, evidence resources or narrative citations propagate. They retain their
own domain validity rules; resource paths are validated structurally but not opened.
Generic bidirectional traversal was rejected because an incidental link would spread
review across unrelated work. A new direction or relation requires a contract change.

## Worked decisions

For child `req:requirement:LOW` decomposing `req:requirement:TOP`, coverage of LOW
by `plan:verification-activity:TEST`, and task `work:work-item:FIX` addressing TEST,
changing TOP reaches LOW, TEST, its coverage plan, FIX and tasks depending on FIX.
Changing LOW does not reach TOP. A task merely relating to TOP is excluded. An
unreferenced `baseline:requirement:TOP` is independent of `req:requirement:TOP`.
A cycle TOP → LOW → TOP visits each node once and never reports the seed as affected.
Changing a plan reaches work addressing that plan but not other activities by inference.

## Graph and query output

`mundane-impact-0.1` JSON has exactly `format`, `complete`, `analyzer`, `selection`,
`imports`, `nodes`, `edges`, `query`, `diagnostics`. Analyzer has `name`, `version`,
`contract`. Selection has manifest `path` and exact `sha256`. Each import has
`scope`, `path`, exact `sha256`, `artifact` and `dependsOn`; embedded artifacts retain
all values, source locations, source hashes and attribute declarations.

Nodes have `key` (`scope:kind:id`), `scope`, `kind`, `id`, `location`. Kinds are
`requirement`, `verification-activity`, `verification-plan`, `work-item`. Locations
have separate `scope`, root-relative `path`, positive `line`, `column`. Edges have
`from`, `to`, `relation`, nullable `context`, `location`. Nodes/imports sort by key/
scope; edges sort by canonical JSON. Exact duplicate edges are collapsed.

A successful query has `from`, `depth`, `truncated`, `affected`. Each affected entry
has `node` and `path`, an array of complete edge objects from the seed. Breadth-first
search returns one shortest explanatory path per reachable node, breaking ties by
canonical edge order. Entries sort by node key. It never claims to enumerate all
paths. Default depth is 8, accepted range 1..64. `truncated` is true iff a visited
frontier has a successor not visited within the bound; cycles alone do not truncate.
An empty affected list means no review consequence within this selected graph and
policy, not absence of effects outside the selection.

Graph bounds are 10,000 nodes and 100,000 edges. Existing 16 MiB/file and 128 MiB
aggregate input bounds apply; JSON and rendered output are limited to 16 MiB.
Graph overflow fails rather than silently dropping relationships. Depth truncation
is a successful, explicitly bounded query. Source/schema changes have no special
implicit edges; a caller selects the requirement node to explore their consequences.

## Report and failures

`mundane-impact view --root DIRECTORY QUERY_JSON` renders deterministic Markdown
root-relative source links, the seed, affected nodes, every explanatory edge and
selected artifact revisions. It prominently labels derived output and any truncation.
Before rendering it validates embedded artifacts and recomputes the graph/query;
forged nodes, edges, paths or results fail. Provenance is a recorded claim, not a
signature or proof that an offline embedded artifact matches a currently available
file. The report does not reread authored source or refresh imports.

Queries snapshot/recheck all read files before publishing. Detected changes fail;
this is not a filesystem transaction. Success exits 0, invalid input/graph/seed/report
exits 1, invocation/read/change/output failure exits 2. Failed queries have
`complete:false`, empty nodes/edges/imports, null query and one diagnostic with
`code`, `message`, `location`. Selection is null if unavailable. Invocation errors
write stderr only; failed reports write no report. Closed streams, broken pipes and
partial output return 2; any emitted prefix is unusable. `--` ends options; standalone
`--help` and `--version` are supported.

Impact-owned rules are `invalid-impact-input`, `impact-limit`, `unknown-impact-node`,
`invalid-impact-analysis`; existing import/work rules keep their meanings, including
`duplicate-scope`, `digest-mismatch`, `missing-target`, `ambiguous-scope`, `missing-scope`,
`missing-dependency`, `build-cycle`, `wrong-kind`, `unsupported-format`, `invalid-json`,
`input-unavailable` and `input-changed`. Diagnostics from reused domain validators
retain their original locations. New graph locations carry separate scope fields.

## Compatibility and decision

This contract and its CLI/output version evolve independently, declared in
`versions.properties`. Existing requirement/plan/work consumers and outputs are
unchanged. Stop extension when it requires unowned assertions or implicit scope
mapping; require a specific workflow and revised contract first. Automatic change
classification, arbitrary graph-query languages, cross-root location remapping and
resource/code-symbol impact are deferred. TC-1702 may implement this bounded policy.
