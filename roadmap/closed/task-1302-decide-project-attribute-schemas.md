# Task TC-1302: Decide Project Attribute Schemas

```json
{
  "format": "mundane-work-source-0.1",
  "status": "Complete",
  "dependencies": [
    "TC-1106",
    "TC-1203",
    "TC-1301"
  ],
  "relations": [],
  "planning": {
    "stage": "13",
    "type": "Decision",
    "condition": "TC-1301, TC-1203, TC-1106",
    "unlocks": "follow-up work selected by the completion decision",
    "statusNote": ""
  }
}
```

## Question

How should a project declare and select the bounded typed attributes chosen by
TC-1301 without hidden configuration or ambiguous source interpretation?

## Outcome

A written schema/syntax decision and worked examples enable separately scoped
future implementation cards; this card does not authorize those implementations.

## Work

- Use TC-1106's requirements YAML profile when attaching attribute values to
  requirements. Independently compare project attribute declarations in that
  representation with a narrowly scoped configuration format; record the chosen
  declaration format and its rationale. Other engineering artifact formats remain
  outside this decision. Specify
  storage, version control, explicit selection versus directory discovery, roots,
  missing/invalid/ambiguous schemas, and single-file operation without project context.
- Decide naming restrictions, built-in/reserved conflicts, type/enumeration
  declarations, duplicate and unknown attributes, required values, multiplicity,
  defaults, and invalid values.
- Specify schema version/compatibility, no-schema behavior, imported schema
  selection, and reproducibility when a schema changes.
- Work through source attachment, semantic representation, deterministic formatter
  ordering and comment preservation without assigning comments invented ownership.
- Map propagation obligations to validation, semantic output, trace, views, ReqIF,
  and editor diagnostics/hover/completion; distinguish lossless support, best-effort
  mapping with visible loss, and explicit unsupported cases.
- Prepare bounded successor cards only after the decision: grammar/model/validation;
  formatting; interchange/output propagation; examples/testing/documentation; and
  individual editor capabilities where justified.

## Acceptance evidence

- A decision record precedes implementation and contains valid and invalid source
  and schema examples for every chosen rule, including isolated-file processing.
- A matrix states whether unchanged 0.1/0.2 projects without schemas remain valid
  and identifies any intentional breaking case and its migration note.
- Worked defaults and schema changes demonstrate what enters compiled semantics;
  generated output never becomes the attribute definition authority.
- The follow-up test plan names golden parsing/formatting, invalid schema/value,
  round-trip, property/mutation, and no-schema compatibility fixtures.
- Each proposed follow-up names this decision as a dependency; unselected syntax
  and unsupported ReqIF/editor behavior are not silently promised.

## Out of scope

- Implementing custom attributes, general metadata inheritance, a policy engine,
  or retroactively assigning assessment authority to descriptive labels.

## Compatibility and affected components

Likely components: future schema source, grammar, Interpreter, formatter, CLI input
selection, artifact contracts, experimental ReqIF adapter, and language reference.
Human IDs remain identity. Reconcile the old safety/allocation dispositions through
a new decision, preserving the historical reasoning.

## Completion decision

Select only a reviewable design whose no-schema and standalone-file cases are
explicit. If schema discovery, defaults, or type breadth makes the initial model
ambiguous, narrow it before creating implementation work.

## References

- [Roadmap](../0001-initial-roadmap.md)
- [TC-1301](task-1301-classify-project-attribute-use-cases.md)
- [TC-1203](task-1203-define-import-and-reference-contracts.md)
- [TC-0902](../task-0902-run-an-independent-reqif-roundtrip.md)
- [Source language](../../specification/0005-mundanereq-source-language-0.2.md)
- [TC-1105](task-1105-compare-yaml-and-custom-requirement-source.md)
- [TC-1106](task-1106-specify-yaml-requirement-source-profile.md)

## Planning refinement

Require the selected YAML/custom-source disposition to inform attribute schema
placement instead of assuming the current grammar will be extended. The completed
comparison selects YAML; add TC-1106 as an explicit prerequisite to resolve its
remaining profile and source-selection rules before schema placement. Existing
ownership, type-scope, and no-schema compatibility decisions remain required.

Distinguish YAML requirement values from the still-unresolved representation of
project attribute declarations. This requirements-specific schema decision does
not establish a platform-wide schema or authoring format. Status and dependencies
remain unchanged.

## Execution refinement

The selected design uses requirements YAML for values and narrow JSON for project
declarations, independently of other artifact formats. Six successor cards split
validation, formatting/trace, compilation, serialized analysis, report propagation,
and integrated examples/documentation. Editor assistance obligations are specified,
but host-specific diagnostics, completion and hover cards await an actual editor
workflow; unsupported ReqIF mapping remains conditional. No attribute implementation
or current normative contract/version change is included in this decision card.

## Completion evidence

Completed 2026-09-05. [Research 0052](../../research/0052-project-attribute-schema-decision.md) selects explicit narrow JSON declarations, YAML values, schema/standalone selection, absence and requiredness, source/output evolution, conservative comparison and propagation boundaries. [Research 0053](../../research/0053-project-attribute-design-cases.md) supplies valid/invalid worked cases and future verification obligations. TC-1303–1308 are indexed successors; only TC-1303 is Ready. JSON/YAML example syntax and planning consistency were checked; no attribute implementation, operational trials or external fidelity results are claimed.
