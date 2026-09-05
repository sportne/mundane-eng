# Task TC-0804: Test Allocation and Controlled Vocabulary

```json
{
  "format": "mundane-work-source-0.1",
  "status": "Complete",
  "dependencies": [
    "TC-0603"
  ],
  "relations": [],
  "planning": {
    "stage": "8",
    "type": "Model-pressure experiment",
    "condition": "TC-0603 and a corpus with real allocation change",
    "unlocks": "An allocation model decision",
    "statusNote": ""
  }
}
```

## Question

When does an allocation label need referential identity, vocabulary control, or relationship semantics?

## Outcome

A corpus experiment compares plain labels, project-policy vocabularies, and separately identified allocation targets through rename and reallocation workflows.

## Work

- Use heterogeneous target types and at least one target rename.
- Exercise reallocation without changing normative statements.
- Test typo detection and vocabulary evolution as policy.
- Compare inline label changes with references to separately identified targets.
- Assess whether one requirement may require multiple allocations.

## Acceptance evidence

- The experiment separates display name from target identity.
- Rename and reallocation diffs are compared.
- Policy enforcement is not confused with language parsing.
- No general systems-modeling scope is introduced.

## Out of scope

- SysML or component architecture modeling.
- Cross-repository allocation packages.
- A configurable type system.

## Completion decision

Keep the plain label unless referential continuity or multi-target semantics solve a demonstrated workflow that project policy cannot.

Completed on 2026-08-29. [Experiment 0020](../../experiments/0020-allocation-model/README.md)
retains the plain label for singular display-oriented allocation and separates
allowed-value policy from parsing. Synthetic rename and multi-target scenarios
show what stable companion target references could represent but do not
demonstrate a real need, so the companion remains deferred.

## References

- [Roadmap allocation study](../0001-initial-roadmap.md#allocation-and-controlled-vocabulary)
- [Minimum allocation model](../../specification/0002-minimum-source-language-and-model.md#allocation)
