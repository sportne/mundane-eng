# Experiment 0008: Formatting Policy

Status: Completed

Result date: 2026-08-23

Roadmap task: [TC-0401](../../roadmap/closed/task-0401-run-the-formatting-policy-experiment.yaml)

## Question

Which minimum canonicalization rules reduce routine diff noise without
erasing useful source structure, changing semantics, or implying that comments
attach to nearby model objects?

## Fixtures

[`input-varied-crlf.mreq`](input-varied-crlf.mreq) is conforming 0.2 source
with CRLF line endings. It contains three records, a minimum one-line record
separator, a blank/comment/two-blank separator retained exactly, a run of three
comment-free inter-record blank lines, wrapped prose, comments at several
structural boundaries, repeated relationships, and a multiline opaque LaTeX
payload.

Two hand-constructed candidate results isolate the policy choice:

- [`candidate-conservative.mreq`](candidate-conservative.mreq) uses LF and
  exactly one blank line between comment-free adjacent records while preserving every
  nonblank line byte-for-byte and in the same order.
- [`candidate-prose-reflow.mreq`](candidate-prose-reflow.mreq) additionally
  reflows prose body lines toward a shorter width. It leaves scalar fields,
  comments, and LaTeX payload lines unchanged.

The candidates originated as experiment evidence rather than executable
behavior. TC-0402 subsequently adopted `candidate-conservative.mreq` as the
maintained golden result for the selected policy; the formatter implementation
and trial contract, rather than this experiment alone, now own that behavior.

## Comparison method

The maintained interpreter validates all three files and produces the same
semantic inventory for each. The comparison uses ordinary text tools:

    make test
    java -cp build/maintained/classes mundanereq.InventoryMain \
        experiments/0008-formatting-policy/FILE
    git diff --no-index --ignore-cr-at-eol --word-diff \
        experiments/0008-formatting-policy/input-varied-crlf.mreq \
        experiments/0008-formatting-policy/CANDIDATE

Byte-level checks separately compare the ordered nonblank lines, comment
lines, and lines from `math latex` through `end math`.

## Observations

Both candidates preserve the semantic inventory. That fact is necessary but
does not make their review effects equivalent.

The conservative candidate exposes only one structural edit once CRLF display
noise is ignored: a three-line inter-record gap becomes one line. Every comment,
record, field, relationship, prose line, and LaTeX line remains visibly where
the author put it. A whole-file byte diff still exposes CRLF normalization,
which is expected and should occur as one deliberate formatting change.

The prose-reflow candidate changes four semantic paragraphs in addition to the
same structural edits. Word-level diff confirms that words are retained, but a
normal unified diff replaces several otherwise unchanged lines. The chosen
width is arbitrary, future edits near a wrap boundary create unrelated line
movement, and reflow would require the formatter to reconstruct semantic prose
while making special exceptions for comments and opaque math. It offers no
demonstrated benefit over editor wrapping for this corpus.

Sorting records, fields, or repeated `decomposes` lines was rejected without a
fixture. Even where order is not semantic, sorting makes broad source changes,
can disturb human narrative order, and would tempt proximity-based comment
attachment. Trailing-space removal was also rejected: conforming scalar
boundaries are already constrained, while whitespace inside prose and math can
participate in their semantic or opaque values.

## Selected policy

The first formatter shall perform only these rewrites on a conforming source
file:

1. normalize every line ending to LF;
2. between an `end requirement` line and the next `requirement` opener, when
   the intervening physical lines contain only the one or more blank lines
   required by the grammar and contain no comments, emit exactly one blank
   line.

All nonblank physical-line text and order shall be preserved exactly. In
particular, the formatter shall not reflow prose, normalize indentation, sort
records or relationships, alter scalar spelling, interpret LaTeX, or move or
rewrite comments. Blank lines outside the narrowly defined direct
record-to-record boundary remain unchanged. If a comment occurs between two
records, all intervening blank lines remain unchanged; this avoids inventing a
rule about which record the comment visually accompanies.

The formatter shall reject invalid source before emitting or replacing any
formatted result. Formatting does not repair source.

## Operating modes

The first interface shall have three explicit modes:

- `mundanereq-format FILE [CONTEXT...]` names exactly one explicit regular output file plus
  zero or more context inputs. It validates the source set selected from the
  output file and all context inputs, then emits only the output file's
  formatted bytes without modifying it. Omitting context therefore requests a
  one-file source set;
- `mundanereq-format --check INPUT...` accepts explicit files or directories using the 0.2 selection
  rules and reports whether every selected file is already formatted;
- `mundanereq-format --write INPUT...` accepts explicit files or directories and replaces only valid
  selected files whose formatted bytes differ.

`--write` is deliberately named and shall validate the complete selected
source set before writing any file. Each changed file shall be written to a
same-directory temporary file and then moved over that one destination, so a
destination is never populated incrementally. This is a per-file safe
replacement rule, not a transaction across the selected set: an operational
failure may occur after earlier files were replaced. The project does not
select an implicit repository root, configuration file, daemon, or
automatic-on-merge behavior.

## Decision

Adopt the conservative policy. It creates useful cross-platform and
record-separator consistency, remains completely explainable from ordinary
source, and can be implemented over the existing concrete lines. Reject prose
reflow and every ordering rule for the first formatter.

This is intentionally a narrow formatter. Complexity beyond these two rules
would need new workflow evidence rather than aesthetic preference.
