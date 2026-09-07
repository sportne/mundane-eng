# Mundane-Eng

A text-based engineering tooling monorepo. Requirements, work items and verification
plans are authored in checked-in source; independent tools compile, validate, link
and analyze them. Generated artifacts and reports are rebuildable views.

Human-authored IDs remain identity. Git records revisions and review history.
Requirements own their statements and descriptive attributes; plans and work items
own their assertions and workflow state. Linking does not imply approval, execution
or requirement satisfaction. Human-authored engineering artifacts use YAML by default,
with independently owned domain fields and semantics.

The repository is [mundane-eng](https://github.com/sportne/mundane-eng).
Requirement-specific commands retain the `mundanereq-*` prefix; broader engineering
tools use `mundane-*`. Existing format identifiers, Java packages and editor
extension identifiers remain compatible.

## Current capabilities

| Component | Authoritative source | Tools and examples |
| --- | --- | --- |
| Requirements | YAML 0.3 (default) or explicitly selected YAML 0.4 with project attributes | [Validate](distribution/validate.md), [format](distribution/format.md), [trace](distribution/trace.md), [compile](distribution/compile.md); [YAML examples](examples/yaml/README.md) |
| Project attributes | Explicit YAML project declaration plus YAML 0.4 values; text/enum types, required/optional presence, no defaults | [Contracts and capabilities](distribution/attributes.md); [small and medium examples](examples/attributes/README.md) |
| Verification planning | Independently specified YAML plans, activities and coverage assertions | [Plan compilation, scoped imports and review analysis](distribution/verification.md) |
| Work items | YAML task/issue records, literal narrative strings and typed relationships | [Authoring and commands](distribution/work-items.md); [repository backlog](WORK-ITEMS.md) |
| Impact analysis | Explicit compiled requirements, plans and work-item imports | [Bounded queries and explained reports](distribution/impact.md); [logger example](examples/impact/README.md) |
| VS Code authoring | Explicit requirement and YAML work-item selections, including unsaved buffers | [Local authoring and imported-target navigation](editors/vscode/README.md); [Linux bundle](distribution/editor-bundle.md) |
| Architecture | YAML context, allocations, modes, interfaces and decisions; explicit compiled requirement imports | [Native compile/check/view](distribution/architecture.md) |
| Configuration baselines | YAML selections, exact resource/source pins and designed/built/deployed distinctions | [Compile, resolve, compare and retain](distribution/configuration.md) |
| Safety analysis | YAML hazards, controls, assumptions, FMEA and qualitative fault trees | [Compile, query and inspect coverage/review gaps](distribution/safety.md) |
| Procedures and evidence | YAML procedures, manual observations and assessments; native JSON simulation logs | [Procedure compilation](distribution/procedure.md), [execution/import and evidence queries](distribution/evidence.md) |
| Derived reports | Compiled snapshots and explicit analysis results | [Experimental verification report](experiments/0029-verification-report/README.md); work-item views via `mundane-work view` |

Compiled interfaces are versioned separately from source languages and tools.
Attribute changes and whole-schema changes participate in conservative review
analysis. Current development is incremental and experimental. ReqIF interchange and
additional engineering artifact domains remain future work. The
[ground control station reference example](examples/ground-control-station/README.md)
provides a runnable workflow with current tools and tested designs for additional
artifacts. Its task cards distinguish completed design work from planned production
capabilities.

## Build and try

Use Java 21 with GraalVM Native Image and the
[documented build prerequisites](distribution/build-verification.md). For the tested
SDKMAN installation:

```sh
sdk use java 21.0.2-graalce
make test
make native-suite native-compile native-plan native-link native-verification native-work native-impact
```

From the repository root:

```sh
build/maintained/mundanereq-validate --source=yaml-0.3 examples/yaml/vaccine-monitoring
build/maintained/mundanereq-format --source=yaml-0.3 --check examples/yaml/vaccine-monitoring
build/maintained/mundanereq-trace --source=yaml-0.3 impact SYS-001 examples/yaml/vaccine-monitoring
python3 experiments/0036-project-attributes/workflow.py
```

The last command rebuilds the attribute example and its report at
`build/attribute-example/report.html`. Each native tool is independently usable.
`native-suite` builds the three requirement validate/format/trace commands; the other
commands have separate targets. The [native package](distribution/README.md) also
contains only those three commands.

## Verify and contribute

`make verify` is the authoritative JVM/native gate, including schemas, YAML fixtures, source-to-report workflows, mutation checks, documentation links and the
backlog rebuild. `scripts/run-ci-verification.sh` runs that gate, records the environment
and tests CI failure propagation. Individual cards and research records identify the inputs and environment actually verified.

The first build downloads a checksummed YAML parser. Full verification also uses
pinned Python schema-checking dependencies; these are not native runtime dependencies.
Build output is disposable under `build/`. `make clean` removes it and downloaded
build/test caches; see [cleanup details](distribution/build-verification.md#cleaning-the-checkout)
and [dependency details](dependencies/README.md).

Use the [living roadmap](roadmap/0001-initial-roadmap.md) for direction and the
[task-card workflow](roadmap/0002-task-card-index.md) for changes. Author status,
dependencies and evidence in YAML cards; regenerate `WORK-ITEMS.md` with `make work-index`.

## Repository guide

- [Specifications](specification/README.md): current contracts, supported historical profiles and authority boundaries.
- [Project foundation](specification/0001-project-foundation.md): model and ownership principles.
- [Research](research/README.md) and [experiments](experiments/README.md): dated decisions, fixtures and evidence; earlier conclusions retain their historical scope.
- `src/main/java/`: requirement tooling, shared primitives and independent engineering consumers.
- `src/test/java/`, `conformance/` and `scripts/`: owning regression and integration checks.
- [Distribution](distribution/README.md): installation, platform assumptions and package contents.

The [VS Code extension](editors/vscode/README.md) provides YAML highlighting,
compiler diagnostics, reference navigation, conservative formatting, attribute
completion and hover documentation for explicitly selected requirement projects.
Build and install the local VSIX; the native bridge is built separately for the host.
