# Specification 0001: Project Foundation

Status: Living, nonnormative project foundation

## Purpose

Develop a text-based engineering tooling ecosystem whose source remains readable,
reviewable and useful in ordinary Git workflows. Mundane-Eng is the engineering
platform; requirements, verification plans and work items have separate models
and tools.
Current syntax and command behavior are defined by the [specification index](README.md).

## Authority and identity

Human-authored IDs identify requirements and other declared entities within their
explicit scopes. Filenames, record order and generated transport identifiers do not
supply a second identity. Content digests identify particular source or compiled
revisions and are expected to change after edits.

Checked-in requirement files, project declarations, plans and work-item records own
their facts. Compiled artifacts retain values, source locations and provenance.
Indexes, analyses and reports are derived and reproducible. Editing generated output
does not update authoritative source.

Git owns version history, branches, commits and merges. Domain tools own parsing,
validation and explicit analysis. Review and engineering procedures determine
approval and evidence adequacy; successful compilation or graph reachability does
not establish them.

## Model boundaries

- Requirements contain their human ID, title, normative statement, optional rationale,
  built-in labels/citations and explicit decomposition links. YAML 0.4 adds project-
  declared text and enum attributes with required/optional presence and no defaults.
- Descriptive metadata can belong to a requirement. Independently revised safety
  assessments, verification results and contextual allocations belong to separate
  assertions or artifacts. A criticality label does not establish assessment authority.
- Verification plans own activities and planned coverage. The implemented analyzer
  compares explicit baseline/current bindings; it does not execute activities or
  determine requirement satisfaction.
- Work items own task/issue status, dependencies, typed relations and narrative.
  Their references do not approve, close or alter the target artifact.
- Authored composition is separate from requirement hierarchy. Generated views already
  exist; a view language needs a demonstrated selection, ordering or delivery need.
- Mathematical content remains explicitly delimited, opaque LaTeX. A future semantic
  math tool requires its own contract and consumer.

## Source and tooling

Requirements, work items, verification plans and project attribute declarations
use YAML with domain-owned fields and shared presentation rules. Human-authored
artifacts should use consistent syntax unless a concrete user workflow justifies
another representation; native engineering files may retain their own formats.
Common integration uses compiled contracts, explicit references and provenance.

Source files can contain multiple complete requirements. Source placement and ordering
remain presentation choices, while cross-file references must resolve in the selected
source set. Tools preserve comments and opaque content according to their selected
profile. They must reject unsupported or incomplete inputs instead of silently losing
semantic values.

Components remain independently usable. Shared code is justified by a demonstrated
boundary; it does not require a universal engineering model, common source notation,
server or database. Diagnostics, formatting and reports support ordinary authoring
and review without becoming mandatory authoring interfaces.

## Incremental development

Add capabilities for concrete workflows and validate them with checked-in examples,
golden/adversarial corpora, compatibility tests and reproducible build evidence.
Preserve earlier research as evidence for its recorded inputs, not as a current
capability declaration. The [roadmap](../roadmap/0001-initial-roadmap.md) identifies
conditional work; the [backlog](../WORK-ITEMS.md) derives status from source cards.
