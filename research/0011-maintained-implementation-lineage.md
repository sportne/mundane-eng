# Research 0011: Maintained Implementation Lineage

Status: Decided

Decision date: 2026-08-23

Roadmap task: [TC-0104](../roadmap/closed/task-0104-select-the-maintained-implementation-lineage.yaml)

## Question

Should the first maintained implementation evolve from the Experiment 0002
probe, or should a separate implementation be created and compared with it?

## Decision

Use a controlled extraction from the probe into one maintained implementation.
The probe remains a temporary behavioral oracle while the maintained code is
built and compared; it does not become the production architecture or a
runtime dependency.

TC-0201 shall create these maintained roots outside `experiments/`:

- `src/main/java` for shared code and later executable entry points;
- `src/test/java` for maintained tests;
- `build/maintained` for disposable classes and native-image output; and
- a repository-root `Makefile` for the dependency-free Java 21 and GraalVM
  build.

Java packages will begin under `mundanereq`. Their names and visibility are
implementation details during the trial, not a stable Java API.

## Cross-tool boundary

The common behavior fixed before extraction is deliberately narrower than a
common command-line interface:

- explicit files and directories select source under Clauses 7 and 12.3 of
  the 0.2 language standard;
- source diagnostics carry a file, one-based line and column, understandable
  category, and plain-language description; and
- every executable remains usable from a clean checkout without hidden state.

The initial native tools use three process-result classes:

- exit status `0`: the requested operation completed successfully and did not
  find a negative result defined by that operation;
- exit status `1`: the requested operation completed and found a defined
  negative result, such as nonconforming source or a formatting-check
  difference; and
- exit status `2`: invalid invocation or an operational failure prevented the
  requested evaluation from completing.

A valid trace query with no relationships is an empty successful result, not a
failure. Each tool's trial contract must state which of its outcomes fit these
classes. The first suite shall not assign another status without a demonstrated
need and a documented convention change.

Exact diagnostic codes and rendering, option spelling, usage text, and the
detailed mapping of tool-specific outcomes remain local to each tool contract.
Sharing Java code does not require presenting one combined CLI.

## Why controlled extraction

The 796-line `Probe` couples seven concerns in one class: command handling,
source discovery, physical decoding, concrete parsing, semantic objects,
source-set validation, normalized inventory, and incoming trace analysis.
That shape is unsuitable as the shared foundation, but the implementation has
substantial audited behavior and executable evidence. Reimplementing all of
it at once would discard useful fault localization without answering a product
question.

Directly refactoring the experiment in place is also insufficient. The probe
discards comments and most physical structure after parsing, while a safe
formatter needs a deliberately small concrete-source representation. The
maintained lineage will therefore extract behavior in layers and compare each
semantic result with the probe. Temporary duplication exists only to make the
transition observable and ends when TC-0204 accepts the maintained code.

## Component disposition

| Probe concern | Disposition | Reason |
| --- | --- | --- |
| UTF-8 decoding, physical checks, and coordinates | Extract and adapt | The rules are language-wide, precisely specified, and needed by every source consumer. |
| Explicit-input source discovery | Extract and adapt | All three tools must select the same source set without hidden repository state. |
| Line-oriented parser | Use as behavioral evidence; extract rules through the TC-0202 concrete layer | Its interpretation is proven, but its current representation drops formatter-relevant source structure. |
| Requirement and content records | Extract semantic values, separating source locations | Validator and trace tools need one model; formatting trivia must not enter requirement semantics. |
| Identity and referential validation | Extract | These are source-language rules shared by semantic consumers. |
| Diagnostic data | Extract the concept, not current strings or rendering | File, position, category, and message are required; codes, wording, ordering, and recovery are not stable contracts. |
| Normalized inventory | Retain as a migration oracle and test utility | It provides comparison evidence but is not a public interchange format. |
| Incoming trace algorithm and output | Leave in the experiment until Stage 5 | Trace analysis is derived tool behavior, not core parsing or validation. |
| Probe command line and exit assignments | Do not extract as shared architecture | Each executable needs its own bounded interface; the experimental CLI is nonnormative. |

Extraction means preserving specified behavior, not copying the probe's class
boundaries. No generalized compiler framework, plugin layer, or public module
system is justified.

## ReqIF disposition

`ReqifProbe` compiles the Experiment 0002 `Probe.java` directly rather than
carrying a second parser. That dependency was reasonable for a bounded
interchange experiment, but neither its XML profile objects nor its CLI belong
in the shared source foundation.

The ReqIF code remains historical experiment evidence. Its comment-omission
roundtrip stays in the TC-0204 preservation set. A future maintained ReqIF
tool may consume the shared semantic model after independent interoperability
evidence justifies that work; the current adapter must not determine core
types, timestamps, hierarchy, attributes, or build structure.

## Transition and retirement boundary

1. TC-0201 creates an empty maintained project boundary and proves the build.
2. TC-0202 introduces only the physical and concrete source information needed
   by identified validator and formatter consumers.
3. TC-0203 moves semantic interpretation and validation behind that layer.
4. TC-0204 ports the audited preservation set and compares maintained results
   with the probe over all project corpora.
5. After TC-0204, product tools shall depend only on maintained code. The
   experiment remains reproducible at its annotated tag and may remain in the
   repository as evidence, but it is no longer an oracle for routine changes.

The annotated tag `reference-probe-0.2-audited` identifies commit `7cf9c58`,
which contains the corrected probe, strengthened test baseline, and completed
0.2 audit. Historical tags are not moved. Existing experiment Makefiles and
paths remain unchanged during extraction.

## Rollback condition

Stop controlled extraction if preserving the TC-0103 evidence would require
product code to depend on the experiment, two maintained parsers to persist,
or formatter needs to force trivia into semantic requirement objects. In that
case, build one small maintained parser alongside the frozen probe, compare it
through the same conformance evidence, and retire the comparison once it
passes. Do not broaden the source language or introduce a generalized syntax
framework merely to preserve the initial extraction plan.

## Consequences

- There will be one maintained interpretation, shared as code but delivered
  through separate validator, formatter, and trace executables.
- The first maintained API is internal and may change as those real consumers
  reveal the right boundaries.
- Historical reproducibility comes from the exact audited commit and annotated
  tag, not from keeping experiment source in the production dependency graph.
- TC-0201 may establish project structure, but it must not begin parser or tool
  feature implementation.
