# Research 0048: Bounded parser recovery

Task: [TC-1403](../roadmap/closed/task-1403-recover-parser-diagnostics-safely.yaml).

The [before capture](../experiments/0031-parser-recovery/results/before-custom.json)
confirms the file-level failure: one malformed title hides valid BEFORE and later
records and produces an erroneous dangling reference to BEFORE. The maintained
parser now catches failures at record boundaries and retains valid neighbors.
Both malformed records receive primary diagnostics, while uncertain absent-target
findings are suppressed. No incomplete records are compiled or formatted.

[Contract 0016](../specification/0016-diagnostic-recovery.md) records the selected
synchronization boundary and stop conditions. There was no need to split unrelated
decoding/formatting responsibilities. The new syntaxComplete flag distinguishes
incomplete interpretation from a complete parse with a semantic error. The existing
valid() gate remains the requirement for downstream use. YAML 0.3 already recovered
independent mappings; invalid envelopes now explicitly prevent record construction,
and malformed YAML remains file-fatal. Older conformance corpora remain covered.

[Experiment 0031](../experiments/0031-parser-recovery/README.md) records the primary
location and public-output goldens. The owning JVM suite adds bounded seed-1403
mutations, math/body boundaries, decode and envelope failures, incomplete-source
barriers, and a diagnostic cap. Public CLI checks cover JVM/native output parity
and unchanged bytes after refused formatter write-back. Existing compiled-output
checks retain valid golden bytes and semantic compatibility across eight corpora.

The completion evidence records actual executed checks, not a claim of universal
error recovery. Unterminated math and malformed YAML intentionally stop reliable
recovery; missing-target diagnostics can be postponed until syntax is repaired.

Recorded execution: `make native-validator native-formatter compiled-verify`
passed with 16 JVM groups, followed by `python3 scripts/check-parser-recovery.py
build/maintained` passing the native/JVM recovery matrix. Java/GraalVM CE 21.0.2,
Ubuntu 24.04 x86-64. Existing compiled goldens and eight source corpora passed.
