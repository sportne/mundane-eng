# Work-item editor verification

Recorded locally on 2026-09-06 for TC-2001 through TC-2006. The paired editor build
is 0.1.2, with the additive work-item source in editor protocol 0.1. Requirement
and work-item source and compiled contracts retain their existing versions.

## Executed evidence

`scripts/run-ci-verification.sh` completed with exit 0 using GraalVM CE 21.0.2,
Node 22.18.0, npm 9.2.0 and VS Code 1.109.5 on the documented Linux environment.
The wrapper records environment, verification and failure-injection logs under
`build/ci-evidence/`. These logs and packages are disposable local outputs.

- All 13 maintained Java test groups passed. Work snapshot tests compare disk and
  buffer semantics and diagnostics, exercise invalid YAML and lifecycle values,
  duplicate IDs and cycle rules, and confirm unsaved values do not write source.
- JVM/native bridge checks agree on requirements and all 60 repository cards,
  invalid selections/schema requests, cursor requests and failed output delivery.
- Eight Node tests cover path/buffer/process boundaries, cache freshness, explicit
  work selection limits, and cross-domain overlap including attribute declarations.
- Actual development and installed Extension Hosts exercise both artifact domains,
  unsaved field/duplicate/missing-dependency/cycle errors and repair, cancellation
  freshness, selection failures and overlap, unselected files, and no-save behavior.
- Navigation follows dependency tokens to current task IDs, including unsaved
  target movement and non-BMP coordinates. Narrative mentions and invalid projects
  yield no jump. Work items receive no formatting edits.
- Completion checks task/issue statuses, quoted insertions that validate as YAML,
  relation roles, local IDs and used-prerequisite exclusion. Hover renders authored
  titles literally with command/HTML trust disabled. Comments, body strings,
  imported target scopes and unsupported YAML receive no guessed assistance.
- The full 60-card repository backlog is copied into disposable workspaces and
  selected through its real manifest. Ordinary `.yaml` language providers validate
  and complete it. A work-item edit launches one work request and zero requirement
  requests. Existing two-folder traffic checks still pass.
- Bundle inventory, checksums, notices, versions, platform constraints and repeatable
  assembly checks pass. An isolated profile loads the installed VSIX and extracted
  bridge, recovers from missing executables, mismatched protocols and bad selections,
  then repeats the provider and backlog workflow.
- Both injected CI faults fail at their expected targets, and their source inputs
  are restored exactly. Existing language, work-item, attribute, impact, package,
  golden, seeded and mutation checks pass through the same authoritative gate.

The initial environment exposed an empty `npm` executable in a separate Node
installation. That invocation is not accepted as npm verification evidence. The
successful full wrapper used the working Node/npm pair identified above.

## Scope and limits

Work-item editor validation covers source rules and local prerequisites. It does
not resolve imported relation targets or evidence resources, enforce supersession
analysis, infer completion, or change authored status. Continue using the full
work-item analyzer for those explicit imports and resources. Task IDs and YAML
remain authoritative; this work adds no authoring language or persisted editor model.

The editor selection is bounded to 128 cards of 1 MiB each, with a 16 MiB wire
limit. This evidence establishes local Linux behavior, not other platforms,
Marketplace publication, user studies or performance claims beyond the recorded
request-count checks. The [authoring guide](../editors/vscode/README.md#yaml-task-cards-and-issues)
and [contract](../specification/0025-work-item-editor-0.1.md) describe setup and limits.
