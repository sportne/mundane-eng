# Task TC-0807: Test Authored Views and Specifications

```json
{
  "format": "mundane-work-source-0.1",
  "status": "Conditional",
  "dependencies": [
    "TC-0603",
    "TC-0903"
  ],
  "relations": [],
  "planning": {
    "stage": "8",
    "type": "Model-pressure experiment",
    "condition": "TC-0603, TC-0903 and a demonstrated composition need",
    "unlocks": "A view-language decision and any justified successor card",
    "statusNote": ""
  }
}
```

## Question

Does a real workflow require authored ordering and composition, or is a disposable generated presentation sufficient?

## Outcome

A delivery or review experiment compares ID-sorted generation, tool-specific ordering input, and a minimal authored view artifact without changing requirement identity.

## Work

- Select a concrete specification or report delivery scenario.
- Generate a useful presentation with no standardized view language first.
- Identify where authored sections, ordering, inclusion, or reuse materially affect the workflow.
- Test a minimal separate view candidate only for those needs.
- Exercise requirement movement, reuse in two views, and missing references.

## Acceptance evidence

- The requirement model remains independent of file and document position.
- The experiment distinguishes delivery structure from requirement hierarchy.
- Ordinary source remains understandable without the generated document.
- Any proposed view syntax has fewer concepts than the workflow it replaces.

## Out of scope

- Making Markdown the authoritative requirement container.
- A general publishing system.
- Standardizing layout or styling.

## Completion decision

Defer a view language if a disposable ordering convention serves the workflow. Select a companion view artifact only when authored composition is demonstrably authoritative.

## References

- [Roadmap views study](0001-initial-roadmap.md#views-and-specifications)
- [Non-Markdown view research](../research/0006-non-markdown-view-notation.md)
- [TC-0903](closed/task-0903-run-a-derived-presentation-experiment.md)

## Planning refinement

Reconciled on 2026-09-04: use the basic report experiment as evidence before
considering authored composition. This reverses the former prerequisite so a
useful generated view can be tested without first selecting a view language.
The original Conditional status and composition scope are retained.
