# Task TC-1401: Protect Formatter Write-Back

```json
{
  "format": "mundane-work-source-0.1",
  "status": "Complete",
  "dependencies": [],
  "relations": [],
  "planning": {
    "stage": "14",
    "type": "Implementation and verification",
    "condition": "none",
    "unlocks": "follow-up work selected by the completion decision",
    "statusNote": ""
  }
}
```

## Question

How can formatter write-back detect intervening source changes and make partial
multi-file replacement failures understandable and recoverable?

## Outcome

The formatter protects detected external edits and reports exactly which selected
files were changed, failed, or left unprocessed.

## Work

- Reproduce the current sequence: selection snapshots bytes, then writeFiles calls
  replace without comparing current contents with the selected snapshot.
- Specify and implement a pre-replacement content/identity check and observable
  conflict diagnostic. Document the remaining check-to-replace race and filesystem
  limitations; do not claim portable compare-and-swap guarantees.
- Choose stop/continue behavior for a later replacement failure, preserving complete
  source-set prevalidation and the existing per-file replacement safety boundary.
- Make retries safe for already formatted files and preserve concurrent user edits.
  Keep permission behavior, temporary-file cleanup, and fallback handling explicit.

## Acceptance evidence

- Deterministic injected edits after reading but before the replacement check are
  detected; the edited bytes survive and the command returns operational failure.
- A later-file failure after one successful replacement reports changed, failed,
  and unprocessed paths according to the documented policy.
- Retrying the remaining work neither overwrites detected external edits nor
  introduces semantic changes; idempotence and parse-format-parse still pass.
- Tests cover deletion/replacement of a selected file and temporary-file cleanup.
  Documentation identifies residual races and any non-atomic fallback behavior.
- Existing POSIX permission, invalid-source prevalidation, and output-failure
  regressions remain covered.

## Out of scope

- Whole-source-set transactions, editor locking protocols, a daemon, or guaranteed
  preservation of metadata already excluded by the formatter contract.

## Compatibility and affected components

This closes a current source-loss risk; specify the tightened exit/diagnostic
behavior in the trial contract. Components: FormatterMain selection/write/replace
paths, filesystem fault-injection tests, and formatter documentation.

## Completion decision

Accept only reproducible preservation and recovery evidence. Narrow the promised
race protection to what supported filesystem operations can establish; do not
substitute a timestamp-only comparison or claim that atomic rename detects edits.

## References

- [Roadmap](../0001-initial-roadmap.md)
- [Formatter write-back](../../src/main/java/mundanereq/cli/FormatterMain.java)
- [Formatter contract](../../specification/0008-formatter-trial-contract-0.1.md)
- [Existing safety evidence](task-0403-verify-formatter-safety-properties.md)


## Completion evidence and decision

Completed 2026-09-05 in the requirements YAML batch.

Added byte/file-key snapshot checks before replacement and fallback, stop-on-
failure reporting, deterministic external-edit/deletion/replacement tests, retry
and temporary cleanup evidence. The remaining check-to-rename race is documented.

The [batch verification](../../research/0035-yaml-requirements-batch-verification.md)
records full make verify, golden/native checks and limitations. The
[contract decision](../../research/0034-yaml-requirements-contract-decision.md),
[source contract](../../specification/0010-requirements-yaml-0.3.md),
[command addendum](../../specification/0011-tool-safety-and-yaml-commands.md), and
[reproducible regression evidence](../../experiments/0026-yaml-requirements-batch/README.md)
provide the acceptance artifacts. Historical contracts remain preserved; other
artifact authoring formats and future attribute scope remain independent decisions.
