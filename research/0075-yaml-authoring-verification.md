# YAML authoring migration verification

Recorded 2026-09-07 for TC-2201 through TC-2204. Attribute declarations use
`mundanereq-attributes-yaml-0.1`; plans use `mundane-plan-yaml-0.1`. Compiled
JSON value shapes remain separate from authoring syntax. Editor bundle is 0.2.0.

## Executed evidence

`scripts/run-ci-verification.sh` completed with exit 0 on Ubuntu 24.04 Linux
x86-64, GraalVM CE 21.0.2, Node 22.13.1, npm 9.2.0, Python 3.12.3 and VS Code
1.109.5. The full gate and both deliberate failure injections passed. Injected
inputs were restored byte-for-byte. Logs are disposable under `build/ci-evidence/`.

- All 14 maintained Java groups passed, including declaration YAML typing,
  duplicate-key coordinates, forbidden YAML features, plan comments/folding,
  actual record origins, local reference checks, failed publication and rechecks.
- Public plan compilation passed exact JVM/native parity on all three migrated
  corpora. An independent decoded-value oracle checked every plan/activity/coverage
  field, and source hashes and source coordinates were checked against actual YAML.
- Attribute validation/formatting/compilation/linking/report checks passed, including
  57 requirements with unchanged built-ins, source/schema mutations and real output
  failures. Twelve seeded workflows and three behavior-changing mutations passed.
- Regenerated goldens preserve pre-migration requirement values, normalized
  declaration meaning, coverage/review findings and affected-node selection. Changed
  source paths, coordinates, digests and plan compiler metadata are intentional.
- Impact golden/adversarial/seeded/mutation checks and parser-free serialized
  consumers passed. The logger example retains six explained review candidates.
- Nine Node tests, JVM/native editor bridge/import checks, actual development and
  installed VS Code hosts passed. YAML declaration buffers drive validation,
  completion and literal hover. Existing work-item and imported-target providers,
  missing/modified-source recovery and delayed-response rejection still pass.
- Two-folder traffic remains bounded: repeated queries launch one request, unrelated
  edits launch zero, a selected edit launches one, and both concurrent folders return
  results. Package inventory/checksums/notices, matching versions, deterministic
  assembly and installed configuration/protocol recovery passed.

## Scope

JSON declaration and TSV plan source parsers/fixtures are removed, without adapters.
Generated JSON, JSON manifests and supported requirement profiles remain. Source
revisions remain human-controlled; none of these checks imply verification execution,
evidence adequacy, approval or satisfaction. Other platforms/publication are untested.

The final cleanup card is created after this completed migration. Its outcome and
verification are recorded separately below when executed.
