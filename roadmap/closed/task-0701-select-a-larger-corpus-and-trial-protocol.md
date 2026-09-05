# Task TC-0701: Select a Larger Corpus and Trial Protocol

```json
{
  "format": "mundane-work-source-0.1",
  "status": "Complete",
  "dependencies": [
    "TC-0603"
  ],
  "relations": [],
  "planning": {
    "stage": "7",
    "type": "Research and trial design",
    "condition": "TC-0603",
    "unlocks": "TC-0702 and TC-0703",
    "statusNote": ""
  }
}
```

## Question

What legally usable corpus and controlled workflow can test the toolchain beyond its designers and small fixtures?

## Outcome

A licensed, provenance-recorded corpus and trial protocol cover multi-level traceability, meaningful changes, both file granularities, and independent authorship.

## Work

- Search for a substantially larger public requirements corpus with clear redistribution terms.
- Record exact upstream revision, license, adaptations, and excluded material.
- Assess whether its structure pressures identity, traceability, allocation, and source discovery without manufacturing facts.
- Define authoring, review, concurrency, baseline, and tool-use scenarios before encoding results.
- Define observation categories and stop conditions.

## Acceptance evidence

- Corpus provenance is independently auditable.
- The trial has enough requirements and relationship depth to exceed prior experiments materially.
- Scenarios distinguish language, tool, Git/forge, policy, and training friction.
- Success does not require participants to know project design history.
- The protocol avoids treating existing RM product features as mandatory outcomes.

## Out of scope

- Changing the language to fit the corpus before the trial.
- Copying material without clear permission.
- Claiming statistical usability evidence from one trial.

## Completion decision

If no corpus is both suitable and redistributable, create a second original corpus informed by observed structures and state that limitation explicitly.

Completed on 2026-08-29. [Experiment 0011](../../experiments/0011-operational-corpus/README.md)
selects an original fictional corpus of 60 requirements, 54 relationships, six
roots, and three trace levels under the project license. Its
[provenance record](../../experiments/0011-operational-corpus/provenance.md) names
exact inspected revisions of NASA cFE, Doorstop, OpenFastTrace, and StrictDoc
and explains why importing each would manufacture trace meaning, broaden scope,
introduce avoidable license/model coupling, or cross the project's stated
provenance boundary.

The Stage 7 protocol is frozen before execution, including participant inputs,
task oracles, two file granularities, concurrent-change cases, observation
classes, evidence rules, and stop conditions. Proceed to TC-0702 and TC-0703;
do not infer real-system correctness or statistical usability from the original
corpus.

## References

- [TC-0603](task-0603-run-the-integrated-toolchain-trial.md)
- [Roadmap Stage 7](../0001-initial-roadmap.md#stage-7--test-operational-use-and-scale)
- [Experiment 0004 provenance approach](../../experiments/0004-transferability/provenance.md)
