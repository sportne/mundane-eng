# Task TC-0601: Package and Document the Native Suite

```json
{
  "format": "mundane-work-source-0.1",
  "status": "Complete",
  "dependencies": [
    "TC-0303",
    "TC-0404",
    "TC-0504"
  ],
  "relations": [],
  "planning": {
    "stage": "6",
    "type": "Packaging and documentation",
    "condition": "TC-0303, TC-0404, and TC-0504",
    "unlocks": "TC-0602",
    "statusNote": ""
  }
}
```

## Question

Can users obtain and understand three separate native tools without mistaking them for one mandatory platform?

## Outcome

Reproducible Linux packages or build artifacts and concise independent documentation exist for validator, formatter, and trace.

## Work

- Produce a reproducible build of all three GraalVM native images.
- Document installation and one-purpose invocation for each executable.
- State tool and source-contract versions independently.
- Verify that generated binaries remain uncommitted and rebuildable.
- Document which ordinary editor, Git, forge, and CI responsibilities remain outside the suite.

## Acceptance evidence

- Each tool can be installed or built independently.
- Documentation never requires a database, server, or wrapper command.
- Removing one binary affects only its stated capability.
- Licenses and third-party notices are complete for distributed artifacts.
- A clean checkout reproduces the documented Linux build.

## Out of scope

- Windows and macOS packages without demonstrated users.
- An auto-updater or package manager.
- Bundling a custom editor or forge.

## Completion decision

If separate distribution creates substantial accidental duplication, share packaging mechanics without merging executable responsibilities.

## Result

Completed on 2026-08-29. `make native-suite-verify` builds, packages, and verifies three independent no-fallback baseline-x86-64 Linux executables with a checked glibc 2.34 symbol ceiling. The [distribution guide](../../distribution/README.md) and one-purpose tool documents describe separate installation and use without a wrapper, server, or database. [Research 0017](../../research/0017-native-suite-packaging.md) records representative isolated operations, exact archive and notice verification, runtime dependencies, checksums, and the source-reproducible rather than byte-identical build boundary.

The package shares build and notice mechanics without merging executable responsibilities. The `native-suite-trial-0.1` tag identifies the rebuildable source baseline; generated package artifacts remain ignored by Git.

## References

- [Validator contract](task-0303-publish-the-validator-trial-contract.md)
- [Formatter contract](task-0404-publish-the-formatter-trial-contract.md)
- [Trace contract](task-0504-publish-the-trace-trial-contract.md)
