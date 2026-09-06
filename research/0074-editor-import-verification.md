# Imported editor target verification

Recorded locally on 2026-09-06 for TC-2101 through TC-2106, editor bundle 0.1.3.
Requirement, work-item and compiled artifact formats retain their existing versions.
The optional editor import fields extend editor protocol 0.1; older bridges reject
those fields. Use the paired bundle.

## Executed evidence

The final `scripts/run-ci-verification.sh` invocation completed with exit 0 using
GraalVM CE 21.0.2, Node 22.18.0, npm 9.2.0 and VS Code 1.109.5 on Linux x86-64.
The wrapper records actual environment, gate and failure-propagation logs beneath
`build/ci-evidence/`. Build artifacts and logs are disposable.

- All 14 maintained Java groups passed. New checks validate compiled attribute-bearing
  requirements, exact pins, mapped source hashes, unavailable/modified sources,
  scope/dependency/inventory restrictions, strict JSON and forged origins. Work
  source checks retain exact typed relation spans without changing compiled output.
- Nine Node tests passed, covering existing process/buffer/cache boundaries and
  imported snapshot overlays, watched missing files and local operation after import
  read failure. Selection and mapped paths remain bounded by the workspace.
- `scripts/check-editor-imports.py` builds fixtures through public requirement and
  work-item compiler commands. Thirteen cases compare exact JVM/native responses:
  valid navigation/completion, missing/changed source, forged origins, pin mismatch,
  duplicate scopes, build cycles, resource bounds, incomplete artifacts, client read
  failure, duplicate JSON, unsupported request domain and invalid diagnostic paths.
  Both processes also reject failed output delivery through `/dev/full`.
- Actual development and installed hosts navigate requirement and YAML work-item
  targets using explicit roots, including Unicode columns and identical IDs across
  different scopes. Scope/kind-qualified completion inserts valid quoted YAML.
  Hover exposes compiled revision and source state; hostile titles remain literal
  with HTML and command trust disabled. Body strings and unsupported roles do not
  acquire reference meaning.
- Unsaved comments and semantic edits block navigation while preserving clearly
  labeled compiled completion/hover. Missing sources and symlink escapes produce
  unavailable-source warnings. Restoring source, selections or pins restores
  navigation; invalid imports leave local prerequisites navigable.
- Import-only edits launch one work request and zero requirement requests. An actual
  bridge response delayed until after mapped-source modification is rejected. Imported
  source files do not become authored work inputs merely by being mapped.
- Both hosts still pass the existing requirement/local work providers and all 66
  repository cards. Two-folder request traffic remains bounded as previously checked.
- The matching Linux bundle passes inventory, checksums, provenance/notices, version,
  platform and repeatable assembly checks. An isolated installed VSIX uses the
  extracted bridge and repeats the source/configuration recovery workflow.
- The full gate retains language, work-item, attribute, impact, golden, seeded and
  mutation coverage. Both injected CI failures reach their expected targets and
  their input bytes are restored exactly.

During review, a new invalid-path regression rejected a binary built before its
matching guard was added. The final full rebuild included that guard and passed the
regression. The recorded success refers to that final rebuild.

## Meaning and limits

Navigation checks the mapped target file's current bytes and structural ID origin
against the selected compiled artifact. It does not certify the semantic accuracy
of all compiled fields or freshness of every source file in that artifact. Hover
explicitly describes compiled data, including when source is absent or changed.
Digests identify exact revisions; human-authored IDs remain identity.

The selection supports requirement output 0.1/0.2 and YAML work-item output 0.2.
Plan/evidence navigation, automatic builds, network retrieval, full imported workflow
analysis, source rewriting and status changes are outside this batch. No publication,
other-platform verification or user-study outcome is inferred from these local checks.
See the [setup guide](../editors/vscode/README.md#imported-requirements-and-work-item-targets)
and [contract](../specification/0026-editor-imports-0.1.md) for settings and limits.
