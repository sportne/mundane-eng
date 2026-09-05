# Roadmap Task-Card Index

Status: Active backlog

Strategic source: [Roadmap 0001](0001-initial-roadmap.md)

## Purpose

This index decomposes the strategic roadmap into bounded, independently
reviewable task cards. Cards are planning, not additions to the source-language
specification. The current tranche contains 28 new cards and three reconciled
verification/report cards; two existing experiments remain conditional.

## Status vocabulary

- **Ready:** known dependencies are complete; work may begin.
- **Planned:** accepted work with incomplete dependencies.
- **Conditional:** perform only when its stated decision or prerequisite justifies it.
- **In progress:** actively being executed.
- **Complete:** acceptance evidence and the completion decision are recorded.
- **Superseded:** replaced by a later card or roadmap decision, with the replacement identified.

Only recorded evidence and decisions complete a card. If future work is
superseded, record its replacement, retain its ID, and keep a closed-card link.
Completed historical cards retain Complete and their original evidence; a
superseded prospective roadmap sequence does not undo their completed work.

## Dependency shape and priority

The YAML/safety, compiled-requirements, verification-workflow, and diagnostics
batches are complete. TC-1301/1302 have selected descriptive text/enum attributes
and explicit project schemas. TC-1303 (validation) is Ready; TC-1304–1308 are the
bounded implementation and verification successors. The graph retains completed prerequisites
for context; active tables contain only remaining work.

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

TC-1105 (complete) -> TC-1103
Completed YAML chain: TC-1105 -> TC-1106 -> TC-1107 -> TC-1108 -> TC-1109
TC-1106 -> TC-1302
TC-1502 (complete) -> TC-1201 (complete)
TC-1201 + TC-1402 + TC-1403 -> TC-1504
TC-1401–1403 and TC-1501–1504 are complete; TC-0902? remains conditional.

Attribute continuation (TC-1301/1302 complete):
TC-1302 -> TC-1303 -> TC-1304 ----------------------------+
                  +-> TC-1305 -> TC-1306 -> TC-1307 ------+-> TC-1308
```

Question marks mark conditional work. Tables include all prerequisites, including
completed evidence. TC-1104 is optional layout work, not a gate for fixes in the
current layout. TC-1101–1103, TC-1201–1204, TC-0903–0905, TC-1401–1403 and TC-1501–1504 are complete.
Implementations include their own tests; completed TC-1501 extends the integrated
corpus without replacing those owning suites. If a conditional card is superseded, revise its
consumers' dependencies explicitly before proceeding.

## Stage 11: Monorepo and ownership decisions

| Card | Outcome | Status | Depends on |
| --- | --- | --- | --- |
| [TC-1104](task-1104-establish-monorepo-component-layout.md) | Establish the Monorepo Component Layout | Conditional | TC-1101 |

The completed import contract precedes the plan contract, whose fixtures drive
the maintained linker and analyzer. Their completed cards are in the evidence
inventory below; TC-1501 extends that integrated workflow.

## Stage 13: Project attribute implementation

| Card | Outcome | Status | Depends on |
| --- | --- | --- | --- |
| [TC-1303](task-1303-validate-project-defined-requirement-attributes.md) | Validate Project-Defined Requirement Attributes | Ready | TC-1302 |
| [TC-1304](task-1304-preserve-attributes-in-formatting-and-trace.md) | Preserve Attributes in Formatting and Trace | Planned | TC-1302, TC-1303 |
| [TC-1305](task-1305-compile-project-attribute-artifacts.md) | Compile Project Attribute Artifacts | Planned | TC-1302, TC-1303 |
| [TC-1306](task-1306-link-and-analyze-project-attributes.md) | Link and Analyze Project Attributes | Planned | TC-1302, TC-1305 |
| [TC-1307](task-1307-display-project-attributes-in-derived-reports.md) | Display Project Attributes in Derived Reports | Planned | TC-1302, TC-1306 |
| [TC-1308](task-1308-verify-and-document-project-attribute-workflows.md) | Verify and Document Project Attribute Workflows | Planned | TC-1302, TC-1304, TC-1306, TC-1307 |

The completed [design](../research/0052-project-attribute-schema-decision.md)
selects explicit JSON declarations, YAML requirement values, no defaults and
separate authority for contextual assessments. These are future capabilities;
current source and output contracts remain unchanged. Editor assistance and ReqIF
mapping are not implicitly implemented by this chain.

Completed diagnostic and workflow work is recorded in the evidence inventory
below. Existing package, compatibility and owning regression checks remain part
of the authoritative gate; additional work needs a concrete new use case.

## Stage 16: Work items as compiled artifacts

Task cards and issues have an immediate consumer in this repository.
Execute TC-1601 -> TC-1602 -> TC-1603 -> TC-1604 -> TC-1605 -> TC-1606.
This chain is independent of the attribute implementation chain.

| Card | Outcome | Status | Depends on |
| --- | --- | --- | --- |
| [TC-1601](closed/task-1601-define-work-item-ownership.md) | Define Work Item Ownership | Complete | TC-1102, TC-1203 |
| [TC-1602](task-1602-specify-work-item-source-and-output.md) | Specify Work Item Source and Output | Ready | TC-1601 |
| [TC-1603](task-1603-compile-and-validate-work-items.md) | Compile and Validate Work Items | Planned | TC-1602 |
| [TC-1604](task-1604-link-and-analyze-work-items.md) | Link and Analyze Work Items | Planned | TC-1603 |
| [TC-1605](task-1605-generate-derived-work-item-views.md) | Generate Derived Work Item Views | Planned | TC-1604 |
| [TC-1606](task-1606-dogfood-the-work-item-backlog.md) | Dogfood the Work Item Backlog | Planned | TC-1603, TC-1604, TC-1605 |

## Conditional existing work

| Card | Outcome | Status | Depends on |
| --- | --- | --- | --- |
| [TC-0807](task-0807-test-authored-views-and-specifications.md) | Authored composition decision if simple reporting is insufficient | Conditional | TC-0603, TC-0903; demonstrated composition need |
| [TC-0902](task-0902-run-an-independent-reqif-roundtrip.md) | External-tool ReqIF fidelity evidence | Conditional | TC-0603; access to an independent implementation |

## Completed evidence

The rows below are historical results, not the current execution sequence.
TC-1001/TC-1002 remain completed records; their prospective decision sequence is
superseded by Stages 11–15. Their filenames and evidence are preserved.

| Card | Outcome | Status | Depends on |
| --- | --- | --- | --- |
| [TC-0101](closed/task-0101-strengthen-unicode-conformance-fixtures.md) | Unicode edge-case fixtures | Complete | — |
| [TC-0102](closed/task-0102-correct-reference-parser-conformance.md) | Known parser deviations repaired | Complete | TC-0101 |
| [TC-0103](closed/task-0103-audit-the-0.2-conformance-baseline.md) | Audited 0.2 implementation baseline | Complete | TC-0101, TC-0102 |
| [TC-0104](closed/task-0104-select-the-maintained-implementation-lineage.md) | Probe-evolution decision | Complete | TC-0103 |
| [TC-0201](closed/task-0201-establish-the-maintained-java-project.md) | Maintained Java/GraalVM project skeleton | Complete | TC-0104 |
| [TC-0202](closed/task-0202-design-the-shared-source-representation.md) | Shared physical and concrete source model | Complete | TC-0201 |
| [TC-0203](closed/task-0203-extract-the-semantic-parser-and-diagnostics.md) | Shared semantic parser, discovery, and diagnostics | Complete | TC-0202 |
| [TC-0204](closed/task-0204-port-tests-and-prove-native-tool-boundaries.md) | Regression coverage and three native boundaries | Complete | TC-0203 |
| [TC-0301](closed/task-0301-implement-the-validator-executable.md) | `mundanereq-validate` implementation | Complete | TC-0204 |
| [TC-0302](closed/task-0302-verify-validator-behavior.md) | Validator conformance and CI evidence | Complete | TC-0301 |
| [TC-0303](closed/task-0303-publish-the-validator-trial-contract.md) | Maintained validator trial release | Complete | TC-0302 |
| [TC-0401](closed/task-0401-run-the-formatting-policy-experiment.md) | Selected conservative formatting policy | Complete | TC-0203 |
| [TC-0402](closed/task-0402-implement-the-formatter-executable.md) | `mundanereq-format` implementation | Complete | TC-0401, TC-0204 |
| [TC-0403](closed/task-0403-verify-formatter-safety-properties.md) | Semantic-preservation and idempotence evidence | Complete | TC-0402 |
| [TC-0404](closed/task-0404-publish-the-formatter-trial-contract.md) | Maintained formatter trial release | Complete | TC-0403 |
| [TC-0501](closed/task-0501-define-the-first-trace-interface.md) | Bounded trace questions and output semantics | Complete | TC-0203 |
| [TC-0502](closed/task-0502-implement-the-trace-executable.md) | `mundanereq-trace` implementation | Complete | TC-0204, TC-0501 |
| [TC-0503](closed/task-0503-verify-trace-graph-behavior.md) | Graph and workflow evidence | Complete | TC-0502 |
| [TC-0504](closed/task-0504-publish-the-trace-trial-contract.md) | Maintained trace trial release | Complete | TC-0503 |
| [TC-0601](closed/task-0601-package-and-document-the-native-suite.md) | Installable, independently documented native tools | Complete | TC-0303, TC-0404, TC-0504 |
| [TC-0602](closed/task-0602-create-the-clean-checkout-ci-workflow.md) | Reproducible example Git/CI workflow | Complete | TC-0601 |
| [TC-0603](closed/task-0603-run-the-integrated-toolchain-trial.md) | First toolchain decision record | Complete | TC-0602 |
| [TC-0701](closed/task-0701-select-a-larger-corpus-and-trial-protocol.md) | Licensed corpus and controlled trial plan | Complete | TC-0603 |
| [TC-0702](closed/task-0702-run-the-independent-interpretation-proxy-trial.md) | AI-proxy independent-interpretation evidence | Complete | TC-0701 |
| [TC-0703](closed/task-0703-run-the-multi-author-and-layout-trial.md) | Concurrency and file-granularity evidence | Complete | TC-0701 |
| [TC-0704](closed/task-0704-measure-operational-scale.md) | Operational-scale measurements | Complete | TC-0702, TC-0703 |
| [TC-0705](closed/task-0705-obtain-independent-conformance-evidence.md) | Independent interpretation evidence | Complete | TC-0103, TC-0303 |
| [TC-0706](closed/task-0706-run-the-subagent-author-review-trial.md) | Material scope deviation: two-author/two-reviewer subagent evidence; human evidence absent | Complete | TC-0701, TC-0702 |
| [TC-0707](closed/task-0707-test-bounded-diagnostic-presentation.md) | Large diagnostic-set presentation decision | Complete | TC-0704 |
| [TC-0801](closed/task-0801-test-identity-continuity.md) | ID-correction model decision | Complete | TC-0603 |
| [TC-0802](closed/task-0802-model-verification-planning-and-evidence.md) | Verification companion-artifact decision | Complete | TC-0603 |
| [TC-0803](closed/task-0803-test-safety-classification-ownership.md) | Safety classification ownership decision | Complete | TC-0603 |
| [TC-0804](closed/task-0804-test-allocation-and-controlled-vocabulary.md) | Allocation model decision | Complete | TC-0603 |
| [TC-0805](closed/task-0805-test-glossary-and-formal-symbol-artifacts.md) | Vocabulary and symbol model decision | Complete | TC-0603 |
| [TC-0806](closed/task-0806-test-reusable-trace-policies.md) | Language-versus-policy decision | Complete | TC-0603 |
| [TC-0901](closed/task-0901-prioritize-the-next-ecosystem-tool.md) | Evidence-backed next-tool selection | Complete: no tool selected | TC-0603, TC-0704 |
| [TC-1001](closed/task-1001-audit-readiness-for-1.0.md) | Historical audit; prospective sequence superseded | Complete | Historical evidence prerequisites retained in card |
| [TC-1002](closed/task-1002-define-compatibility-and-publish-or-defer-1.0.md) | Historical decision; prospective sequence superseded | Complete | TC-1001 |
| [TC-1003](closed/task-1003-execute-vaccine-monitoring-requirements-pilot.md) | End-to-end formal-traceability pilot | Complete | TC-1002 |
| [TC-1105](closed/task-1105-compare-yaml-and-custom-requirement-source.md) | Compare YAML and Custom Requirement Source | Complete | — |
| [TC-1106](closed/task-1106-specify-yaml-requirement-source-profile.md) | Specify the YAML Requirement Source Profile | Complete | TC-1105 |
| [TC-1107](closed/task-1107-interpret-and-validate-yaml-requirement-source.md) | Interpret and Validate YAML Requirement Source | Complete | TC-1106 |
| [TC-1108](closed/task-1108-format-yaml-source-without-content-loss.md) | Format YAML Source Without Content Loss | Complete | TC-1107 |
| [TC-1109](closed/task-1109-migrate-yaml-examples-and-conformance-material.md) | Migrate YAML Examples and Conformance Material | Complete | TC-1107, TC-1108 |
| [TC-1401](closed/task-1401-protect-formatter-write-back.md) | Protect Formatter Write-Back | Complete | — |
| [TC-1402](closed/task-1402-report-cli-output-failures.md) | Report CLI Output Failures Consistently | Complete | — |
| [TC-1101](closed/task-1101-define-monorepo-component-boundaries.md) | Define Monorepo Component Boundaries | Complete | — |
| [TC-1102](closed/task-1102-define-requirement-and-assertion-ownership.md) | Define Requirement and Assertion Ownership | Complete | TC-1101 |
| [TC-1103](closed/task-1103-test-compilation-linking-and-rebuilds.md) | Test Compilation, Linking, and Rebuilds | Complete | TC-1102, TC-1105 |
| [TC-1502](closed/task-1502-centralize-version-declarations.md) | Centralize Independent Version Declarations | Complete | — |
| [TC-1201](closed/task-1201-define-requirement-semantic-output.md) | Define Requirement Semantic Output | Complete | TC-1103, TC-1502 |
| [TC-1202](closed/task-1202-emit-compiled-requirement-artifacts.md) | Emit Compiled Requirement Artifacts | Complete | TC-1201 |
| [TC-1203](closed/task-1203-define-import-and-reference-contracts.md) | Define Import and Reference Contracts | Complete | TC-1201 |
| [TC-1204](closed/task-1204-implement-bounded-artifact-linking.md) | Implement Bounded Artifact Linking | Complete | TC-1202, TC-1203, TC-0905 |
| [TC-0905](closed/task-0905-define-verification-analyzer-contract.md) | Define the Verification Analyzer Contract | Complete | TC-0802, TC-1003, TC-1103, TC-1203 |
| [TC-0904](closed/task-0904-implement-the-selected-ecosystem-tool.md) | Implement the Selected Ecosystem Tool | Complete | TC-0905, TC-1204 |
| [TC-0903](closed/task-0903-run-a-derived-presentation-experiment.md) | Run a Derived Presentation Experiment | Complete | TC-0904 |
| [TC-1503](closed/task-1503-align-ci-with-authoritative-verification.md) | Align CI with Authoritative Verification | Complete | — |
| [TC-1403](closed/task-1403-recover-parser-diagnostics-safely.md) | Recover Parser Diagnostics Safely | Complete | — |
| [TC-1501](closed/task-1501-extend-artifact-workflow-regression-corpora.md) | Extend Artifact Workflow Regression Corpora | Complete | TC-0903 |
| [TC-1504](closed/task-1504-emit-sarif-validation-diagnostics.md) | Emit SARIF Validation Diagnostics | Complete | TC-1201, TC-1402, TC-1403 |
| [TC-1301](closed/task-1301-classify-project-attribute-use-cases.md) | Classify Project Attribute Use Cases | Complete | TC-1102 |
| [TC-1302](closed/task-1302-decide-project-attribute-schemas.md) | Decide Project Attribute Schemas | Complete | TC-1301, TC-1203, TC-1106 |

## Planning reconciliation

The initial monorepo reconciliation added 17 Stage 11–15 cards and changed these
existing planning files. The YAML comparison follow-up below adds TC-1105.

| File | Material change |
| --- | --- |
| [Strategic roadmap](0001-initial-roadmap.md) | Monorepo scope, compile/link workflow, bounded checks, historical anchors, incremental execution order |
| [This index](0002-task-card-index.md) | Current dependencies/statuses and separate completed evidence inventory |
| [Task template](task-card-template.md) | Compatibility/components, present problem, risks, and explicit refinement instructions |
| [TC-0905](closed/task-0905-define-verification-analyzer-contract.md) | Ready becomes Planned; add compilation/import decisions and compiled plan fixture obligations |
| [TC-0904](closed/task-0904-implement-the-selected-ecosystem-tool.md) | Add bounded linking dependency and compiled consumer evidence; unlock report instead of historical audit |
| [TC-0903](closed/task-0903-run-a-derived-presentation-experiment.md) | Conditional becomes Planned; select verification report and make composition a possible successor |
| [TC-0807](task-0807-test-authored-views-and-specifications.md) | Reverse the old report prerequisite; retain conditional composition scope |
| [TC-0902](task-0902-run-an-independent-reqif-roundtrip.md) | Clarify that the decision concerns a maintained interchange capability; external-tool prerequisite remains |
| [Repository README](../README.md) | Align current direction and verification wording with incremental planning |
| [Specification index](../specification/README.md) | Remove stale prospective status while retaining current normative authority |

The former strategic field-use/separate-parser expectations and prospective
publication gate are removed from active planning. Existing completed audit,
pilot, identity, safety, and verification evidence is preserved unchanged.
No feature implementation, source-contract change, or release action is part of
this reconciliation.

## YAML comparison follow-up

[TC-1105](closed/task-1105-compare-yaml-and-custom-requirement-source.md) is complete.
[Research 0033](../research/0033-yaml-source-representation-decision.md) selects a
constrained YAML direction, now implemented through the explicit source 0.3
contract and command safety addendum. Default source 0.2 invocation is retained. [Experiment 0025](../experiments/0025-yaml-source-comparison/README.md)
records the corpus, failures, replay and clause-level outline.

TC-1106–TC-1109 completed specification, interpretation, safe formatting and
migration, together with TC-1401 and TC-1402 safety fixes. TC-1103 still depends
on TC-1102; maintained YAML requirements are now available for the experiment.
TC-1302 retains the completed profile as a prerequisite alongside the unresolved
ownership and import decisions. The roadmap records the selected direction while
retaining independent correctness, ownership and integration work.

## Requirements-only YAML scope refinement

The YAML decision applies only to requirement source. Other artifact authoring
formats remain independent workflow decisions; compiled interfaces, references,
provenance and linking provide the integration boundary. No task statuses, IDs or
dependencies change in this refinement, and no additional cards are needed.

| Card | Clarification |
| --- | --- |
| [TC-1101](closed/task-1101-define-monorepo-component-boundaries.md) | Record source-format decisions separately from shared component interfaces |
| [TC-1103](closed/task-1103-test-compilation-linking-and-rebuilds.md) | Exercise provisional YAML requirements with the existing TSV plan adapter; remove shared-notation comparison |
| [TC-1106](closed/task-1106-specify-yaml-requirement-source-profile.md) | Specify the requirements model, YAML mapping, structural schema and semantic rules with explicit authority and scope |
| [TC-1203](closed/task-1203-define-import-and-reference-contracts.md) | Separate common linking meanings from each artifact's authored encoding |
| [TC-0905](closed/task-0905-define-verification-analyzer-contract.md) | Select plan notation independently and consume published compiled interfaces |
| [TC-1302](closed/task-1302-decide-project-attribute-schemas.md) | Keep project declaration format a decision distinct from YAML requirement values |

The [roadmap product direction](0001-initial-roadmap.md#product-direction) records
this boundary. Existing requirements-only implementation and migration cards
inherit TC-1106's scope; other artifact implementations inherit their own contracts.

## Completed requirements YAML batch

TC-1106–TC-1109, TC-1401 and TC-1402 are Complete and filed under closed/.
[Research 0035](../research/0035-yaml-requirements-batch-verification.md) records the
source/schema decision, maintained YAML commands, separate migration executable,
120 equivalent migrated requirement values, safety regressions and full verification.
TC-1302 and TC-1504 retain their other incomplete prerequisites. TC-1403 still
concerns the legacy parser; the YAML diagnostics do not complete it. TC-1502's
build description now reflects the pinned Java YAML parser dependency.

## Completed compiled-requirements batch

TC-1101–1103, TC-1502 and TC-1201–1202 are Complete and filed under closed/.
[Research 0041](../research/0041-compiled-requirements-verification.md) records the
maintained compiler and final full verification. Research 0036–0040 record logical
boundaries, ownership, the 13-case serialized experiment, independent version
declarations and the selected output contract. Existing physical paths are retained;
TC-1104 stays Conditional until a move has a demonstrated consumer benefit.
TC-1203 and TC-1301 are Ready. TC-0905 still needs TC-1203; TC-1302 needs TC-1301
and TC-1203. TC-1504 still needs parser recovery. TC-1204 remains Conditional.

## Updating cards

When beginning a card, change its status here and in the card together. When
completing it:

1. record reproducible evidence and the resulting decision;
2. update affected contracts, experiments, or roadmap text;
3. mark the card complete, retain its ID, and link its result under closed/;
4. make newly unblocked cards Ready only when their conditions are satisfied;
5. revise or supersede dependencies when evidence selects a smaller scope;
6. make a separate commit for each completed task card, including its evidence and
   necessary index/dependency updates.

Use the [template](task-card-template.md). Explain material scope/status/dependency
changes in a Planning refinement section. Do not renumber existing cards or turn
generated output into authored source.

## Completed attribute decisions and selected successors

TC-1301/1302 are Complete under closed/. Research 0051–0053 record the ownership,
two-type scope, declaration/attachment contract, comparison policy and worked cases.
TC-1303–1308 are six new bounded successors, preserving current source contracts
until their opt-in implementations land. TC-1303 is Ready; formatting/trace and
compilation can proceed independently after it, then serialized analysis, report
propagation and integrated verification follow. Existing conditional composition,
layout and external ReqIF cards remain unchanged.
