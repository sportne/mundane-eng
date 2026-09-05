# Roadmap Task-Card Index

Status: Active backlog

Strategic source: [Roadmap 0001](0001-initial-roadmap.md)

## Purpose

Source cards selected by [work-items.json](work-items.json) own status, dependencies
and typed links. The [derived work-item index](../WORK-ITEMS.md) is generated from
those cards; update the source and run `make work-index`. This document retains
strategic sequencing and historical planning decisions, not editable status rows.
See the [authoring workflow](../distribution/work-items.md) for card changes.

## Status vocabulary

- **Ready:** known dependencies are complete; work may begin.
- **Planned:** accepted work with incomplete dependencies.
- **Conditional:** perform only when its stated decision or prerequisite justifies it.
- **In progress:** actively being executed.
- **Complete:** acceptance evidence and the completion decision are recorded.
- **Superseded:** replaced by a later card or roadmap decision, with the replacement identified.

Only recorded evidence and decisions justify an authored completion claim. If future work is
superseded, record its replacement, retain its ID, and keep a closed-card link.
Completed historical cards retain Complete and their original evidence; a
superseded prospective roadmap sequence does not undo their completed work.

## Dependency shape and priority

The diagrams below preserve the planned dependency shape for context. Current
relationships, statuses and unfinished prerequisites are compiled from source cards
in the derived index. The attribute chain remains independent of the work-item chain.

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

Current cards and their status are in the [derived work-item index](../WORK-ITEMS.md).

The completed import contract precedes the plan contract, whose fixtures drive
the maintained linker and analyzer. Their completed cards are in the evidence
inventory below; TC-1501 extends that integrated workflow.

## Stage 13: Project attribute implementation

Current cards and their status are in the [derived work-item index](../WORK-ITEMS.md).

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

Current cards and their status are in the [derived work-item index](../WORK-ITEMS.md).

## Conditional existing work

Current cards and their status are in the [derived work-item index](../WORK-ITEMS.md).

## Completed evidence

Closed cards retain historical results and completion evidence.
TC-1001/TC-1002 remain completed records; their prospective decision sequence is
superseded by Stages 11–15. Their filenames and evidence are preserved.

Current cards and their status are in the [derived work-item index](../WORK-ITEMS.md).

## Planning reconciliation

The initial monorepo reconciliation added 17 Stage 11–15 cards and changed these
existing planning files. The YAML comparison follow-up below adds TC-1105.

| File | Material change |
| --- | --- |
| [Strategic roadmap](0001-initial-roadmap.md) | Monorepo scope, compile/link workflow, bounded checks, historical anchors, incremental execution order |
| [This index](0002-task-card-index.md) | Current dependencies/statuses and separate completed evidence inventory |
| [Task template](task-card-template.yaml) | Compatibility/components, present problem, risks, and explicit refinement instructions |
| [TC-0905](closed/task-0905-define-verification-analyzer-contract.yaml) | Ready becomes Planned; add compilation/import decisions and compiled plan fixture obligations |
| [TC-0904](closed/task-0904-implement-the-selected-ecosystem-tool.yaml) | Add bounded linking dependency and compiled consumer evidence; unlock report instead of historical audit |
| [TC-0903](closed/task-0903-run-a-derived-presentation-experiment.yaml) | Conditional becomes Planned; select verification report and make composition a possible successor |
| [TC-0807](task-0807-test-authored-views-and-specifications.yaml) | Reverse the old report prerequisite; retain conditional composition scope |
| [TC-0902](task-0902-run-an-independent-reqif-roundtrip.yaml) | Clarify that the decision concerns a maintained interchange capability; external-tool prerequisite remains |
| [Repository README](../README.md) | Align current direction and verification wording with incremental planning |
| [Specification index](../specification/README.md) | Remove stale prospective status while retaining current normative authority |

The former strategic field-use/separate-parser expectations and prospective
publication gate are removed from active planning. Existing completed audit,
pilot, identity, safety, and verification evidence is preserved unchanged.
No feature implementation, source-contract change, or release action is part of
this reconciliation.

## YAML comparison follow-up

[TC-1105](closed/task-1105-compare-yaml-and-custom-requirement-source.yaml) is complete.
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
| [TC-1101](closed/task-1101-define-monorepo-component-boundaries.yaml) | Record source-format decisions separately from shared component interfaces |
| [TC-1103](closed/task-1103-test-compilation-linking-and-rebuilds.yaml) | Exercise provisional YAML requirements with the existing TSV plan adapter; remove shared-notation comparison |
| [TC-1106](closed/task-1106-specify-yaml-requirement-source-profile.yaml) | Specify the requirements model, YAML mapping, structural schema and semantic rules with explicit authority and scope |
| [TC-1203](closed/task-1203-define-import-and-reference-contracts.yaml) | Separate common linking meanings from each artifact's authored encoding |
| [TC-0905](closed/task-0905-define-verification-analyzer-contract.yaml) | Select plan notation independently and consume published compiled interfaces |
| [TC-1302](closed/task-1302-decide-project-attribute-schemas.yaml) | Keep project declaration format a decision distinct from YAML requirement values |

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

1. Update the structured YAML fields in the authoritative card and its prose.
2. Preserve human IDs; add explicit dependencies/relations rather than infer links.
3. Record completion evidence before setting Complete; move completed cards under
   closed/ and update the selected path in work-items.json and incoming Markdown links.
4. Regenerate `WORK-ITEMS.md` with `make work-index`; do not edit its status rows.
5. Run `make work-backlog-verify` and owning implementation checks.
6. Commit each completed task separately, including its source and derived index.

Use the [template](task-card-template.yaml). Conditional policies remain authored
qualifications; dependency completion alone does not authorize execution. Historical
planning notes below are records of earlier decisions, not another status source.

## Completed attribute decisions and selected successors

TC-1301/1302 are Complete under closed/. Research 0051–0053 record the ownership,
two-type scope, declaration/attachment contract, comparison policy and worked cases.
TC-1303–1308 are six new bounded successors, preserving current source contracts
until their opt-in implementations land. TC-1303 is Ready; formatting/trace and
compilation can proceed independently after it, then serialized analysis, report
propagation and integrated verification follow. Existing conditional composition,
layout and external ReqIF cards remain unchanged.

## Completed work-item integration

TC-1601–1606 established the original source profile, compiler, bounded typed
linking, prerequisite analysis and generated index. The repository uses these tools for its own cards; TC-1607–1609
move current authoring to the independently specified YAML profile. [Research 0059](../research/0059-work-item-backlog-verification.md)
records migration and clean verification; current status remains in source cards
and their derived view.

## YAML work-item continuation

TC-1607 -> TC-1608 -> TC-1609 replaces the source-carrier decision with a YAML
specification/schema, compiler support, then checked backlog migration. The completed
TC-1601–1606 evidence remains historical. This chain is independent of TC-1303–1308.

TC-1607–1609 are complete. [Research 0062](../research/0062-work-item-yaml-backlog-verification.md)
records the checked YAML migration and clean authoritative verification. Current
status remains in YAML source cards and their derived index.
