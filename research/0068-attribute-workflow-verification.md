# Research 0068: Integrated project attribute workflows

TC-1308 completes the TC-1303–1308 implementation batch with bounded, repeatable
repository evidence. The [example guide](../examples/attributes/README.md) and
[capability/migration guide](../distribution/attributes.md) describe implemented
behavior, explicit opt-in and remaining editor/interchange limits.

## Examples and behavioral evidence

The small checked-in logger source, JSON declaration and independently selected TSV
plan compile through explicit baseline/current scopes and a pinned baseline. The
controlled current edit changes one owner value and a declaration description.
The resulting report explains both changes and links to actual requirement,
declaration and assertion source copies. Deleting derived JSON and HTML and rebuilding
produces identical analysis and report bytes. It does not execute the planned review.

The medium adoption corpus preserves all 57 built-in requirement values and edges
from the maintained four-file vaccine-monitoring example. Illustrative layer enums
and four optional team labels are the only added descriptive facts. Original source
and pilot attribution remain intact under the repository license; no user feedback,
engineering approval, deployment or external interoperability result was invented.

Twelve deterministic seeds (130800–130811) check two-file authoring, independently
expected values, CRLF-only formatting/idempotence, semantic preservation, exact pins,
value-only and whole-schema changes, report rebuild and incomplete publication
barriers. Unicode source slices and invalid/valid artifact goldens have independent
assertions. Existing custom/YAML 0.3, old output, mutation and report fixtures remain
active. Three compilable behavioral mutations were killed: ignored requiredness,
invalid enum acceptance and ignored schema comparison. [Mutation results](../experiments/0036-project-attributes/results/mutations.json)
record actual baseline/mutant signatures. [Replay and minimization instructions](../experiments/0036-project-attributes/README.md)
state bounds and retained-input handling; no general coverage score is claimed.

## Clean verification

The immutable candidate `bc18f9a657f61075c45be30fcf5139a2d2a726db` is retained by local
branch `codex/attribute-verification-evidence`. A separate detached checkout began
without a build directory and remained clean after verification. On 2026-09-05,
`scripts/run-ci-verification.sh` completed with exit 0 there, using Java 21.0.2,
GraalVM Native Image and the recorded Linux build environment.

All 21 maintained JVM groups, native builds, historical compatibility, independent
structural schemas, parser-free linking/work-item consumption, source-to-report
workflows and new attribute checks passed. Both deliberately injected CI failures
reached their expected failing targets and restored exact input bytes.

- [Environment and candidate](../experiments/0036-project-attributes/results/clean-environment.txt)
- [Full authoritative make verify log](../experiments/0036-project-attributes/results/clean-verify.log)
- [CI failure propagation](../experiments/0036-project-attributes/results/clean-failure-propagation.log)
- [Wrapper exit status](../experiments/0036-project-attributes/results/clean-status.txt)

Only terminal escape sequences and trailing output whitespace were removed from
copied logs. Final closeout adds this evidence, completion notes, the card's closed
path and the regenerated index. Runtime, build and check inputs match the verified
candidate. Final inventory, dependency analysis, exact derived-index rebuild, relative
links and diff whitespace are checked again after closeout. This is local evidence;
no hosted CI run, release or push is claimed.

## Compatibility and limits

Old source files remain valid without adoption. YAML 0.4 uses explicit checked-in
project declarations, text/enum values, required/optional presence and no defaults.
Compiled definitions are derived snapshots; human-authored IDs remain identity.
Whole-schema changes request conservative review rather than infer satisfaction.
Editor-host/schema completion and attribute ReqIF mapping remain unimplemented.
The report remains experimental, and formatter snapshot checks retain the documented
last-check/rename race. Other artifact formats remain independent decisions.
