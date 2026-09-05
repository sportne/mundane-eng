# Work-item source, compilation and analysis 0.1

Status: Selected experimental contract. Implementation proceeds in TC-1603–1606.

Source: `mundane-work-source-0.1`; selection: `mundane-work-set-0.1`;
compiled: `mundane-work-items-0.1`; analysis: `mundane-work-analysis-0.1`.

## Authority and source

A task is planned work; an issue is a problem report. Each has a human-authored ID.
Completion/closure is an authored claim, not inferred requirement satisfaction.
Source cards are authoritative. Indices, inverse links and findings are derived.

Select Markdown with exactly one structured JSON metadata fence after the heading:

````text
# Task TC-EXAMPLE: Review the alarm

```json
{"format":"mundane-work-source-0.1","status":"Ready","dependencies":[],"relations":[],"planning":{"stage":"16","type":"Review","condition":"","unlocks":"","statusNote":""}}
```

## Question

Does the selected alarm requirement explain the intended behavior?
````

The outer text fence above is illustrative; the source file itself begins with the
heading and contains only the inner JSON fence. A [complete fixture](../examples/work-items/task.work.md)
provides exact bytes. The heading is `# Task ID: TITLE` or `# Issue ID: TITLE` and
owns kind/ID/title. ID uses `[A-Za-z0-9][A-Za-z0-9._-]*`. Title is nonempty, unpadded,
single-line Unicode. No repeated metadata heading copies are required.

Line 2 is blank; line 3 is exactly three backticks followed by `json`. A closing
three-backtick line terminates the metadata. The remaining Markdown is preserved
as opaque prose, including ordinary links and code fences; it must contain nonblank
content. Nothing in prose creates typed relationships, status changes or executable
instructions. The compiler does not render Markdown or evaluate source code.

Require UTF-8 without BOM, LF or CRLF termination, no bare CR, NUL or malformed
Unicode. Metadata is strict JSON: duplicate/unknown keys, invalid types, comments,
trailing commas and depth over 64 fail. Exact metadata keys are format, status,
dependencies, relations, planning. Planning has exactly stage, type, condition,
unlocks, statusNote; each is a string (possibly empty). These are opaque annotations,
not another dependency/status vocabulary. Migration preserves original prerequisite
qualifications in condition and exceptional completion wording in statusNote. Only
structured dependencies control graph analysis. Unlocks is historical explanatory
wording; inverse edges are derived from dependencies, never parsed from it.

Task statuses: Ready, Planned, Conditional, In progress, Complete, Superseded.
Issue statuses: Open, Closed, Superseded. Issues have no scheduling dependencies.
Dependencies are distinct local task IDs; order is immaterial. Source compilation
validates structure, IDs and duplicates; target resolution/cycles belong to analysis.
Complete compilation alone does not claim linked validity.

A relation has exactly `relation`, `scope`, `kind`, `target`. No implicit reference
inference, aliases or machine identity. Relation array order is immaterial; duplicate
four-tuples are invalid. Reserved primary scope is `work`; other scopes come from
explicit imports. Scope/target IDs use the ID pattern. Relation kinds:

| Relation | Allowed target kind | Meaning |
| --- | --- | --- |
| addresses / relates-to | work-item, requirement, verification-plan, verification-activity | Typed target existence only |
| supersedes | work-item | New work item supersedes the target; no automatic status update |
| evidence | resource | Root-relative local file citation; scope must be null; file existence and snapshot only, no content approval |

For example: `{"relation":"addresses","scope":"requirements","kind":"requirement","target":"SYS-001"}`.
A task may address an issue via scope work and kind work-item. A resource target is
a normalized root-relative path without empty/dot/parent segments, backslash or
control characters. No URI, fragment, remote fetch, code symbol or document section
resolution is selected. Ordinary Markdown links can retain those richer citations
without falsely advertising typed resolution. Resource paths in metadata use the
invocation root; links in preserved Markdown keep their normal file-relative meaning.

## Explicit selection and compilation

```json
{"format":"mundane-work-set-0.1","files":["roadmap/task-1603-compile-and-validate-work-items.md"]}
```

`mundane-work compile --root DIRECTORY MANIFEST` reads exactly the files in this
checked-in selection. No recursion, glob, extension inference, remote lookup or
parser fallback. All manifest file paths are root-relative under the same rules
as local imports; resolved symlink escapes fail. Duplicate paths/IDs are invalid.
Limits: 1 MiB per card, 16 MiB per JSON input, 10000 cards, 128 MiB aggregate snapshot
bytes, 1000 relations/dependencies per card. Compile output is bounded to 16 MiB;
exceeding it produces a small incomplete artifact, never a truncated complete one.
The compiler reads a selected snapshot once then rechecks bytes/available identity;
there remains a race after the recheck, not an atomic filesystem guarantee.

Output is deterministic compact sorted-key UTF-8 JSON plus LF:
`{artifactKind,format,sourceContract,compiler,complete,selection,sources,items,diagnostics}`.
artifactKind is work-items. compiler is `{name,version,contract}`. selection records
`{path,sha256}` for the manifest; sources is sorted `{path,sha256}` card snapshots.
Each item is `{values,location,metadataLocation}`. values contains exactly id, kind
(task/issue), title, status, dependencies, relations, planning, body. Dependencies
sort by ID; relations sort by serialized tuple; items sort by ID. Body preserves
all bytes after the metadata closing newline, decoded as Unicode, including original
line endings. Thus prose line-ending edits change its recorded value; IDs do not.
Locations are `{path,line,column}` with one-based code-point coordinates. Item
location is heading start; metadataLocation is line 4 column 1. Metadata diagnostics
and relationships use this honest declaration-level point, not fabricated value spans.

Errors are `{code,message,location}`; stable codes include invalid-work-source,
invalid-work-set, duplicate-work-id, invalid-work-artifact, work-output-limit,
plus existing input-unavailable/input-changed/invalid-json. Metadata type/shape errors
point to metadata start; malformed physical input points to line 1. Independent card
errors are accumulated (one per failed card); invalid compilation emits items=[]
with complete=false and diagnostics. Sources may describe successfully read snapshots.
Operational failures exit 2; invalid source/manifest exit 1; valid output exits 0.
Invocation failures (unknown/duplicate option, missing root/input, outside-root
command argument) use stderr, exit 2 and no artifact. Output failures override to 2;
a delivered prefix is unusable. Help/version are standalone text exceptions.

## Imports, analysis and findings

`mundane-work analyze --root DIRECTORY --imports MANIFEST COMPILED_WORK_ITEMS`
uses a separate, explicit `mundane-imports-0.1` manifest with the existing five fields
scope, path, kind, sha256, dependsOn. Empty imports are useful for a local backlog.
Support only requirements output 0.1, verification plan output 0.1 and work items
output 0.1. Existing requirement/plan validators guard those serialized boundaries;
work-item validation checks every required field/type/ID/status/location/digest,
not merely complete=true. Reject unknown kinds/formats, malformed/incomplete inputs,
duplicate scopes, reserved work scope, bad pins and cyclic/missing build dependencies.
Limit imports to 100, aggregate work items to 10000 and work relationships to
100000. Analysis output is bounded to 16 MiB; oversize output fails with
work-output-limit and no successful graph. No source parser is imported or invoked by the consumer.

Dependency targets are tasks in the same artifact. Missing/non-task targets or
cycles fail, including disconnected components. Validate every imported work-item
dependency graph too. Addresses/relates-to resolve scope, target kind and ID.
Supersession edges form a separate acyclic graph across work scopes; self edges fail.
A Superseded item needs an incoming supersedes edge among selected artifacts. It
still does not count as a completed prerequisite, and successors are not substituted.
All resource paths, including those in imported work items, resolve against the
explicit analysis root; callers must make that layout available, with no implicit
rebasing to another checkout. Resources use bounded local snapshots; missing/unreadable files or root escapes fail.
No future artifact kind is accepted without a defined validating adapter.

On success, output is
`{format,complete,analyzer,workArtifact,selection,imports,resources,edges,findings,diagnostics}`.
selection is `{path,sha256}` for the exact import declaration;
workArtifact is `{path,sha256,artifact}`; imports are scope-sorted
`{scope,path,sha256,artifact}` snapshots; resources are sorted `{path,sha256}`.
Edges contain `{from,relation,to,location}`; from/to are scope-qualified work/entity
keys (`scope:kind:ID`) or `resource:PATH`. Includes depends-on edges. Findings are
ID-sorted primary-work records `{id,status,unfinishedDependencies}`. A prerequisite
is finished only at status Complete. Report unfinished prerequisites even for a
card claiming Ready or Complete, but do not mutate authored status or automatically
infer conditional policy satisfaction. Findings are informational, not link errors.

Failures have complete=false, edges=[], findings=[] and diagnostics, never partial
successful navigation. Analysis exits 1 for semantic errors, 2 for operational/output
errors. Stable codes add missing-work-target, dependency-cycle, supersession-cycle,
missing-supersession, wrong-kind, unsupported-format, incomplete-import,
invalid-work-artifact, invalid-import, duplicate-scope, digest-mismatch,
missing-dependency and build-cycle. Locations point to the authored metadata for
relations and the declaration start for import failures. All reads are rechecked.

## Derived view and compatibility

`mundane-work view --root DIRECTORY ANALYSIS` consumes supported complete analysis,
validates its embedded artifacts and consistency of derived edges/findings, and
renders deterministic Markdown: primary status/index table, dependencies, typed
relationships, reverse navigation, source paths and imported/resource provenance.
It escapes authored text for Markdown/HTML safety; it does not render opaque body
Markdown as executable HTML. Links are root-relative paths encoded for Markdown;
consumers should place output at the invocation root or supply that root as link base.
Sources are not reread: output describes the compiled snapshots, not live file state.
Reject incomplete/unknown/tampered analysis before rendering. Exit 0/1/2 has the same
boundary; generated content is marked derived, never a second status authority.

Use independently declared current work source/output/analysis/CLI versions. No
existing requirement/plan/link interfaces or source selectors change. Unsupported
formats fail exactly, without numeric-prefix guesses. Prose/header migration is
explicit under TC-1606; historic IDs, titles, body bytes and qualifications survive.
Replace duplicate manual status rows with generated views, preserving authored
strategic roadmap narrative. No long-term freeze, tracker/server, attribute dependency,
remote collaboration or automatic approval is implied.
