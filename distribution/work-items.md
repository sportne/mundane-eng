# Work items: YAML source, compilation and derived backlog

Build `make native-work`. The standalone `build/maintained/mundane-work` executable
supports compile, analyze and view. Java 21/GraalVM and the existing native toolchain
assumptions apply. Work-item compilation shares the pinned YAML parsing library,
while its domain model is independent of the requirements parser. Serialized
analysis/view do not need either source parser. No database or external tracker.

A card is one YAML mapping. ID, kind, title, status, dependencies and typed relations
are structured fields; `body` is one opaque string, normally a literal block with
Markdown prose. Use [the template](../roadmap/task-card-template.yaml),
[YAML examples](../examples/work-items/yaml/task.yaml),
[specification](../specification/0019-work-items-yaml-0.2.md) and its
[structural schema](../specification/schema/work-items-yaml-0.2.json).
Requirements and plans also use YAML, with independently owned fields and semantics.

```yaml
format: mundane-work-yaml-0.2
id: TC-EXAMPLE
kind: task
title: Review the alarm
status: Ready
body: |-
  ## Question

  Explain this flow:
      Input -> Compiler -> Artifact
```

`|-` strips the final newline, `|` retains one, and `|+` preserves trailing blank
lines. All preserve internal newlines and relative indentation; YAML removes common
indentation and normalizes physical line endings. Quoted escapes can retain exact
CRLF string values when needed. The compiler preserves decoded strings without
trimming or interpreting their Markdown. YAML comments are presentation, outside
compiled body text. This change does not introduce a YAML formatter.

Optional dependencies/relations default to empty lists; optional planning annotations
default to empty strings. Status/ID/title/kind/body remain explicit. Quote text that
YAML Core would resolve as a number, boolean or null. Unknown/duplicate fields,
tags/anchors/merges and multiple documents fail. Generic schema validation checks
structure; compilation checks the source profile and domain values; analysis checks
links and cycles. None establishes the truth of completion or evidence claims.

## Commands and current authoring

From the repository root, check each exit status before using the next output:

```sh
build/maintained/mundane-work compile --root . roadmap/work-items.json > build/work-items.json
build/maintained/mundane-work analyze --root . --imports roadmap/work-imports.json build/work-items.json > build/work-analysis.json
build/maintained/mundane-work view --root . build/work-analysis.json > WORK-ITEMS.md
```

The root-relative view is derived. The helper `python3 scripts/work-backlog.py --write`
checks every pipeline step and replaces the index only after successful rendering.
Incomplete JSON output and failed-stream prefixes are unusable. Exit 0 means success,
1 means invalid source/link/view, and 2 means invocation/input/output failure.

- Use `task-NNNN-description.yaml` or `issue-description.yaml` under roadmap/ or
  roadmap/closed/. Select each explicitly in roadmap/work-items.json, whose format
  is mundane-work-set-0.2 and source is mundane-work-yaml-0.2. It owns selection only.
- Edit YAML facts and narrative in the same source. planning.condition, unlocks and
  statusNote retain opaque qualifications, not executable policy. Put mechanized
  dependencies/relationships in their typed fields; prose citations remain prose.
- On completion, record evidence in body, set Complete for tasks or Closed for issues,
  move to closed/, update the selected path and rebase incoming/outgoing prose links.
  Preserve the ID. A relation never automatically closes or approves another item.
- Run `make work-index`, `make work-backlog-verify`, and owning implementation checks.
  `make verify` includes YAML/schema, corpus and supported-profile checks.
- Commit each completed task separately, including source and generated index.

Tasks use Ready, Planned, Conditional, In progress, Complete or Superseded. Issues
use Open, Closed or Superseded. Analysis reports unfinished prerequisites without
changing authored status or inferring that conditional policy is satisfied.

## Migration and compatibility

Manifest 0.1 still explicitly selects the old Markdown profile; manifest 0.2 selects
YAML. There is no extension inference or fallback. Compiled output 0.1 and 0.2 remain
readable; YAML 0.2 adds real declaration coordinates while retaining semantic values.
Current analysis remains 0.1. New compiled artifacts, locations and exact-revision
hashes differ after migration, so rebuild derived imports/pins/views as appropriate.
The work-item ID remains its identity. Source digests are revision provenance only.

Supported typed targets remain work items, requirements and verification plans/
activities. Evidence resources are local file snapshots, not validated code symbols
or approved claims. Source roots are explicit. Remote synchronization, custom
lifecycles and other artifact source formats need their own justified decisions.

## VS Code authoring

The [YAML editor](../editors/vscode/README.md#yaml-task-cards-and-issues) uses an explicit
work manifest for live diagnostics, local dependency navigation, status/relation/ID
completion and literal hover help. Full imports and evidence analysis remain CLI
operations. For the repository backlog, select `roadmap/work-items.json`.

Optional [editor imports](../editors/vscode/README.md#imported-requirements-and-work-item-targets)
add typed requirement/work-item target completion and revision-checked source
navigation. Full dependency, supersession, plan and resource analysis remain CLI
operations; editor navigation does not establish workflow validity.
