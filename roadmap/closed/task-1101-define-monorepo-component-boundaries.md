# Task TC-1101: Define Monorepo Component Boundaries

```json
{
  "format": "mundane-work-source-0.1",
  "status": "Complete",
  "dependencies": [],
  "relations": [],
  "planning": {
    "stage": "11",
    "type": "Decision",
    "condition": "none",
    "unlocks": "TC-1102, TC-1104",
    "statusNote": ""
  }
}
```

## Question

How can this repository host an engineering tooling ecosystem while requirements
remain an independently usable language and tool suite?

## Outcome

A written architecture decision assigns component responsibilities, permitted
dependencies, public artifact boundaries, and the smallest useful monorepo layout.
This enables TC-1104's bounded reorganization and TC-1102's ownership decisions.

## Work

- Map existing Java sources, tests, experiments, contracts, native commands, and
  packaging to requirements, artifact integration, verification, views, and shared
  test infrastructure. Distinguish current code from prospective responsibilities.
- Compare logical boundaries in the existing layout with physical component
  directories. Record a concrete benefit for every proposed move.
- Scope the YAML source decision to requirements. Leave other artifact authoring
  formats to their own workflow decisions, including reuse of existing formats
  and native engineering files. Shared integration contracts concern compiled
  interfaces, references, provenance and linking; source notation remains separate.
- Define which consumers use published compiled interfaces and where shared
  implementation is appropriate; describe a test that exposes accidental use of
  requirement implementation internals.
- Distinguish this tooling monorepo from users' engineering repositories. Allow
  future same-repository and cross-repository artifacts without requiring a server.
- Preserve independent command installation and source readability. Record how
  existing source, CLI, artifact-format, and package version domains remain distinct.

## Acceptance evidence

- A decision record contains a component/dependency diagram, a current-to-proposed
  path map, alternatives, and ownership of each public contract.
- A requirements-only build/use scenario excludes verification and view tooling;
  a compiled-output consumer scenario does not need original parser internals.
- Every proposed component contains an existing capability or the first bounded
  workflow; future safety, BOM, and CAD integrations appear as possibilities, not
  empty frameworks to implement.
- The boundary record distinguishes each component's source-format decision from
  its published compiled interface. Only requirements inherit the YAML decision;
  other source formats are recorded as existing experimental inputs or unresolved.
- The decision identifies the verification workflow as the first integration and
  identifies observable checks required before moving files.

## Out of scope

- Moving code, renaming the repository, choosing a separate language per artifact,
  a universal engineering metamodel, registries, or plugin infrastructure.

## Compatibility and affected components

Plan preservation of current .mreq semantics, human-authored IDs, explicit source
selection, native commands, and historical experiment paths. Likely components:
root documentation, src/, Makefile, distribution/, and future component boundaries.

## Completion decision

Select only boundaries with an identified consumer or independence benefit. Keep
the current physical layout where movement adds coordination without improving a
checkable boundary. Revisit any design that requires all ecosystem tools to use
requirements.

## References

- [Roadmap](../0001-initial-roadmap.md)
- [Project foundation](../../specification/0001-project-foundation.md)
- [Implementation lineage](../../research/0011-maintained-implementation-lineage.md)

## Planning refinement

Clarify that component boundaries do not select a common authoring notation.
The requirements YAML decision has requirements-only scope. Status and
dependencies remain unchanged.

## Completion evidence

Completed by [Research 0036](../../research/0036-monorepo-component-boundaries.md): dependency diagram, path/contract ownership map, executable independence scenarios and alternatives. Retain current paths; TC-1104 remains Conditional because no physical move currently has a demonstrated benefit. TC-1102 is now Ready.
