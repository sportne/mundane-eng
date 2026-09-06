# SARIF validation output

Status: Selected experimental command addendum, decided before implementation.

Select `--output=sarif --root DIR` on `mundanereq-validate`, after an optional
leading `--source=yaml-0.3` or `--source=yaml-0.4`, followed by explicit inputs.
`--` ends options. Root is required, must be a directory, and all selected input
paths must be lexically beneath it. No root traversal, repository discovery, forge
upload or editor installation is implied. Default text mode and its version/help
forms retain their existing behavior. Root is meaningful only in SARIF mode.
Unknown/duplicate options, invalid roots and missing arguments produce stderr and
exit 2 without SARIF. Input discovery/read failures after a valid invocation
produce structured diagnostics and exit 2.

Use [OASIS SARIF 2.1.0 with Errata 01](https://docs.oasis-open.org/sarif/sarif/v2.1.0/errata01/os/sarif-v2.1.0-errata01-os-complete.html)
and its published schema. The checked-in schema is a verification dependency,
not a runtime dependency. One run contains validator identity, independent command
and source contracts, stable rule IDs from the compiled diagnostic catalog,
`columnKind:unicodeCodePoints`, deterministic results and invocation status.

All current source diagnostics have level `error`; they retain their rule IDs.
Artifact URIs are percent-encoded paths relative to the explicitly supplied root.
The consuming editor/viewer supplies the same source root; no machine-specific
absolute base URI, clock time, environment or command line is embedded. Inputs,
results and observed rule descriptors are sorted deterministically. Unknown future
rule IDs must first be added to the diagnostic contract.

The interpreter currently supplies diagnostic points, not token ranges. Emit
truthful `startLine`/`startColumn` without invented end positions. Use Unicode code-point columns. Missing/unreadable inputs have an
artifact URI but no fabricated source region. Unicode supplementary characters
count as one code point, independent of UTF-16 editor indexing. A consumer must
honor columnKind when navigating. Full diagnostic spans require later parser data.

Exit 0 means valid source; 1 means source nonconformance; 2 means invocation/input
or output failure. Emitting valid SARIF does not turn a failed validation into a
successful validation. `properties.sourceSetValid` records source validity and
`properties.syntaxComplete` records interpretation completeness. Invocation
`executionSuccessful` is false for incomplete interpretation/input failures and
true for a fully executed analysis, including a detected semantic violation.
`exitCode` records the computed validation status before output delivery.
An incomplete-interpretation notification makes omitted downstream checks visible.

Closed output streams, short writes, broken pipes and flush failures remain
command failures. The owning JVM tests include closed stdout/stderr PrintStreams.
An externally closed OS stderr descriptor is only observable when a diagnostic
write is attempted; quiet SARIF emission writes no stderr bytes and cannot
portably detect that unused-descriptor closure. A later delivery failure cannot retroactively alter already written
JSON; callers must check the actual process exit status before retaining output.
No successful output artifact is authoritative requirement source.
