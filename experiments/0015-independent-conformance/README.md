# Experiment 0015: Independent Conformance Interpretation

Status: Completed

Date: 2026-08-29

Roadmap task: [TC-0705](../../roadmap/closed/task-0705-obtain-independent-conformance-evidence.yaml)

## Question

Can the 0.2 written standard independently produce the same source validity and
semantic interpretation as the maintained implementation without access to its
design or output?

## Method

[`prepare-package.sh`](prepare-package.sh) reconstructs the sealed participant
package from the normative standard, 17 source files forming 16 selections, and
[`participant-task.md`](participant-task.md). It excludes fixture README files,
expected inventories, reference executables, Java source, history, and prior
analysis. [`input-SHA256SUMS`](input-SHA256SUMS) identifies every supplied byte.

A fresh independent agent interpreted the package manually. It self-reported
using shell and text inspection tools and no programming-language runtime,
project source, or reference output; the frozen files and package contents are
consistent with but cannot independently prove that private tool-use report.
Its exact method, 16 verdicts, complete independently authored
inventories, and four interpretation notes were frozen and checksummed under
[`result/`](result/). The [invocation record](participant-invocation.md) states
the participant class, exact prompt, assistance, and capture limit.

Only after the result was frozen did [`compare.rb`](compare.rb):

- invoke the maintained native validator on the same 16 selections;
- compare accepted/rejected decisions;
- parse both complete independent inventories and the maintained expected
  inventories into one neutral semantic value; and
- record diagnostic coordinates without demanding an anchor the standard does
  not prescribe.

The comparison is disposable experiment machinery written in Ruby. It is not a
second maintained parser and does not participate in source interpretation.

## Results

- All 16 validity decisions agree.
- Both valid selections contain the same three requirements, all scalar values,
  ordered prose/math blocks, and three decomposition relationships. Their
  neutral canonical semantic SHA-256 values agree exactly.
- Eleven selections have equal participant and maintained coordinates, including
  both Unicode-scalar-column fixtures.
- Five construct-level failures use different anchors: repeated relationship,
  unterminated math, comment before a body, comment within math, and comment
  within prose. The independent result anchors the decisive encountered token;
  the maintained implementation sometimes anchors the construct opener or next
  parse point.

[`comparison/comparison.tsv`](comparison/comparison.tsv) records every verdict
and coordinate disposition. [`comparison/semantic-comparison.tsv`](comparison/semantic-comparison.tsv)
records complete semantic equality. [`comparison/maintained-results.tsv`](comparison/maintained-results.tsv)
preserves the exact reference diagnostics used in the post-freeze comparison.

## Disposition of independent findings

1. A path named `0.1` does not select a parser mode. Clause 15.1 already states
   that version selection is external and 0.1 source is compatible with 0.2.
2. Duplicate IDs are symmetric in the model. Choosing the lexically later file
   as a diagnostic anchor is a deterministic tool choice, not semantics.
3. The standard requires a useful line and Unicode-scalar column but does not
   require one universal anchor for a missing or unterminated construct. The five
   differences are conforming diagnostic choices, not interpretation disagreement.
4. Specific grammar clauses, collected validity clauses, and diagnostic coverage
   clauses overlap intentionally; the specific substantive clause is sufficient
   to explain rejection.

No finding requires a specification clarification, fixture change, or maintained
implementation correction. Diagnostic byte-for-byte equality is not—and should
not become—language conformance.

## Decision

The written 0.2 standard has passed this bounded independent interpretation.
Retain it unchanged. This closes one implementation-independence gap for a 1.0
audit, but it is one agent-authored case and does not replace TC-0706's human
authoring/usability evidence.
