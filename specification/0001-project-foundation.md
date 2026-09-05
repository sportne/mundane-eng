# Specification 0001: Project Foundation

Status: Living project foundation; nonnormative

Last reconciled: 2026-08-30 at `mundanereq-source-0.2`

## Purpose

`mundanereq` is an experiment in applying a software-toolchain model to requirements engineering.

The central premise is that requirements should have a durable, human-readable textual source representation that is suitable for normal Git version control and ordinary code-review workflows. Requirements-specific tools may parse, validate, analyze, render, transform, export, or otherwise operate on that source, but the source repository remains authoritative.

## Core model

The project should treat requirements as independent model objects rather than as paragraphs whose identity depends on a document, section, file, or line number.

Documents, specifications, matrices, reports, and other presentations should be views over the requirements model or generated artifacts derived from it.

## Initial design goals

The requirements source format should eventually support, at minimum:

1. Stable requirement identity.
2. Human-facing requirement identifiers.
3. Requirement title and normative statement.
4. Optional rationale.
5. Only durable structured attributes demonstrated to belong to a requirement; review and workflow state are not presumed intrinsic.
6. Explicit typed relationships between requirements.
7. Human readability without specialized tooling.
8. Clean and comprehensible ordinary Git diffs.
9. Deterministic parsing into a structured semantic representation.
10. Validation by independent tooling.

## Non-goals for the initial investigation

The first phase does not attempt to provide:

- SysML or general systems-modeling support.
- CAD, drawing, or other geometric artifact management.
- A hosted requirements database.
- A custom review queue.
- A mandatory graphical editor.
- A mandatory custom diff renderer.
- Formal tool qualification or certification support.
- A complete requirements-management product suite.

## Architectural principles

### Source is authoritative

The human-readable source files and their Git history are the durable project artifacts. Tool-specific databases and indexes must be treated as derived state.

### Git is version control, not requirements analysis

Git is responsible for recording versions, branches, commits, merges, and history. It is not responsible for determining whether requirements are syntactically valid, semantically consistent, well written, properly traced, or verified.

Those responsibilities belong to separate tools, analogous to parsers, compilers, linters, static analyzers, test frameworks, and documentation generators in software development.

### Specialized tooling is optional enhancement

A project-specific renderer, semantic diff viewer, IDE integration, or traceability browser may substantially improve usability, but raw source must remain useful without them.

### Record syntax, field content, and rendering are distinct

Plain text is a property of the authoritative source, not a commitment to Markdown or another general-purpose markup language. The syntax that identifies requirement objects, fields, and relationships may be separate from notations embedded within field content.

For example, a mathematical requirement may contain an explicitly delimited LaTeX-style expression whose formatted rendering is derived. The canonical expression, its boundaries, and its interpretation must remain visible in source; rendering must not own the requirement or depend on hidden state.

### Prefer iteration over speculative completeness

The project should establish a minimal usable source representation, exercise it in real Git workflows, and add tooling in response to observed friction rather than designing a complete ecosystem before use.

## Evidence-supported direction

Experiment 0001 narrowed the representation without freezing a production
language. [Research 0007](../research/0007-provisional-source-representation-decision.md)
records that evidence and rationale. Later experiments established
deterministic interpretation, transferability, comments, independent
conformance, maintained native tools, bounded operational scale, and explicit
dispositions for the principal model-pressure questions. The [end-to-end pilot
decision](../research/0032-end-to-end-pilot-decision.md) is the current summary
of what that evidence does and does not support.

The current direction is:

- Continue with the purpose-built, keyword-based requirement record form tested in Candidates A and B.
- Carry the authoritative human-facing ID inside the record.
- Permit one or more complete records per source file while treating filenames, paths, file counts, and record positions as non-semantic.
- Preserve the separation between requirement objects and authored order or composition, without yet selecting a view language.
- Do not use a Markdown document containing authoritative embedded records as the initial storage representation.
- Rely on ordinary Git merging; the experiment does not justify semantic merge machinery.
- Treat Git commits and annotated tags as the initial baseline mechanism rather than adding intrinsic baseline or per-requirement revision fields.
- Keep review status, approval, retirement state, timestamps, and change justification outside requirement records unless later workflows demonstrate durable requirement semantics.
- Treat decomposition completeness as policy analysis rather than universal syntax validity.
- Treat verification planning as relationships between requirement revisions and separately identified activities rather than repeated workflow fields intrinsic to requirement objects; a verification-plan syntax remains deferred.

These remain the project foundation. [Specification
0002](0002-minimum-source-language-and-model.md) records the model and design
rationale. The normative grammar, discovery, cardinality, validation, and
mathematical-content rules are stated by the current [0.2 language
standard](0005-mundanereq-source-language-0.2.md) and its [provisional
contract](0006-provisional-0.2-contract.md).

## Current dispositions and reopening questions

- **Identity:** 0.2 retains the human-facing ID as sole identity. Reopen this
  only for an independently baselined consumer that needs pre-exchanged
  continuity across ID correction. [Decision](../research/0023-identity-continuity-decision.md)
- **Verification:** activity, plan, coverage, execution, evidence reference,
  and result are separate companion concepts. Their stable carrier and
  satisfaction policy remain deferred. Experiment 0024 now justifies a focused
  analyzer-contract experiment, not requirement syntax.
  [Decision](../research/0032-end-to-end-pilot-decision.md)
- **Safety and criticality:** contextual classifications belong to a
  baseline-bound assessment assertion, not an intrinsic requirement field.
  Stable carrier and scheme policy remain deferred.
  [Decision](../research/0025-safety-classification-ownership-decision.md)
- **Allocation:** retain the optional opaque label and keep allowed values in
  project policy. Reopen referential target identity for a demonstrated rename
  continuity or multi-target responsibility workflow.
  [Decision](../research/0027-allocation-model-decision.md)
- **Glossary and symbols:** use authoritative prose, ordinary search, and
  adjacent local mathematical definitions. Reopen a companion model only when
  those mechanisms make continuity or impact ambiguous.
  [Decision](../research/0028-glossary-and-symbol-decision.md)
- **Trace policy:** source conformance remains separate from scoped coverage
  and cycle policy. No reusable policy language or fourth tool is selected.
  [Decision](../research/0029-trace-policy-decision.md)
- **Views:** authored ordering and composition remain conditional on a concrete
  delivery or review workflow. [Task](../roadmap/task-0807-test-authored-views-and-specifications.yaml)
- **Mathematics:** LaTeX remains an opaque, explicitly delimited source block.
  A constrained profile requires a concrete analyzer or renderer need.

The principal unresolved maturity questions are empirical rather than missing
grammar: independent human authoring, acceptance in normal human review, use in
a real formal-traceability workflow, and sustained operation by a systems-
engineering team.
