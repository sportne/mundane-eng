# Task TC-0903: Run a Derived Presentation Experiment

```json
{
  "format": "mundane-work-source-0.1",
  "status": "Complete",
  "dependencies": [
    "TC-0904"
  ],
  "relations": [],
  "planning": {
    "stage": "9",
    "type": "Rendering experiment",
    "condition": "TC-0904",
    "unlocks": "TC-1501 and conditional TC-0807",
    "statusNote": ""
  }
}
```

## Question

Can a useful specification, matrix, or browser be generated without making presentation structure authoritative?

## Outcome

One disposable presentation is generated from source and evaluated against a concrete delivery or review workflow.

## Work

- Generate one deterministic verification coverage/staleness report from the
  selected compiled artifacts and TC-0904's analyses, using the pilot/change case.
- Start with deterministic derived ordering and no new authoritative view syntax.
- Render statement prose and opaque LaTeX content without changing their source ownership.
- Include source IDs and trace navigation needed by the selected workflow.
- Delete and reproduce the output from a clean recorded commit archive.
- Record whether sections, selection, and ordering need authored configuration.
  Supply that evidence to TC-0807 only if the simple generated view is insufficient.

## Acceptance evidence

- Built-in values, source IDs, assertion locations, input revisions, analysis
  completeness, and compiler/format provenance are visible or directly linked.
  Project-defined attributes may be shown only after their design/implementation;
  this experiment does not invent them or depend on their availability.
- Repeated builds from recorded inputs have identical report content under the
  documented provenance policy. A changed requirement updates the expected rows.
- Missing, incompatible, partial, and output-failure cases cannot produce a report
  presented as a complete successful analysis. Generated views are clearly labeled
  with regeneration instructions and retain no competing authoring authority.
- The output is reproducible and disposable.
- Every displayed requirement value traces to readable source.
- Rendering failure does not prevent understanding or editing requirements.
- Any required authored ordering is explicitly identified rather than hidden in code.
- The experiment determines whether a standalone renderer merits a task.

## Out of scope

- A hosted requirements database.
- A mandatory web editor.
- General document-publishing features.

## Completion decision

Build a maintained renderer only if the presentation materially serves the selected workflow. Do not standardize a view language solely because a renderer can consume one.

## References

- [TC-0807](../task-0807-test-authored-views-and-specifications.md)
- [Roadmap views study](../0001-initial-roadmap.md#views-and-specifications)
- [TC-0904](task-0904-implement-the-selected-ecosystem-tool.md)
- [TC-1501](task-1501-extend-artifact-workflow-regression-corpora.md)

## Compatibility and affected components

Likely components: an experimental renderer, report fixtures, and source-location
links over public artifact/analysis contracts. An issued report may be retained
as a delivery record, with its inputs and separate approval provenance; generating
it does not approve content or replace authored requirements and assertions.

## Planning refinement

Reconciled on 2026-09-04: select verification reporting as the concrete workflow,
change Conditional to Planned, and replace the authored-view prerequisite with
TC-0904. TC-0807 becomes a conditional successor if this experiment demonstrates
composition needs. The card remains a bounded experiment, not a general report
framework; stop or narrow if its first view requires a new publishing language.

Refined during execution: use a clean archive of a recorded Git commit for input
reproduction, with installed analyzer/renderer tools. This retains immutable input
evidence without creating a tag. The recorded tool revision and fixture digests
identify the reproduction boundary.

## Completion evidence

Completed the deterministic report experiment with public analyzer input, source navigation, provenance, golden rebuild and failure evidence. See [Research 0046](../../research/0046-verification-report-decision.md) and [Experiment 0029](../../experiments/0029-verification-report/README.md). Retain the experimental renderer; TC-0807 stays conditional and TC-1501 is unlocked.
