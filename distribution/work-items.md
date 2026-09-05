# Work items: source, compilation and derived backlog

The migration verification needs the recorded ancestor commit, so use a full Git
checkout for `make verify`; hosted checkout fetches history for that replay.

Build `make native-work`. The standalone `build/maintained/mundane-work` executable
supports compile, analyze and view; `--help` and `--version` describe its interface.
Java 21/GraalVM and the existing native toolchain assumptions apply. The work-item
runtime uses no YAML parser, requirement source parser, database or external tracker.

A card owns ID/kind/title in its Markdown heading, structured facts in one strict
JSON block, and ordinary Markdown narrative below. See the
[task template](../roadmap/task-card-template.md),
[task/issue examples](../examples/work-items/task.work.md), and
[contract](../specification/0018-work-items-0.1.md).
Requirements YAML and plan TSV retain their own formats. A valid metadata block
establishes structure, not the truth of an issue, completion or evidence claim.

From the repository root:

```sh
build/maintained/mundane-work compile --root . roadmap/work-items.json > build/work-items.json
build/maintained/mundane-work analyze --root . --imports roadmap/work-imports.json build/work-items.json > build/work-analysis.json
build/maintained/mundane-work view --root . build/work-analysis.json > build/work-view.md
```

Check each exit status before consuming output. Compilation/analysis errors publish
an incomplete JSON artifact without usable records/edges; view errors publish no
Markdown. Invocation/input/output failures return 2; invalid source/link/view returns
1; success returns 0. Capturing a prefix from a failed output stream is not success.
The view's links use the invocation root as base; move the example view to the root
for ordinary file-relative Markdown navigation. The repository helper does this.

## Maintaining this repository's backlog

- Edit source cards. JSON status and dependencies are the authoritative facts.
  `planning.condition`, unlocks and statusNote preserve explanatory/historical
  qualifications; they are not parsed as executable rules or inverse links.
- Use `task-NNNN-description.md` for tasks or `issue-description.md` for issues
  under roadmap/ or roadmap/closed/. Add a card's root-relative path to `roadmap/work-items.json`. That manifest selects
  inputs only. Add supported typed links explicitly; ordinary Markdown links remain
  useful citations without claiming artifact resolution.
- On completion, record evidence, set Complete for a task or Closed for an issue,
  move the source under closed/,
  update the selected path and rebase incoming/outgoing Markdown links. Preserve ID.
- Run `make work-index` to regenerate root `WORK-ITEMS.md`. With an already built
  tool, `python3 scripts/work-backlog.py --write` performs only the public pipeline.
- Run `make work-backlog-verify` to check migration replay, complete card selection,
  identical source-to-view rebuilds and relative documentation links. `make verify`
  includes these plus the compiler/link/view regressions.
- Commit source and generated index together; do not update status rows by hand.
  The strategic roadmap and planning narrative remain authored documents.

Open/Closed are issue statuses. Tasks use Ready, Planned, Conditional, In progress,
Complete or Superseded. Analysis reports unfinished task prerequisites without
rewriting status or evaluating conditional policy. A task can address an issue or
requirement; completing it closes neither automatically. Superseded prerequisites
remain unfinished until an author changes the dependency.

## Migration and limits

[Migration evidence](../experiments/0034-work-items/migration.json) names the immutable
input commit and each of 78 converted cards. Headings and narrative bytes were
preserved at conversion. Legacy stage/type/prerequisite/unlocks fields and exceptional
completion/disposition wording were moved explicitly into JSON metadata. Four new
execution cards gained reviewed local-resource evidence links. No relationship was
inferred from body Markdown. The migration checkpoint is historical evidence;
source cards can subsequently evolve, with completion records and link rebasing.

Only supported compiled work items, requirements and verification plans are imported.
Resource evidence is a local file snapshot, not a parsed code symbol or validated
claim. Source roots of imported work artifacts are not guessed; resource citations
resolve under the explicit analysis root. Remote URIs, code-symbol adapters, external
issue synchronization, customizable lifecycle rules and automatic approval are outside
this contract. Unknown formats/kinds are rejected rather than silently flattened.
