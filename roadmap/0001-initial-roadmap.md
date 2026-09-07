# Roadmap 0001: A Composable Engineering Tooling Monorepo

Status: Living, incremental roadmap

The [source-card workflow](0002-task-card-index.md) and [derived index](../WORK-ITEMS.md)
record work and status. This document describes direction and conditions for new work.

## Purpose and boundaries

Build a text-oriented engineering tooling ecosystem with independently usable
components. Human-authored requirements, declarations, plans and work items own
their facts. Compilation produces versioned values and provenance; linking resolves
explicit references; domain analysis interprets them; reports remain derived.

```text
requirements + project declarations -> requirement compiler --+
verification plan source -----------> plan compiler -----------+-> linking / analysis -> views
work-item source -------------------> work-item compiler ------+
```

This shows component relationships, not a universal command or shared metamodel.
Requirements and work items independently use YAML, plans use TSV and project
attribute declarations use narrow JSON. Future artifacts may use different formats
or retain native engineering files. Shared integration does not require common
source notation. Requirement IDs remain human-authored; digests identify revisions.

## Implemented foundation

| Area | Current result and evidence |
| --- | --- |
| Requirement authoring | YAML 0.3/0.4, validation/SARIF, conservative formatting and decomposition trace; [contracts](../specification/README.md) |
| Compiled integration | Versioned requirement output, explicit scopes and pins, parser-free consumers; [compiler guide](../distribution/compile.md) |
| Verification | Independent TSV plan compiler, coverage/review analysis and an experimental derived report; [workflow](../distribution/verification.md) |
| Work items | YAML source, typed linking, prerequisite analysis and generated views; the repository uses these for its own cards; [contracts](../specification/README.md) |
| Project attributes | Explicit declarations, text/enum values, formatter/validator propagation, strict imports, whole-schema review comparison and reports; [contracts](../specification/README.md) |
| Impact analysis | Scoped prospective queries, deterministic shortest explanations and validated source-linked reports; [workflow](../experiments/0037-impact-analysis/README.md) |
| VS Code authoring | Local extension with explicit projects, unsaved diagnostics, local and imported-target navigation, formatting and typed assistance; [guide](../editors/vscode/README.md) |
| Contributor checks | Authoritative `make verify`, clean-checkout wrapper, compatibility/golden corpora, seeded workflows and targeted behavioral mutations; [build guide](../distribution/build-verification.md) |

These capabilities establish bounded workflows. They do not establish executed
verification, evidence adequacy, safety approval or requirement satisfaction.
Completed cards record the scope of their maintained capabilities.

## Impact-analysis workflow

TC-1701 defines the [impact contract](../specification/0023-cross-artifact-impact-0.1.md).
TC-1702 assembles the scoped graph; TC-1703 queries explained paths; TC-1704 renders
source-linked reports; TC-1705 verifies and documents the complete workflow.
The dependency order is TC-1701 → TC-1702 → TC-1703 → TC-1704 → TC-1705.
[Recorded verification](../research/0069-impact-workflow-verification.md) covers the
completed workflow. Its source cards retain individual completion evidence.

## Conditional backlog

| Card | Condition and decision enabled |
| --- | --- |
| [TC-0807: Authored views](task-0807-test-authored-views-and-specifications.yaml) | Demonstrate a composition/delivery need beyond the current generated report; compare simple ordering with a separate view artifact before selecting syntax. |
| [TC-1104: Component layout](task-1104-establish-monorepo-component-layout.yaml) | Show a measurable navigation or dependency benefit; retain current paths when movement has no observable value. |

Completed prerequisites do not remove these conditions. ReqIF interchange remains unimplemented. The three-command native archive covers
validate/format/trace; the other maintained commands have separate build targets.

## Candidates for new bounded cards

Select a concrete workflow before turning a candidate into implementation work:

- Unicode confusable/invisible-character diagnostics with explicit source rules.
- Removal of repeated decoding/parsing where measurements show a correctness or
  performance benefit, supported by behavioral regression tests.
- Packaging/platform improvements for an identified contributor or user need, with
  tested toolchain assumptions, provenance and smoke checks.
- Additional safety, allocation, BOM, code or CAD adapters justified by a concrete
  authoring and analysis workflow, with their own ownership and format decisions.

These are candidates, not selected batches. No monorepo rearrangement, universal
metadata model or authored view language is a prerequisite for independent work.

## Execution and evidence

Keep design decisions ahead of dependent implementation. Separate decoding, syntax,
semantics, validation and domain analysis where the boundary has observable value.
Every implementation supplies its own regression evidence; integration checks then
exercise published interfaces and reproduce the complete workflow.

Use checked-in examples, golden/adversarial and compatibility fixtures, seeded
property checks, targeted mutations, dogfooding and recorded clean builds. Focused
human usability sessions or external interoperability checks are useful when actual
contributors/tools are available; do not invent those results or make other work
conditional on open-ended adoption studies.

Follow the [task-card workflow](0002-task-card-index.md), preserve original evidence
and explicitly revise dependencies when scope changes. Generated output never
becomes an alternative authoring source, and compatibility work serves current
language and tooling consumers without promising permanent stability.

## Views and specifications

The existing report is disposable, deterministic and linked to source. Issued
reports may be retained as delivery records with their own approval provenance.
Selection, ordering and composition can become separately authored facts only when
TC-0807 demonstrates the need; they do not change requirement identity or hierarchy.

## VS Code authoring batch

TC-1800 establishes YAML-only requirement commands. TC-1801 defines the VS Code
extension and explicit project snapshots; TC-1802 adds compiler diagnostics.
Navigation (TC-1803), formatting (TC-1804) and attribute completion (TC-1805)
then use that foundation. TC-1806 adds attribute hover help after completion.
The [derived index](../WORK-ITEMS.md) records individual status and evidence.

## Editor consolidation batch

TC-1901 reconciles planning and setup documentation. TC-1902 centralizes editor
version declarations; TC-1903 measures and reduces repeated validation while
preserving freshness. TC-1904 packages the matching Linux editor and bridge, and
TC-1905 tests installation and recovery from that bundle. The execution order is
TC-1901 → TC-1902 → TC-1903 → TC-1904 → TC-1905.

## Work-item editor batch

TC-2001 selects the existing work-item manifest and defines the editor boundary.
TC-2002 reuses compilation for buffers; TC-2003 adds local diagnostics. TC-2004
adds dependency navigation, TC-2005 adds structural completion and hover, and
TC-2006 dogfoods the backlog and verifies installation. Execution order:
TC-2001 → TC-2002 → TC-2003 → TC-2004 → TC-2005 → TC-2006.

## Imported editor targets

TC-2101 defines explicit imports and source mappings, TC-2102 validates compiled
inputs, and TC-2103 captures typed relation spans. TC-2104 adds revision-checked
navigation, TC-2105 adds imported completion/hover, and TC-2106 verifies installed
workflows. Order: TC-2101 → TC-2102 → TC-2103 → TC-2104 → TC-2105 → TC-2106.

## YAML authoring migration

TC-2201 defines consistent YAML presentation and source contracts; TC-2202 migrates
attribute declarations; TC-2203 replaces TSV verification sources with YAML;
TC-2204 verifies complete workflows and removes the replaced source representations.
Generated JSON remains the integration boundary. Follow-up cleanup is scoped after
migration so only obsolete material is removed.
