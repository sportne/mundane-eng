# Experiment 0021: Glossary and Formal Symbols

Status: Completed

Date: 2026-08-29

Roadmap task: [TC-0805](../../roadmap/closed/task-0805-test-glossary-and-formal-symbol-artifacts.yaml)

## Question

When do repeated domain terms or mathematical symbols need separately
identified, human-readable definitions?

## Scenario

The term case uses three requirements derived from the existing UAS corpus.
They repeat “command link unavailable” with one shared meaning. A rename-only
transition changes the preferred term to “command link lost” without changing
its definition. A separate definition-only transition changes the event that
makes the state true while requirement source and use mappings remain
byte-identical. Keeping those transitions separate avoids assuming that every
meaning change preserves conceptual identity; that remains an explicit project
review decision.

The mathematical case retains the existing `SYS-006` formula, all six adjacent
variable definitions, values, and units. Its LaTeX payload is byte-identical
through the term rename. Two additional bounded requirements deliberately reuse
the spelling `T` for a sampling interval in seconds and temperature in kelvin,
each defined beside its own equation. This tests local symbol scope rather than
assuming project-global symbol uniqueness.

## Candidates

1. An ordinary prose file is declared normative and contains the shared term
   definition. It is fully readable but has no stable identity or use map.
2. A companion table gives the one reused term a stable ID, preferred spelling,
   explicit aliases, and meaning. A second table maps its four source uses
   across three requirements.
3. Tool-only conventions hard-code the meaning and accepted spellings in an
   AWK linter rather than requirements-project source.

Normativity is a governance choice, not a property of tables: candidates 1 and
2 are both declared authoritative for comparison. Ordinary prose remains the
better representation when stable identity and queries are unnecessary.

## Checks and results

[`run.sh`](run.sh) validates all three `.mreq` baselines and checks that each
mapped literal is the preferred term or an explicit alias, occurs in the named
requirement, and covers every known occurrence. Mutations detect an undefined
definition ID, a literal mapped to the wrong definition, and a deleted use row.

Changed definition IDs are derived from the tables rather than hard-coded. Both
the rename and meaning-change queries identify `SYS-006`, `SYS-007`, and
`SYS-009`. The rename changes four ordinary source lines. The definition-only
change leaves requirement source untouched while making all four mapped uses
across three requirements review-relevant.

The old tool-owned convention rejects all four renamed uses until the linter's
source code is changed. That behavior is mechanically exercised, not merely an
architectural objection. It also accepts Baseline C while silently reporting
the obsolete meaning until its code changes. Tool-owned authority couples both
rename and meaning changes to a particular implementation.

No formal-symbol companion is selected. `SYS-006` is independently readable
because its symbols and units remain beside the formula. Project-global symbol
identity would incorrectly conflate the two local meanings of `T`. Detecting
arbitrary undefined LaTeX symbols would require interpreting an opaque payload,
which this experiment deliberately does not do.

## Decision

Do not select a term companion yet. Stable identity and explicit mappings make
the two queries deterministic, but for one definition and three affected
requirements an authoritative prose definition plus ordinary source search
returns the same impact set with fewer concepts and no mapping maintenance.
Retain the companion as a candidate for a larger or more ambiguous workflow.

Do not infer normativity from representation. Keep author guidance and simple
normative definitions in ordinary prose. Reject tool-owned authority. Keep
mathematical symbol definitions local and adjacent for the tested workflows;
do not add glossary or symbol syntax to `.mreq`, standardize a companion table,
promise a LaTeX subset, or interpret formulas.
