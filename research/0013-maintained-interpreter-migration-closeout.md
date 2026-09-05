# Research 0013: Maintained Interpreter Migration Closeout

Status: Complete

Date: 2026-08-23

Roadmap task: [TC-0204](../roadmap/closed/task-0204-port-tests-and-prove-native-tool-boundaries.yaml)

## Question

Has the maintained interpreter preserved the audited behavior closely enough
to retire the historical probe as a routine implementation oracle, and can the
shared code support three independent GraalVM native executables?

## Regression transfer

The maintained test suite now runs the twelve historical behavior groups that
belong to shared interpretation: layout equivalence, prose and math, optional
fields, comments, line endings, discovery, explicit files, physical source,
Unicode edges, record and field diagnostics, math diagnostics, and identity
and relationship validation. The experimental command interface and incoming
trace query remain historical or later-tool concerns rather than parser tests.

The maintained and historical interpreters produced byte-identical normalized
inventories for every valid project corpus. SHA-256 values of that common
output were:

| Corpus | SHA-256 |
| --- | --- |
| 0.1 valid conformance | `1c67511cfff247103a53567518adc2efb806060e1a1ba68c55dfc031a5acb67b` |
| 0.2 valid conformance | `1c67511cfff247103a53567518adc2efb806060e1a1ba68c55dfc031a5acb67b` |
| Candidate A modules | `be870530e7542d9b35fba4f033952ebe8ed2fe0cb667049075fb92c591d844f9` |
| Candidate B one-record files | `be870530e7542d9b35fba4f033952ebe8ed2fe0cb667049075fb92c591d844f9` |
| Sustained-authoring corpus | `cb7268715fc04ce9fcdc0dabf73f9f8561ba2f05cd02aa341e9628331f36f865` |
| NASA FRET transfer corpus | `212f14bb8f8d2b1ef6fc861913021798b67a64580b9da6fe0e59713e460e87d9` |

The comparison used the inventory only as migration evidence. Routine tests
now exercise maintained code directly. The historical experiment remains
reproducible through its own Makefile and the annotated
`reference-probe-0.2-audited` tag.

## Native boundaries

Three minimal main classes build as separate Linux native images with GraalVM
CE 21.0.2 and `--no-fallback`:

- `mundanereq-validate`;
- `mundanereq-format`;
- `mundanereq-trace`.

These boundaries intentionally implement only a smoke invocation. An isolation
test removes each generated executable in turn and successfully starts the
other two. No executable invokes another, and no tool behavior has been pulled
forward from Stages 3 through 5.

## Decision

Stage 2 is complete. Product tools shall use the maintained interpretation,
not compile or invoke the historical probe. The common code remains one small
source representation and interpreter; no plugin system, generalized syntax
tree, metamodel, persistent index, or framework was needed.
