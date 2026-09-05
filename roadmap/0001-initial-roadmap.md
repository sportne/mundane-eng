# Roadmap 0001: A Composable Engineering Tooling Monorepo

Status: Draft living roadmap

Last reconciled: 2026-09-05 after YAML work-item migration

Execution is decomposed into the [task-card index](0002-task-card-index.md).
This remains the single strategic roadmap; cards describe bounded work and
acceptance evidence. Stage numbers group work and preserve existing task IDs;
the execution order identifies dependencies and opportunities for parallel work.

## Purpose

Develop a text-oriented engineering tooling ecosystem in this monorepo, with
Mundane-Req as its first independently usable language and tool component.
Human-authored requirements, assertions, and project declarations remain
authoritative. Compiled artifacts, indexes, analyses, and views are derived.

Maintained requirement and plan compilers, explicit local linking and a focused
analyzer now support this loop; the report remains a bounded experiment:

```text
requirements source -> requirement compilation ----+
                                                   +-> explicit linking
verification plan source -> plan adapter/compiler -+       |
                                                           v
                                              coverage/staleness analysis
                                                           |
                                                           v
                                              reproducible verification report
                                                           |
                                              source edit and recorded rebuild
```

"Compile" means interpret source into a documented semantic representation with
diagnostics and provenance. "Link" means resolve explicit references and check
their selected contracts. Domain tools interpret relationship consequences;
reference reachability alone establishes neither invalidity nor satisfaction.

## Product direction

One repository can coordinate contracts and consumers without requiring one
language, executable, shared version number, or universal engineering metamodel.
YAML is independently selected for requirements and work items. Each other artifact's authoring format
remains a separate workflow decision, including use of existing formats or native
engineering files. Common integration concerns compiled interfaces, explicit
references, provenance and linking. Neither shared YAML notation nor a common
source schema is an ecosystem requirement.
The initial responsibilities to investigate are:

| Responsibility | Boundary to preserve |
| --- | --- |
| Requirements | Source language, validation, formatting, decomposition analysis, and compiled semantic output |
| Artifact integration | Only demonstrated import, qualification, revision, provenance, and linking contracts |
| Verification | The pilot-selected plan, coverage, and staleness workflow |
| Views | Reproducible presentation over selected artifacts and explicit analysis results |
| Examples and integration checks | Bounded workflows that expose accidental coupling and incomplete results |

TC-1101 selected logical boundaries and retained the current physical layout.
TC-1104 remains conditional on moves that improve a checkable boundary. Requirements
commands, including compilation, remain usable without verification or view tools.

This is the tooling monorepo. Users may author engineering information in one
repository or several; the first import contract tests both with local fixtures.
Existing formats may be adapted, and binary/native engineering files may remain
authoritative where practical. Additional safety, BOM, source-code, and CAD
integrations need concrete workflows before acquiring implementations.

## Current evidence and gaps

Default invocation retains source 0.2 with prior 0.1 conformance fixtures. Explicit
requirements YAML 0.3 has a normative schema and semantic contract. Java 21/GraalVM
supplies validate/format/trace tools, a migration utility and an independent
requirements compiler. Separate native plan, linking and verification commands
consume published serialized interfaces. The existing
lossless source representation preserves comments; normalizedInventory is a
testing utility rather than a public compiled-artifact contract.

- [The pilot](../experiments/0024-vaccine-monitoring-pilot/assessment.md)
  exercised 57 requirements and two baselines. Its
  [decision](../research/0032-end-to-end-pilot-decision.md) selected verification
  planning because manual coverage and coarse revision binding caused friction.
- The former formatter-inventory gap was repaired in commit 42e5082. The latest
  pilot closeout extended documented coverage to 30 source sets and 64 files.
  The diagnostics batch adds the SARIF example for 31 source sets and 65 files.
  The old audit finding is not an outstanding formatter-corpus task.
- Existing tests already cover formatter idempotence, semantic preservation,
  comment retention, and selected output/replacement failures. New work extends
  those checks rather than reopening completed cards.
- Formatter write-back now checks snapshot bytes and available file identity;
  detected external edits survive. Partial failures identify completed and remaining
  paths. All commands check stdout/stderr delivery; a rename race remains documented.
- Bounded parser recovery retains reliable neighboring records and suppresses
  uncertain reference errors. Malformed YAML and ambiguous math boundaries remain
  incomplete. Formatter decoding is still repeated; no unrelated refactor was needed.
- SARIF output preserves rule IDs, source-point coordinates and incomplete-analysis
  status. Diagnostic end spans remain unavailable rather than being invented.
- CI runs the complete Makefile verification gate and checks deliberate failure
  propagation in a recorded Ubuntu 24.04 environment. Current version identifiers
  now come from versions.properties,
  with separate source, command, package and compiled-format domains.
- Linux packaging already supplies checksums, notices, environment information,
  and isolation checks. Existing evidence is source-reproducible, not a claim of
  byte-identical native binaries.
- ReqIF remains a bounded experimental self-roundtrip. External-tool evidence is
  conditional on availability. Human usability and broader operational evidence
  remain unestablished; their absence is not a dependency for this backlog.

The [completed YAML batch](../research/0035-yaml-requirements-batch-verification.md)
records current source, migration, safety and verification evidence.
The [compiled-requirements batch](../research/0041-compiled-requirements-verification.md)
adds the documented compiler boundary, retained source spans and a serialized-only
consumer check. Maintained local imports, TSV plans and review-basis comparisons
now have contracts and implementations. Project attribute schemas remain unresolved.
Current paths are retained. Normative source and output contracts live in specification/.

## Stage 11 — Monorepo and ownership decisions

TC-1101 and TC-1102 are complete: logical component boundaries retain independent
requirements use, while contextual assessments and verification plans retain their
own authority and revisions. No physical reorganization is currently justified;
[TC-1104](task-1104-establish-monorepo-component-layout.yaml) remains Conditional.

The completed YAML chain TC-1105–TC-1109 supplies maintained YAML 0.3 with explicit
selection and default custom 0.2 preservation. TC-1103 used that interpreter and
the existing experimental TSV plan carrier in a 57-requirement, 13-case experiment.
It demonstrated missing/ambiguous references, context, exact source versus semantic
revision comparison, incomplete input rejection and identical clean rebuilds.
TC-0905 independently selected bounded TSV plan tables with explicit context and
baseline/current scopes.

## Stage 12 — Publish bounded compiler and import interfaces

[TC-1201](closed/task-1201-define-requirement-semantic-output.yaml) completed versioned
requirement semantics, source ranges, diagnostic meanings, provenance, ordering,
and completeness; [TC-1202](closed/task-1202-emit-compiled-requirement-artifacts.yaml)
implemented the selected interface as mundanereq-compile.

[TC-1203](closed/task-1203-define-import-and-reference-contracts.yaml) completed explicit
import selection, qualification, target kinds, revision binding, and failure
behavior. Qualification preserves human-authored requirement IDs. The design
distinguishes relationship cycles from build cycles and describes partial input
without presenting it as fully analyzed.

[TC-0905](closed/task-0905-define-verification-analyzer-contract.yaml) completed the
verification-plan contract and staleness decisions after those designs.
[TC-1204](closed/task-1204-implement-bounded-artifact-linking.yaml) implemented the
bounded resolver, including exact input pins and incomplete-input rejection.
[TC-0904](closed/task-0904-implement-the-selected-ecosystem-tool.yaml) implemented plan
compilation and the focused verification consumer, with 57 planned assertions and
two stale statement bindings in the checked-in change case.

The experiment compared exact Git tree binding with per-requirement semantic values.
Compiled output records SHA-256 of exact input bytes for provenance; it has no public
per-requirement fingerprint. TC-0905 selected normalized requirement-value
comparison; source moves and comments do not create review-stale bindings. Human
IDs remain identity and digests do not enter authored requirement records.

## Stage 13 — Implement the selected project attributes incrementally

[TC-1301](closed/task-1301-classify-project-attribute-use-cases.yaml) and
[TC-1302](closed/task-1302-decide-project-attribute-schemas.yaml) are complete.
[Research 0051](../research/0051-project-attribute-use-case-decision.md) selects
text/enumeration, required/optional single values and no defaults for descriptive
annotations. Independently revised assessments, verification results and contextual
allocations remain outside the requirement. Built-in allocation/source stay valid.

[Research 0052](../research/0052-project-attribute-schema-decision.md) selects a
checked-in narrow JSON declaration passed explicitly to requirements commands,
and YAML requirement values under an opt-in future source profile. Other artifact
formats remain independent. No discovery, schema merging or default values are
introduced. Current normative schemas, version constants and commands are unchanged;
[worked design cases](../research/0053-project-attribute-design-cases.md) are expected
outcomes for future tests, not claims of implemented support.

Execute [TC-1303](closed/task-1303-validate-project-defined-requirement-attributes.yaml) for
contracts/model/schema validation first. Then
[TC-1304](closed/task-1304-preserve-attributes-in-formatting-and-trace.yaml) supplies safe
formatting/trace independently of
[TC-1305](task-1305-compile-project-attribute-artifacts.yaml)'s versioned output.
[TC-1306](task-1306-link-and-analyze-project-attributes.yaml) validates serialized
imports and compares explicit values plus schema definitions; changes must be
visible rather than silently ignored.
[TC-1307](task-1307-display-project-attributes-in-derived-reports.yaml) extends the
existing experimental report and records unsupported interchange boundaries.
[TC-1308](task-1308-verify-and-document-project-attribute-workflows.yaml) closes the
integration loop with reproducible examples, compatibility checks and migration
notes; each implementation supplies its own tests before completion.

Compiled output sorts attribute maps; authored YAML keeps its comments and key
order under the conservative formatter. Existing custom/YAML projects remain
valid without opting into attributes. Editor assistance requires its own selected
host/workflow, and external ReqIF fidelity remains conditional under TC-0902.

## Stage 14 — Address current correctness independently

TC-1401 through TC-1403 are complete:

- [TC-1401](closed/task-1401-protect-formatter-write-back.yaml): detect intervening edits
  and document recoverable multi-file outcomes, including remaining filesystem races.
- [TC-1402](closed/task-1402-report-cli-output-failures.yaml): avoid false success on
  stdout/stderr failures and retain focused existing command behavior.
- [TC-1403](closed/task-1403-recover-parser-diagnostics-safely.yaml): recover independent
  errors without inventing complete valid models or misleading cross-file findings.

Separate decoding, syntax, semantics, and validation only where it produces an
observable improvement. TC-1202 retained one interpretation and source spans; repeated formatter decoding
was inspected and left unchanged because the emitter did not require a refactor. Further performance work requires a profile and a
concrete consumer, not a general cleanup objective.

## Stage 15 — Extend repeatable checks and contributor integration

[TC-1502](closed/task-1502-centralize-version-declarations.yaml) completed authoritative current version
declarations while preserving independent source/tool/package/format identifiers and
experimental migration policy.
[TC-1503](closed/task-1503-align-ci-with-authoritative-verification.yaml) closed the
gap between hosted checks and make verify, with clean-run and deliberate-failure
evidence.

The first report loop is complete.
[TC-1501](closed/task-1501-extend-artifact-workflow-regression-corpora.yaml) adds five
source-rebuilt report corpora, 12 replayable seeds in both source modes and six
isolated implementation mutations. Every implementation card still supplies its
own tests. [TC-1504](closed/task-1504-emit-sarif-validation-diagnostics.yaml) adds
schema-validated SARIF with source-accurate points and tested output failure behavior.

Reproduce existing package checks rather than commissioning new packaging work
without a demonstrated need. Document tested Java, GraalVM, native-toolchain,
OS and architecture assumptions. Future checksums/provenance/platform additions
must name a current missing capability and measured evidence.

## Views and specifications

[TC-0903](closed/task-0903-run-a-derived-presentation-experiment.yaml) completed a
deterministic report over the analyzer, recording input revisions and completeness
and linking findings to source. Its repeatable review workflow justified retaining
the experimental renderer but did not demonstrate a need for authored composition.

[TC-0807](task-0807-test-authored-views-and-specifications.yaml) is a conditional
successor if the report demonstrates a need for authored composition. Source
definitions can own selection and ordering; generated report content remains
derived. Issued reports may be retained as delivery records with separate
approval provenance, never as an editable competing source of requirement facts.

[TC-0902](task-0902-run-an-independent-reqif-roundtrip.yaml) retains its existing
conditional external-tool experiment. Editor highlighting, inline diagnostics,
navigation, formatter integration, attribute hover/completion, and Unicode
confusable/invisible-character diagnostics remain candidates for bounded follow-up
cards after their contracts and authoring cases exist. These are individual
capabilities; the roadmap does not require a single large language-server task.

## Immediate execution order

```text
TC-1101 -> TC-1102 -> TC-1103 -> TC-1201 -> TC-1203 -> TC-0905
   |            |                 |                       |
   v            v                 v                       |
TC-1104?     TC-1301           TC-1202 ---------------------+
                |                                         v
                +-- (+ TC-1203) -> TC-1302             TC-1204
                                                          |
                                                          v
                                        TC-0904 -> TC-0903 -> TC-1501
                                                       |
                                                       v
                                                   TC-0807?

Ready: TC-1303; TC-1301 and TC-1302 are complete
TC-1302 -> TC-1303 -> TC-1304 ----------------------------+
                  +-> TC-1305 -> TC-1306 -> TC-1307 ------+-> TC-1308
Complete: TC-1101, TC-1102, TC-1103, TC-1502, TC-1201, TC-1202,
          TC-1203, TC-0905, TC-1204, TC-0904, TC-0903, TC-1503,
          TC-1403, TC-1504, TC-1501
TC-1105 (complete) -> TC-1103
Completed YAML chain: TC-1105 -> TC-1106 -> TC-1107 -> TC-1108 -> TC-1109
TC-1106 -> TC-1302
TC-1502 (complete) -> TC-1201 (complete)
TC-1201 + TC-1402 + TC-1403 -> TC-1504
TC-0902? remains independent of the main chain
```

A question mark marks a conditional card. All dependencies are enumerated in
the index, including completed evidence prerequisites. If a conditional design
selects a smaller boundary, revise downstream dependencies explicitly rather than
performing unjustified work. TC-1104's optional moves need coordination with
active code changes but do not block fixes in the existing layout.

## Cross-cutting rules for every stage

1. Authoritative source and human requirement IDs remain sufficient for understanding.
2. Compiled semantics, indexes, and reports have documented provenance and regeneration.
3. Independent components are tested through published artifact interfaces as well
   as any justified shared implementation.
4. Parsing, linking, domain analysis, and reporting have distinct completeness and
   failure rules; generation is not approval or satisfaction.
5. Source/CLI/package/compiled-format versions remain independently identified.
6. Decisions record valid/invalid examples, alternatives, and stop conditions.
7. Verification uses checked-in corpora, golden/adversarial fixtures, seeded
   generation, mutation, existing compatibility cases, focused dogfooding, and
   reproducible clean builds. Optional contributor sessions supplement that evidence.
8. New capabilities enter through demonstrated workflows and small reviewable cards.

## Historical evidence and superseded planning

Completed cards retain their IDs, paths, and recorded outcomes in the index.
TC-1001 and TC-1002 are completed historical decisions; their former future
decision sequence is superseded by Stages 11–15. TC-1003 remains completed pilot
evidence for TC-0905. No completed evidence card is relabeled as unfinished work.

The former Stage 7 field-use and separately implemented-parser expectations are
replaced in active planning by the bounded checks above. Historical evidence
limitations remain facts about those experiments.

The following anchors preserve links from completed cards and research. Their
historical work is summarized here; the current task dependencies are above.

<a id="first-implementation-hypothesis"></a>
<a id="stage-1--establish-an-exact-02-implementation-baseline"></a>
<a id="stage-2--extract-the-smallest-shared-foundation"></a>
<a id="stage-3--deliver-mundanereq-validate"></a>
<a id="stage-4--define-and-deliver-mundanereq-format"></a>
<a id="stage-5--define-and-deliver-mundanereq-trace"></a>
<a id="stage-6--exercise-the-tools-in-concert"></a>
<a id="stage-7--test-operational-use-and-scale"></a>

Stages 1–7 supplied conformance repair, the maintained Java/source foundation,
independent native commands, formatting/trace evidence, packaging, controlled
workflows, and bounded scale. See the completed-card inventory for exact results.

<a id="identity-continuity"></a>
<a id="verification-planning-and-evidence"></a>
<a id="safety-classification-and-other-assessments"></a>
<a id="allocation-and-controlled-vocabulary"></a>
<a id="glossary-and-formal-symbols"></a>
<a id="trace-policy"></a>

Stage 8's studies retain human ID identity, independently owned verification and
safety assertions, an opaque allocation label, and project-specific trace policy.
Their recorded decisions inform TC-1102 and TC-1301.

<a id="stage-9--add-focused-ecosystem-tools-only-where-use-justifies-them"></a>
<a id="derived-presentation-milestone"></a>
<a id="stage-10--decide-whether-to-stabilize-10"></a>

The initial ecosystem prioritization was followed by the completed audit and
decision records and the two-baseline pilot. This roadmap replaces their
prospective gates with the incremental workflow above while preserving the
[closed cards](0002-task-card-index.md#completed-evidence).

## Stage 16 — Issue and task artifacts

The selected batch specifies and implements a bounded source-to-compiled-to-linked
work-item workflow, using this repository’s cards as the migration corpus.
[TC-1601](closed/task-1601-define-work-item-ownership.yaml) through
[TC-1606](closed/task-1606-dogfood-the-work-item-backlog.yaml) preserve human IDs, authored
status and completion evidence; generated indices are derived. Issue/task source
notation is selected independently of requirements YAML.

TC-1601–1606 are complete. [Work-item commands](../distribution/work-items.md) now
compile, link and render the repository backlog; the [derived index](../WORK-ITEMS.md)
replaces manual status tables. The attribute implementation chain remains available
independently.

## YAML work-item continuation

TC-1607–1609 select and implement a work-item-specific YAML specification/schema
and migrate the authoritative backlog. Literal body strings retain Markdown prose
without defining machine relationships. Compiled work-item imports, analysis and
derived views keep their established meanings; the old source remains a compatibility
profile. Other artifact formats remain independent decisions.
