# Task TC-1104: Establish the Monorepo Component Layout

```json
{
  "format": "mundane-work-source-0.1",
  "status": "Conditional",
  "dependencies": [
    "TC-1101"
  ],
  "relations": [],
  "planning": {
    "stage": "11",
    "type": "Implementation",
    "condition": "TC-1101",
    "unlocks": "follow-up work selected by the completion decision",
    "statusNote": ""
  }
}
```

## Question

Can the selected monorepo boundaries be made visible without breaking independent
tools, historical evidence, or contributor verification?

## Outcome

The layout selected by TC-1101 is implemented with a reviewable mapping of moved
paths and equivalent observable requirements-tool behavior.

## Work

- Execute only TC-1101's justified moves or dependency boundaries. Keep unrelated
  language changes and cleanup in their own cards.
- Update build/source discovery, documentation links, package assembly, and tests
  affected by those moves. Preserve historical experiment reproducibility or supply
  an explicit compatibility path.
- Keep validator, formatter, and trace independently buildable and installable.
- Add a focused boundary check where a new module boundary prevents an identified
  accidental dependency. Coordinate moves with any active source changes.

## Acceptance evidence

- Before/after path and responsibility inventories explain every move.
- The authoritative verification command and existing command examples pass in a
  clean checkout; semantic inventories and formatter outputs remain equivalent.
- Requirements-only operations succeed without verification or view components;
  the removal-isolation checks still pass.
- Every changed documentation/experiment reference resolves, and no empty
  speculative component or generated authoritative source appears.

## Out of scope

- Repository renaming, a build-system migration, feature implementation, or moving
  historical files solely to make the directory tree uniform.

## Compatibility and affected components

Preserve current source and CLI behavior, package entry points, and the documented
Java/GraalVM baseline. Components are src/, Makefile, distribution/, CI path
references, and component documentation only where the selected moves require it.

## Completion decision

Perform this card only when TC-1101 identifies a measurable boundary or navigation
benefit. If the existing physical layout is sufficient, record that decision and
supersede this card rather than manufacture file movement.

## References

- [Roadmap](0001-initial-roadmap.md)
- [TC-1101](closed/task-1101-define-monorepo-component-boundaries.md)
- [Native packaging evidence](../research/0017-native-suite-packaging.md)
- [Integrated trial](closed/task-0603-run-the-integrated-toolchain-trial.md)
