# Task TC-1301: Classify Project Attribute Use Cases

```json
{
  "format": "mundane-work-source-0.1",
  "status": "Complete",
  "dependencies": [
    "TC-1102"
  ],
  "relations": [],
  "planning": {
    "stage": "13",
    "type": "Experiment and decision",
    "condition": "TC-1102",
    "unlocks": "TC-1302",
    "statusNote": ""
  }
}
```

## Question

Which project-specific facts earn typed requirement attributes, and which need
independently owned assertions in the compiled-artifact workflow?

## Outcome

A bounded use-case decision selects the smallest useful attribute capability, or
records why a particular example should remain an assertion or project policy.

## Work

- Collect descriptive classifications, external references, ownership labels,
  assessed safety levels, verification status, and allocation-by-variant examples.
- Apply TC-1102's ownership matrix; distinguish project annotation from consequential
  assessment and broadly useful language behavior from organization-specific policy.
- Consider text and enumeration first. Evaluate lists, booleans, integers, URLs,
  and requirement references individually, with a concrete need for each inclusion.
- Decide required/optional/repeated behavior and whether defaults improve authoring
  enough to justify implicit values and schema-change effects.
- Draft valid/invalid conceptual examples without prematurely selecting .mreq syntax.

## Acceptance evidence

- A use-case matrix records ownership, type, cardinality, authority, and concrete
  consumers for each candidate.
- At least one text and one enumeration example have valid/invalid values; list
  references and contextual criticality explicitly receive include/defer/reject
  decisions with reasons.
- The record separates descriptive tags from contextual safety assertions and
  verification results, using existing studies as evidence.
- Rejected alternatives and stop conditions are written down; TC-1302 receives a
  bounded initial type/cardinality set rather than a universal metadata framework.

## Out of scope

- Grammar, implementation, automatic machine identities, inferred requirement
  satisfaction, or moving all project facts onto requirement records.

## Compatibility and affected components

Existing requirement fields remain valid. Any later attribute declarations must
be typed, explicit, reviewable, and checked-in source. Likely inputs: ownership
decision, project examples, model-pressure studies, and future language reference.

## Completion decision

Proceed to schema design only for intrinsic/descriptive cases with a demonstrated
consumer. Keep independently revised, contextual, or evidential assertions outside
the requirement object. Stop types whose sole justification is hypothetical breadth.

## References

- [Roadmap](../0001-initial-roadmap.md)
- [TC-1102](task-1102-define-requirement-and-assertion-ownership.md)
- [Safety ownership](../../research/0025-safety-classification-ownership-decision.md)
- [Allocation decision](../../research/0027-allocation-model-decision.md)

## Completion evidence

Completed 2026-09-05. [Research 0051](../../research/0051-project-attribute-use-case-decision.md) records the ownership/use-case matrix, text and enumeration scope, required/optional single values, rejected defaults and deferred lists/references, and valid/invalid conceptual examples. This is a design assessment against repository evidence, not implementation or user-trial evidence. TC-1302 is now Ready.
