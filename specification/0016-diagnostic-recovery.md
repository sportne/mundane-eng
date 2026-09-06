# Diagnostic recovery and incomplete interpretation

Status: Experimental YAML interpretation contract.

YAML document syntax, encoding, forbidden profile constructs and invalid top-level
envelopes are file-fatal. Once a valid envelope and sequence have been composed,
independently invalid requirement mappings do not discard valid neighboring
mappings. The existing YAML byte/depth/record/diagnostic limits remain in force.
No textual resynchronization of malformed YAML is attempted.

`Interpreter.Result.syntaxComplete()` is false if any selected source could not
be fully decoded and interpreted. Recovered requirements/origins are diagnostic
context and must not be treated as a complete source model. Duplicate-ID checks
can still report duplicates actually observed. Absent-target checks are suppressed
across the selected source set when syntax is incomplete, because absence cannot
be established. This may postpone a real missing-target error until primary
errors are repaired. With complete syntax, relationship validation is unchanged;
a semantic error can make `valid()` false while `syntaxComplete()` is true.

Strict validation still returns nonconformance. Formatter write-back is blocked
for the entire selected invalid source set. Compiled requirements retain
`complete:false`, primary diagnostics, and an empty `requirements` array; recovered
records are not published for linking. Existing valid-input semantic inventories,
formatter output, default human CLI mode and source selection are unchanged.
Diagnostic ordering remains path, line, column and rule. The YAML diagnostic cap
is 100 per file; the [catalog](0013-compiled-diagnostic-rules.md) defines rule IDs.
