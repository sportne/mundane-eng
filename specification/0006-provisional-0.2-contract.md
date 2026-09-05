# Specification 0006: Provisional 0.2 Contract

Status: Provisional trial contract

Contract identifier: `mundanereq-source-0.2`

Contract baseline: annotated Git tag `provisional-0.2`

## Mission

mundanereq exists to make requirements durable, human-readable source artifacts that work naturally with ordinary Git workflows and independent, composable tools.

## Intended audience and success threshold

The provisional contract is for a systems-engineering team that needs formal traceability and is willing to trial text-first requirements alongside normal editor, Git, forge, and CI practices.

A trial succeeds when the team can read and edit the authoritative source without mundane-req-specific software, review meaningful ordinary diffs, validate the source set deterministically, follow decomposition traces, and reproduce named baselines from Git.

It is not yet a claim of operational scale, certification suitability, ReqIF compatibility, or language stability.

## Contract surface

### Authoritative requirement source

The `.mreq` source language, source-set selection, semantic model, and validity rules are normatively defined by the [mundanereq Source Language Specification 0.2](0005-mundanereq-source-language-0.2.md). [Specification 0002](0002-minimum-source-language-and-model.md) remains the design record and rationale. For the 0.2 contract:

- complete requirement records are authoritative;
- the human-facing ID is the only requirement identity;
- file paths, file boundaries, and record positions are non-semantic;
- `title` and `statement` are required;
- `allocation`, `rationale`, and `source` are optional;
- zero or more `decomposes` relationships may point from a lower-level requirement to a higher-level requirement;
- mathematical `latex` payloads are opaque field content;
- full-line `#` author comments may occur at defined structural positions and have no semantic value or attachment;
- a source set must pass the physical, syntactic, identity, and referential rules in the language standard.

No renderer, database, index, manifest, or repository configuration is needed to understand or select explicitly supplied source.

### Baselines and revisions

A requirement revision is its state in a repository snapshot. A baseline is a Git commit given durable project meaning, normally through an annotated tag and documented project convention.

The existence of a tag does not itself imply review, approval, authority, or certification. Those are project-governance facts about the snapshot.

### Verification planning

Planned verification relates a requirement revision to a separately identified activity. Activity execution, evidence, and pass/fail results are distinct from planning coverage and are not requirement fields.

The 0.2 contract fixes this conceptual ownership but does not define verification-plan syntax.

### Optional derived query

An implementation may derive incoming and transitive decomposition traces. The reference probe's `--incoming ID` operation returns one deterministic shortest child-to-parent path for every reachable incoming requirement.

Reverse links are derived and must not be copied into authoritative requirement records merely to serve the query.

## Source and interpreter conformance

### Conforming source set

A source set conforms to `mundanereq-source-0.2` when it satisfies the mundanereq Source Language Specification 0.2 exactly. Conformance does not assert prose quality, allocation-vocabulary validity, decomposition completeness, approval, or verification coverage.

The fixtures under [`conformance/0.2`](../conformance/0.2/README.md) provide a small independently runnable example. The valid fixture and expected normalized inventory are normative examples of semantic interpretation. Invalid fixtures are representative rather than an exhaustive replacement for the specification.

### Conforming interpreter

A conforming 0.2 interpreter must:

1. accept every conforming source set;
2. reject every source set that violates the language standard;
3. produce the specified semantic values independently of file traversal order and LF/CRLF choice;
4. reject duplicate identities and dangling decomposition targets across the complete selected set;
5. report a source file, one-based line and column, and understandable failure category for each reported diagnostic;
6. preserve opaque mathematical payload characters and line-break structure after CRLF normalization and the specified structural de-indentation;
7. accept permitted source comments while omitting them from the semantic model;
8. expose enough semantic output to compare the conformance inventory, whether or not it uses the reference textual inventory format.

Diagnostic code strings and exact wording are not standardized in 0.2. A tool may stop after the first syntax diagnostic in a file, but source-set diagnostics should not depend on hidden repository state.

### Reference probe

The reference experiment implementation is dependency-free Java 21 and builds as a GraalVM native executable:

    cd experiments/0002-deterministic-interpretation
    make test
    make native

Validation:

    build/mundanereq FILE_OR_DIRECTORY...

Normalized semantic inventory:

    build/mundanereq --inventory FILE_OR_DIRECTORY...

Incoming trace query:

    build/mundanereq --incoming REQUIREMENT_ID FILE_OR_DIRECTORY...

The CLI and implementation language are reference tooling, not authoritative-source requirements. Another implementation may expose a different interface while conforming to the same source contract.

## CI trial guidance

A repository trial should run source validation from a clean checkout using explicit requirement roots. The validation job should fail on nonzero exit status and retain plain diagnostics in the ordinary job log.

The trial should not commit the native executable, normalized inventory, indexes, rendered output, or databases unless a separate delivery requirement makes one of those artifacts authoritative. Rebuilding derived output must remain possible from the tagged source snapshot.

Projects may add policy checks for allowed allocation labels, required decomposition coverage, baseline naming, or verification planning. Such failures should be identified as policy diagnostics rather than 0.2 source-language errors.

## Compatibility policy

The `0.2` contract is provisional and intentionally small.

Every conforming 0.1 source set is conforming 0.2 source with the same semantic interpretation. Version 0.2 adds only nonsemantic source comments; source containing those comments is not conforming 0.1 source.

- Clarifications that do not change accepted source or semantic interpretation may update the documentation under the same contract identifier.
- A change that accepts additional syntax without changing existing interpretation requires a later provisional minor contract.
- A change that rejects previously conforming 0.2 source or changes its semantic interpretation requires a new contract identifier.
- Tools should state which contract they implement; source files do not contain an embedded version directive in 0.2.
- No backward-compatibility promise extends to experimental CLI text, Java APIs, document structure, or deferred companion languages.
- Before a stable 1.0 contract, incompatible provisional versions may exist, but each must remain recoverable by its Git tag and written specification.

The absence of a source-level version marker remains deliberate. The repository-level [`VERSION`](../VERSION) file or equivalent project/tool configuration selects the current contract without adding a directive to every source file or record. A more elaborate selection mechanism is deferred until incompatible versions must coexist in one working source set.

## Explicitly outside the 0.2 contract

- authored document views and ordering;
- arbitrary attributes and configurable schemas;
- controlled allocation vocabularies;
- glossary or formal-symbol-table syntax;
- approval, signatures, review workflow, and access control;
- verification-plan, execution, result, and evidence syntax;
- persistent indexes or servers;
- semantic merge and semantic diff;
- cross-repository references and package management;
- variants and reuse mechanisms;
- ReqIF import, export, or round-trip behavior;
- formal interpretation of prose, LaTeX, or other embedded notation;
- durable requirement remarks or annotations;
- production packaging, support, or certification claims.

## Trial checklist

Before evaluating the 0.2 contract, a team should:

1. encode a bounded but representative corpus;
2. declare requirement roots and any project policy separately;
3. validate from a clean checkout;
4. review at least one normative change, comment-only change, relationship change, file move, and concurrent edit through ordinary Git diffs;
5. establish and compare two annotated baselines;
6. inspect incoming traces for a changed higher-level requirement;
7. record information that could not be represented without distortion;
8. decide whether observed friction belongs to source, policy, Git/forge workflow, or an independent tool.

## Stability disposition and residual evidence

The [source 1.0 readiness
audit](../research/0031-source-1.0-readiness-audit.md) finds no demonstrated need
to expand the 0.2 requirement grammar before a stability decision. Identity,
verification, safety assessment, allocation, glossary and symbols, and trace
policy have explicit dispositions. Their deferred companion carriers are not
missing intrinsic requirement fields.

The only identified 1.0 candidate remains a no-feature, semantics-identical
successor to this contract. [TC-1002](../roadmap/closed/task-1002-define-compatibility-and-publish-or-defer-1.0.yaml)
deferred publication on 2026-08-30 because independent-human authoring, normal
human review, and realistic workflow evidence were insufficient for a durable
compatibility promise. [Experiment 0024](../experiments/0024-vaccine-monitoring-pilot/assessment.md)
subsequently exercised a realistic self-run formal-traceability workflow and
retained the requirement grammar unchanged. It does not supply the independent-
human evidence or authorize 1.0 publication. The formatter and native
verification gate is passing.

The following remain future evidence or independently versioned work rather
than prerequisites for the 0.2 semantic model:

- sustained use by a real systems-engineering team on a larger corpus;
- incompatible source-version coexistence if a real migration requires it;
- stable verification, assessment, allocation, glossary, symbol, view, or
  project-policy companion carriers; the verification workflow now justifies a
  bounded analyzer-contract experiment but not a standardized carrier;
- cross-tool ReqIF fidelity, update behavior, and identity continuity beyond
  Experiment 0006's bounded semantic self-roundtrip; and
- broader native platform and operational support evidence.
