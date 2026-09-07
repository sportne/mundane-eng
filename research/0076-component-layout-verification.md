# Component layout verification

Completed for TC-1104 on 2026-09-07. The
[component decision](../distribution/components.md) records ownership, dependencies,
source moves, alternatives and preserved interfaces.

## Observed change

Before this work, every native-tool target depended on the single compilation of
all production and test sources. Afterward, native targets use explicit production
component classpaths and focused suites. Eleven source sets own 58 maintained Java
sources plus the generated Versions source exactly once. Their source counts are:
shared 3, yaml 1, requirements 22, artifacts 8, plan 2, verification 2, work-model 6,
work-source 3, work 1, impact 6 and editor 5 (including the generated source).

Five editor production files and three integration test files moved into
`editors/bridge`, with byte-identical Java contents. Other domain paths and the
existing Java entry points, source contracts and versions remain unchanged.
The aggregate test class directory is derived from component builds and is not a
production compiler input.

## Reproducible verification

Used Java/GraalVM CE 21.0.2 and Node 22 from the documented local installations:

```sh
export PATH=/home/jack/.sdkman/candidates/java/21.0.2-graalce/bin:/home/jack/.cache/ms-playwright-go/1.50.1:$PATH
make verify
python3 scripts/check-ci-failure-propagation.py
make work-backlog-verify
bash scripts/check-work-yaml.sh
```

`make verify` completed successfully in both the working checkout and a fresh
source export created from the staged implementation, with its own Git metadata
and no copied build outputs, dependencies or editor installation. The fresh tree
fetched its own pinned dependencies and VS Code runtime. Later changes only add
navigation documentation and record task completion; planning/schema checks cover
that final metadata. No implementation changes followed these successful gates.

Both full gates passed:

- All 14 maintained JVM test groups, independently compiled production components
  and focused suites for the native targets.
- Unique source ownership and acyclic dependency checks. Four real javac probes
  reject forbidden dependencies: requirements to work, verification to plan source,
  impact to work source and artifacts to the requirement parser.
- A separate temporary requirements-only source build without engineering domains,
  editor sources, tests or aggregate classes. Validator, formatting check and trace
  operations succeed on the maintained authoring fixture.
- Existing semantic/golden corpora, YAML schemas, CLI/native parity, parser-removal
  checks, seeded workflows and behavioral mutations for attributes and impact.
- Native package inventory, checksums, compatibility limits and independently
  installed validator/formatter/trace operations.
- Nine Node unit tests, actual VS Code provider and request-freshness tests,
  repeatable editor packaging, and the installed VSIX/extracted bridge workflow.
- Deterministic backlog rebuild and source/documentation link checks.

The CI failure-propagation checker injected a dangling requirement reference and
an invalid requirement schema. `make verify` failed at `test` and
`yaml-schema-verify`, respectively; the checker restored exact original bytes and
both tracked files were confirmed unchanged afterward.

Final closure regenerates the selected 99-card backlog and reruns the owning
backlog/documentation and independent YAML/schema checks. The historical TC-1101
decision is refined with the concrete mapping; TC-1104 owns this implementation
and evidence. The GCS plan now points to the completed component boundaries.

## Limits and retained decisions

This demonstrates production isolation and preserved tested behavior, not faster
builds or new engineering artifact support. Focused tests can explicitly depend
on collaborating components: plan/verification share integration tests, and editor
integration tests use the work command. Production dependencies remain separate.
Recipes sharing output directories run serially. Existing compiled-consumer
runtime isolation checks complement compile-time boundaries; neither forbids
arbitrary reflective access as a security sandbox. No build-system migration,
Java package renaming or speculative GCS domain directories were introduced.
