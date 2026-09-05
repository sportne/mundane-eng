# Task TC-1502: Centralize Independent Version Declarations

```json
{
  "format": "mundane-work-source-0.1",
  "status": "Complete",
  "dependencies": [],
  "relations": [],
  "planning": {
    "stage": "15",
    "type": "Implementation and documentation",
    "condition": "none",
    "unlocks": "TC-1201",
    "statusNote": ""
  }
}
```

## Question

How can independently versioned source, tools, packages, and compiled formats avoid
drift as more components are developed in one repository?

## Outcome

One authoritative declaration location feeds current version identifiers while
preserving distinct version domains and documenting experimental change policy.

## Work

- Inventory duplicated TOOL_VERSION/SOURCE_CONTRACT constants, SUITE_VERSION,
  package metadata, documentation claims, and compatibility checks.
- Select one declaration mechanism that works with the Java/Make
  build; keep source contract, individual CLI contracts, package, and compiled
  formats separately identified.
- Make current CLI output/build metadata/checks consume it; document how TC-1201's
  later compiled-format identifiers enter the same mechanism.
- Define compatible additions versus intentional breaking syntax/semantic/output
  changes, migration notes, supported prior contracts, and rejection of unknown
  formats. Preserve historical normative documents and tagged identifiers.
- Document Java 21, selected GraalVM/native-image, C toolchain, Linux architecture
  and glibc assumptions, distinguishing tested combinations from untested ones.

## Acceptance evidence

- A declaration inventory maps every current version consumer to its source and
  identifies immutable historical contracts separately.
- A fixture or build check detects deliberate disagreement between command/package
  declarations; changing one version domain does not silently change the others.
- Existing --version values remain correct, and generated metadata agrees with the
  selected declarations.
- Written examples classify an additive field and a changed field meaning, with
  consumer behavior and a migration-note procedure for the experimental series.
- The supported-build table cites actual repository evidence and labels unknown
  combinations without inventing successful builds.

## Out of scope

- Coupling all versions to one number, long-term API promises, release publication,
  broad new platform packaging, or editing historical contract identifiers.

## Compatibility and affected components

Present problem: duplicated constants and suite metadata can diverge today.
Components: CLI constants, Makefile, build metadata, contracts/documentation, and
focused version-consistency checks. Existing Linux checksums/notices remain reused.

## Completion decision

Choose the smallest mechanism that removes declaration drift. Reject a build-system
migration or a shared version number that changes independent interface meanings.

## References

- [Roadmap](../0001-initial-roadmap.md)
- [Makefile](../../Makefile)
- [Native distribution](../../distribution/README.md)
- [Packaging evidence](../../research/0017-native-suite-packaging.md)

## Completion evidence

Completed by [Research 0039](../../research/0039-independent-version-declarations.md) and versions.properties. `make version-verify native-suite-verify` passed all maintained JVM groups, declaration/mutation checks and native package metadata/identity/isolation checks. Historical contracts and current --version values are preserved. TC-1201 is now Ready.
