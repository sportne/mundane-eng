# Research 0015: Formatter Verification

Status: Complete

Date: 2026-08-23

Maintained-corpus coverage expanded: 2026-08-30

Roadmap task: [TC-0403](../roadmap/closed/task-0403-verify-formatter-safety-properties.yaml)

## Question

Does the conservative formatter preserve requirement semantics, authored
nonblank text, comments, and opaque math while remaining idempotent and
predictable across JVM and native execution?

## Automated evidence

`make formatter-verify` builds `mundanereq-format` as a no-fallback GraalVM
native image and runs the same source through the Java entry point and native
process. The maintained source-set matrix contains:

- the valid 0.1 and 0.2 conformance corpora;
- both source-representation layouts;
- the sustained-authoring and NASA FRET transfer corpora; and
- all three Experiment 0008 formatting-policy fixtures, including all-CRLF
  input and both LF candidates;
- trace-workflow, integrated-toolchain, and operational corpora; and
- all language-valid source sets from the identity, verification, safety,
  allocation, glossary and symbol, and trace-policy experiments.

For every selected physical file, the gate formats once and twice and requires
the second byte sequence to equal the first. It requires ordered nonblank line
text, ordered full-line comments, and every physical line from `math latex`
through `end math` to remain exact. It then interprets the formatted source set
and requires equal requirements and decomposition relationships.

Every maintained source set is also passed to `--check` through both the JVM
entry point and native process with exact status, output, and diagnostic
agreement. A completeness assertion discovers every `.mreq` file under
`conformance/` and `experiments/` outside an `invalid/` directory and requires
the explicit source-set matrix to cover it. Adding maintained valid source therefore
cannot silently bypass this gate.

The process-level matrix compares status, standard output, and standard error
between JVM and native executions for:

- CRLF standard-output formatting;
- changed and unchanged `--check` results;
- `--write` results and resulting bytes;
- invalid-input rejection without mutation; and
- standard-output formatting of one file whose relationship target is supplied
  by a separate context file.

The maintained command-interface tests additionally cover failed standard
output, POSIX permission-bit preservation, failed replacement before mutation,
temporary-file cleanup, and complete-source-set validation before any write.
Native boundary isolation performs real `--check` work while sibling
executables are removed.

The development mode statuses verified here are:

- standard-output mode returns `0` after complete output and `2` for
  invocation, source-set, or output failure;
- check mode returns `0` when every selected file is formatted, `1` when one
  or more valid files would change, and `2` for invocation or source-set
  failure; and
- write mode returns `0` after all required replacements and `2` for
  invocation, source-set, or replacement failure.

These meanings are the behaviors under test for trial publication; TC-0404
decides whether to publish them as a maintained interface.

## Ordinary-diff review

The Experiment 0008 input produces an intentional whole-file line-ending
rewrite from CRLF to LF. With end-of-line differences ignored, ordinary unified
diff shows only two removed excess inter-record blank lines. Word-level diff
shows no nonblank token change. The comment-separated boundary, repeated
relationships, prose wrapping, record order, and multiline LaTeX remain in
place.

The twenty-file one-record-per-file corpus and the three-file module corpus are
already formatted under the selected policy. Converting working copies to CRLF
and formatting them back produces no unified diff when CR-at-EOL is ignored.
Thus a large normalization can still appear as a whole-file byte change, but
ordinary Git options reveal that it contains no requirement-content change.
Teams should introduce line-ending normalization as a deliberate standalone
commit rather than mixing it with requirement edits.

No semantic renderer or semantic diff was needed to inspect these changes.

## Safety boundary

The evidence establishes exact preservation of source-language semantics, all
nonblank physical text, comment text and order, and opaque math text for every
maintained valid source set. It establishes byte-level idempotence after the
first format and JVM/native agreement for the supported modes.

It does not establish that arbitrary future language versions are safe, that
LaTeX expressions are mathematically equivalent, that filesystem metadata
beyond documented POSIX permission bits is retained, or that multiple file
replacements form one transaction.

## Decision

No safety blocker was found. The formatter may advance to a maintained trial
contract without widening the two-rule policy. Any future prose reflow,
ordering, indentation, comment-placement, or math rewrite remains a new policy
experiment rather than an implementation detail.

## Maintained-corpus expansion

TC-1001 discovered that valid source added by Experiments 0016 through 0022
was visible to the completeness assertion but absent from the explicit
source-set matrix. TC-1002 added 15 selections at their actual independent
source-set boundaries, including allocation-typo and trace-policy-failure
fixtures that conform to the source language while intentionally failing
separate project policy.

The expanded focused gate passes over 29 source sets and 60 files. It preserves
33 comments and 102 opaque math lines in order, remains byte-idempotent, and
agrees between JVM and native execution. The complete `make verify` gate also
passes, including native boundaries, all three tool verifications, package and
CI checks, integrated and multi-author workflows, and independent conformance.
No formatter implementation or policy change was necessary.

Experiment 0024 subsequently added its four-file vaccine-monitoring work
product as one independent source set. The maintained matrix now covers 30
source sets and 64 files and preserves 37 comments and 102 opaque math lines.
This was an inventory-only extension; formatter behavior and policy remain
unchanged.
