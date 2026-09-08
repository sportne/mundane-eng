# Maintained component boundaries

Decision: TC-1101 refined and implemented by TC-1104 on 2026-09-07. Component
ownership and allowed production dependencies are executable declarations in
[scripts/components.py](../scripts/components.py). The source languages, CLI names,
Java entry-point names and independently declared versions are unchanged.

## Ownership and physical mapping

Paths below are relative to the repository. Existing package directories already
identify most ownership; they remain in place. Build components are explicit source
sets, not a new Java module system or an external build-system dependency.

| Component | Owned source, relative to `src/main/java` unless stated otherwise | Production dependencies |
| --- | --- | --- |
| shared | `mundane/json`, `mundane/attributes`, generated `mundanereq/Versions.java` | JDK |
| yaml | `mundane/yaml` | shared, SnakeYAML |
| requirements | `mundanereq`, excluding the relocated editor | shared, yaml, SnakeYAML |
| artifacts | `engineering/artifacts` | shared |
| plan | `engineering/verification/PlanCompiler.java`, `PlanMain.java` | artifacts, yaml |
| verification | `engineering/verification/Verifier.java`, `VerifyMain.java` | artifacts |
| work-model | `engineering/work/WorkArtifact.java`, `WorkGraph.java`, `WorkValues.java`, `WorkAnalyzer.java`, `WorkView.java`, `WorkResult.java` | artifacts |
| work-source | `engineering/work/WorkCompiler.java`, `WorkYaml.java`, `WorkEditor.java` | work-model, SnakeYAML |
| work | `engineering/work/WorkMain.java` | work-source |
| impact | `engineering/impact` | work-model |
| editor | `editors/bridge/src/main/java/mundanereq/editor` (repository-relative) | requirements, work-source |

Dependencies are transitive. Shared metadata keeps its existing Java package name
for compatibility; it is generated and owned by shared, not by requirements.
The strict JSON facade in artifacts stays in place. Attribute rules are shared
compiled-value checks, not a source parser. WorkEditor belongs to work-source
because its assistance interprets that domain's YAML; the bridge coordinates domains.

The only source moves are:

| Before | After | Reason |
| --- | --- | --- |
| `src/main/java/mundanereq/editor/` | `editors/bridge/src/main/java/mundanereq/editor/` | Make the cross-domain editor integration an explicit owner alongside the VS Code client |
| `src/test/java/mundanereq/editor/` | `editors/bridge/src/test/java/mundanereq/editor/` | Keep bridge tests with that owner |
| `src/test/java/engineering/work/WorkSnapshotTest.java` | `editors/bridge/src/test/java/engineering/work/WorkSnapshotTest.java` | This test exercises bridge integration and must not make standalone work builds depend on the editor |

Java packages remain unchanged, preserving package-private test access and existing
entry points. Requirements, plans, work items, impact, examples, specifications,
historical experiments and native-package entry points retain their existing paths.
No speculative directories are created for future GCS domains.

## Build and test interactions

Use the [normal prerequisites](build-verification.md). Examples from the root:

```sh
make test-requirements
make native-validator native-formatter native-trace
make test-work native-work
make test-editor native-editor
make test
make component-boundary-verify
make verify
```

Focused suites exist for requirements, artifacts, plan, verification, work, impact
and editor. `native-link` uses the artifacts suite. Plan and verification tests
intentionally exercise each other, but their production compilation is separate.
Editor tests explicitly depend on the work command; work's own suite does not
depend on the editor. Production output never includes test classes.

`build-components.py build COMPONENT` compiles only the named production component
and its dependency closure after version generation and YAML dependency setup.
`build-components.py classpath COMPONENT` prints its production classpath. The
maintained native recipes use these classpaths instead of the aggregate test output.
A requirements build therefore cannot be blocked by broken work/editor Java sources.
The build compiles with an empty sourcepath, so javac cannot silently pull in
undeclared repository sources. A failing declared dependency still fails the build.

Each component writes to `build/maintained/components/COMPONENT/classes`; focused
tests use its `test-classes` directory. `make test` assembles a disposable aggregate
at `build/maintained/classes` for the existing integration scripts and runs all 22
maintained test groups. This is a compatibility test view, never a component's
production compile input. It is rebuilt from scratch, including after deletions.

Recipes sharing these outputs run serially, including when invoked with `make -j`.
Do not run separate build processes against the same checkout simultaneously.
Incremental compilation, parallel component builds and build-time speed guarantees
are outside this change. Full CI still runs the authoritative verification gate,
including native packages and installed-editor checks.

## Boundary evidence and acceptance

The [boundary check](../scripts/check-component-boundaries.py) enumerates every
maintained production source exactly once and checks the dependency graph. It
uses real javac probes to reject requirements-to-work, verification-to-plan-parser,
impact-to-work-parser and artifacts-to-requirement-parser dependencies. It also
builds the requirements closure in a temporary source tree without other domains,
tests or aggregate classes, then exercises validation, formatting checks and trace.
Existing parser-removal tests continue to verify compiled consumers at runtime.

TC-1104 completed with those checks, the full verification gate, native package
isolation, installed-editor verification and documentation/index checks passing.
[Execution evidence](../research/0076-component-layout-verification.md)
records results and practical limits.

## Decision and alternatives

The measurable benefit is enforced production dependency selection and independent
native-tool build prerequisites. The previous global source/test compilation imposed
unrelated failures on every tool despite requirements already compiling alone.
Moving all packages into new top-level modules would create additional path churn
without strengthening this enforcement. Moving only the editor clarifies an existing
cross-domain owner. Maven/Gradle adoption, JPMS descriptors, Java package renaming
and a universal domain model are not needed for this result.

Future GCS ownership decisions extend these source sets only when concrete code
exists. Shared must remain bounded infrastructure; domain compilers must not be
added to it to evade dependency rules. Any new compiler/consumer boundary requires
an explicit dependency decision and an appropriate isolation check.

## Architecture domain

TC-2405 adds `domain` (artifact infrastructure, bounded structure and provenance),
`domain-source` (YAML decoding and origin capture), `architecture-model` (compiled
schema, semantics and views), and `architecture` (CLI/source wiring). The model uses
only `domain`/`artifacts` and the JDK. Source wiring uses `domain-source` and its YAML
dependency. Shared infrastructure contains no architecture-specific field meanings.
`test-architecture` has a test-only requirements dependency to produce real imports.
`native-architecture` builds its independently tested closure. Boundary probes reject
YAML/source compiler dependencies from the compiled architecture model.

TC-2409 adds `configuration-model` (selection, comparison and retained publication),
using architecture's compiled model, and `configuration` (source/CLI wiring).
Nested validation shares the bounded snapshot capture and recheck infrastructure.
`test-configuration` exercises publication and failure semantics with real files;
`configuration-verify` exercises the native GCS workflow and independent schemas.

TC-2407 adds `safety-model`, using compiled configuration/architecture models and
existing requirement/plan adapters, plus `safety` for source/CLI wiring. FMEA and
fault-tree semantics stay with this owner. `test-safety` checks graph constraints;
`safety-verify` exercises the public native commands, GCS cases and source-free
consumer. Compiler probes reject YAML dependencies from the safety model.

TC-2411 adds `procedure-model`/`procedure` and `evidence-model`/`evidence`. Compiled
models use bounded configuration/architecture/requirement/plan adapters without
YAML source readers. Evidence owns the deterministic event simulator and criterion
interpreter; its CLI owns manual-source normalization and assessment compilation.
`test-evidence` also builds the procedure CLI and exercises simulator/evaluator
separation. `evidence-verify` runs both native commands and adversarial provenance
workflows; model compiler probes reject YAML/source dependencies.

TC-2417 adds `software-model` (project relationships and independent SLSA/CycloneDX
adapters), depending on compiled safety/configuration/architecture, and `software`
for YAML/CLI wiring. Its isolated consumer requires no YAML parser.

TC-2413 adds `equipment-model` on compiled configuration/architecture and `equipment`
for source/CLI wiring. BOM and wiring share the same checked connectivity model.

TC-2415 adds `budget-model` (dimensional interval arithmetic and conditional repair
models) on compiled equipment/configuration, and `budget` for source/CLI wiring.
The three new domains bring the aggregate suite to 21 maintained groups.

TC-2419 adds `assurance-model` on compiled software and evidence and `assurance`
for source/CLI wiring. Standard DSSE/JWK verification uses the JDK; compiled checks
have no YAML dependency. The suite has 22 maintained groups.
