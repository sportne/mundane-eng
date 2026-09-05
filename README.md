# mundanereq

**Mundane requirements management.**

`mundanereq` explores a simple idea: requirements should be human-readable source files stored in ordinary Git repositories, with requirements-specific tooling layered on top in the same way compilers, linters, renderers, IDEs, and CI systems are layered on top of software source code.

Its mission is to make requirements durable, human-readable source artifacts that work naturally with ordinary Git workflows and independent, composable tools.

The source representation is the foundation. A requirements file should be understandable and reviewable directly in a text editor or standard Git forge diff without requiring a proprietary application or custom renderer.

## Project principles

- Requirements source is human-readable text.
- Git provides version control, history, branches, immutable snapshots, and merge workflows. Those snapshots can supply baseline mechanics; baseline scope and authority may need conventions or tooling.
- Pull requests / merge requests are valid review mechanisms without requiring a requirements-specific review system.
- Requirements-specific tooling should be additive: parsers, validators, formatters, renderers, exporters, IDE integrations, and semantic diff tools can improve the experience without becoming the authoritative store.
- Requirements are model objects; documents and reports are views or generated artifacts.
- Derived indexes, databases, and rendered outputs should be rebuildable from the source repository.
- Early development should favor small working prototypes and iteration over speculative completeness.

## Repository layout

- [`specification/`](specification/README.md) — project, language, and tool
  contracts, with an index distinguishing normative standards from design
  records.
- `conformance/` — independently runnable examples for provisional source contracts.
- [`research/`](research/README.md) — surveys, evidence, and recorded design
  decisions.
- [`experiments/`](experiments/README.md) — concrete fixtures, protocols, and
  recorded experimental results.
- `distribution/` — maintained native-package documentation and notice policy.
- `roadmap/` — development strategy, experiments, and sequencing.
- `src/main/java/` — maintained shared implementation and later tool entry points.
- `src/test/java/` — maintained dependency-free tests.

## Current status

Requirements now have an explicitly selected [YAML 0.3 source
contract](specification/0010-requirements-yaml-0.3.md), with a normative structural
schema and separate semantic rules. The [authoring and migration
guide](examples/yaml/README.md) provides executable examples. That YAML decision
is specific to requirements; work items now have their own independently selected
YAML specification. Other artifact formats remain independent decisions.

The default source mode remains [custom 0.2](specification/0005-mundanereq-source-language-0.2.md).
Historical 0.1/0.2 contracts and fixtures are preserved. Three independent native
tools validate, format and trace either explicitly selected source contract;
`mundanereq-migrate` provides checked conversion into a new output directory.
The [command addendum](specification/0011-tool-safety-and-yaml-commands.md)
defines the source selector, output failure behavior and formatter snapshot checks.

[Experiment 0024](experiments/0024-vaccine-monitoring-pilot/assessment.md)
executed a 57-requirement, two-baseline formal-traceability pilot, retained the
source model, and selected a bounded verification-plan analyzer investigation.
`mundanereq-source-0.2` remains the default invocation contract.

The [current roadmap](roadmap/0001-initial-roadmap.md) describes an engineering
tooling monorepo with requirements as its first independently usable component.
[Experiment 0027](experiments/0027-compilation-linking/README.md) now demonstrates
compilation, explicit linking, coverage/review-staleness analysis and reproducible
rebuilds through independent requirements and plan adapters. Component and fact
ownership are recorded; current physical paths are retained.

[`mundanereq-compile`](distribution/compile.md) publishes a documented experimental
JSON artifact with semantic values, retained source ranges, checksums, diagnostics
and completeness. A consumer can use it without parser implementation classes.
The [independent companion commands](distribution/verification.md) now compile an
explicit TSV verification plan, link locally selected requirement artifacts, and
analyze planned coverage and review staleness. [Experiment 0029](experiments/0029-verification-report/README.md)
builds a disposable source-linked HTML report. TSV was selected independently for
this plan workflow; other artifact formats remain open decisions. The
[backlog](roadmap/0002-task-card-index.md) tracks implementation of the selected
[project-attribute design](research/0052-project-attribute-schema-decision.md):
explicit project declarations and descriptive text/enum values. Attributes are
not implemented yet. [Independent version declarations](versions.properties)
feed command builds and package metadata.

[Work items](distribution/work-items.md) now provide a separate YAML work-item
source profile for tasks and issues, with compilation, typed linking, prerequisite
analysis and a [derived backlog view](WORK-ITEMS.md). The repository uses that
workflow for its own task cards; source cards own status and completion evidence.

[Bounded diagnostic recovery](specification/0016-diagnostic-recovery.md) now retains
reliably parsed neighbors while keeping invalid sources incomplete. The validator
also offers [SARIF output](specification/0017-sarif-validation-output.md) with stable
rules and Unicode source points for downstream tooling. The
[workflow regression corpus](experiments/0033-workflow-regressions/README.md)
extends source-to-report rebuild checks across custom and YAML requirements,
including replayable generated cases and targeted implementation mutations.

The first code was a small [deterministic-interpretation
probe](experiments/0002-deterministic-interpretation/README.md) used to test
the provisional source language. It remains historical evidence rather than
production architecture. The [maintained implementation
lineage](research/0011-maintained-implementation-lineage.md) provides a Java 21
source area with lossless source handling and focused tools. The YAML reader adds
the pinned [SnakeYAML Engine dependency](dependencies/README.md).
Earlier experiments [transfer the minimum
model](experiments/0004-transferability/README.md) to a licensed NASA FRET
case-study corpus and add one [focused incoming trace
query](experiments/0005-incoming-trace-query/README.md) without changing
authoritative source.

A bounded [ReqIF 1.2 interchange experiment](experiments/0006-reqif-interchange/README.md) demonstrates schema-valid semantic self-roundtrips while keeping ReqIF derived and outside the source-language contract. [Experiment 0007](experiments/0007-source-comments/README.md) confirms that 0.2 author comments do not enter that semantic interchange model.

## Maintained build skeleton

Select a Java 21 GraalVM distribution with Native Image, such as the installed
SDKMAN candidate:

    sdk use java 21.0.2-graalce

The complete JVM and native clean-checkout gate is:

    make verify

The recorded formatter verification repair expanded coverage to every maintained
valid corpus; Experiment 0024 extends that inventory to 30 source sets and 64
files. The current formatter inventory includes 31 source sets and 65 files,
including the SARIF authoring example. The complete `make verify` gate is the
authoritative repository check.
The hosted workflow runs that command through
`scripts/run-ci-verification.sh`, which records tool versions and checks deliberate
failure propagation. [Build prerequisites and local CI reproduction](distribution/build-verification.md)
describe the tested Ubuntu 24.04, GraalVM CE 21.0.2 and Python 3.12 environment.
YAML verification adds 120 equivalent requirements across three
migration corpora, authoring fixtures, negative cases and JVM/native command checks.

The first build needs curl/network access to obtain a checksummed parser jar.
Builds use Python 3 to generate version declarations. Full verification needs
venv support for the pinned independent
schema check; these packages are not runtime dependencies. Cached dependencies
permit subsequent offline checks.

`make test` runs the maintained JVM regression suite. `make native-smoke` builds and runs a
test-only executable with `--no-fallback`. Generated classes and native output
are disposable under `build/maintained`. `make boundary-isolation` removes each
of the three built requirements executables in turn and proves that the others
still run independently. The validator, formatter, and trace
boundaries are separate focused product tools with maintained trial contracts.

## Validator

`mundanereq-validate` is the first focused maintained tool. Build its standalone
Linux native executable after selecting the GraalVM Java 21 SDKMAN candidate:

    make native-validator

Validate one or more explicit files or directories:

    build/maintained/mundanereq-validate requirements/

The validator implements `mundanereq-source-0.2`, reports diagnostics as
`file:line:column: category: message`, and uses exit status `0` for conforming
source, `1` for source nonconformance, and `2` when invocation or input failure
prevents validation. `--help` and `--version` describe the small current
interface. The command performs no project-policy, formatting, trace, or ReqIF
work.

The [validator trial contract](specification/0007-validator-trial-contract-0.1.md)
defines the historical `trial-0.1` interface, installation procedure,
compatibility boundaries, and known omissions. The Git tag
`validator-trial-0.1` identifies its reproducible source baseline. This is a
maintained, separately versioned trial interface.

`make validator-verify` runs the complete JVM/native validator evidence set,
including all conformance selections, maintained corpora, source-selection
cases, and representative editor repairs.

## Formatter

`mundanereq-format` is the second independent native tool. It implements the
narrow policy selected by [Experiment 0008](experiments/0008-formatting-policy/README.md):
normalize LF line endings and collapse comment-free inter-record blank-line
runs to one line. It does not reflow prose, reorder source, alter comments, or
interpret opaque math.

Build the maintained trial executable with:

    make native-formatter

Check or explicitly rewrite a complete selected source set:

    build/maintained/mundanereq-format --check requirements/
    build/maintained/mundanereq-format --write requirements/

Check mode returns `0` when every selected file is already formatted, `1`
when valid files would change, and `2` when invocation, source-set, or report
output failure prevents completion. Standard-output and write modes return `0`
on success and `2` on invocation, source-set, output, or replacement failure.

To write one formatted file to standard output while validating relationships
against its wider source context:

    build/maintained/mundanereq-format requirements/child.mreq requirements/

Formatting validates the complete selected source set before producing output
or replacing files. The [formatter trial contract](specification/0008-formatter-trial-contract-0.1.md)
defines the historical `trial-0.1` interface, formatting behavior, safety
boundary, and deliberate omissions. The Git tag `formatter-trial-0.1`
identifies its reproducible source baseline. This is a maintained trial
interface with its own version and compatibility boundary.

On filesystems with a POSIX permission view, write mode preserves the file's
read, write, and execute permission bits. Replacement does not promise to
preserve ownership, ACLs, extended attributes, or hard-link identity. Teams
that depend on those properties should use check or standard-output mode until
a broader metadata contract is justified.

## Trace

`mundanereq-trace` is the third independent native tool. Its maintained trial
interface answers four questions over valid `decomposes` relationships:

    mundanereq-trace parents ID requirements/
    mundanereq-trace children ID requirements/
    mundanereq-trace higher ID requirements/
    mundanereq-trace impact ID requirements/

Direct operations list immediate higher- or lower-level requirements.
Transitive operations show one deterministic shortest path per result and
report reachable cycles as non-fatal structural observations. Incoming links,
reverse indexes, paths, and cycle components are derived in memory and are
never written to source.

Build the executable with `make native-trace`; `make trace-verify` runs the
complete graph, workflow, file-layout, and JVM/native agreement evidence. The
[trace trial contract](specification/0009-trace-trial-contract-0.1.md) defines
the complete `trial-0.1` interface, deterministic human-readable output,
compatibility boundaries, and deliberate omissions. The Git tag
`trace-trial-0.1` identifies its reproducible source baseline. This is a
maintained trial interface. Its human-readable output is not a machine-readable
interchange contract.

## Native tool suite

The three maintained tools can be built together without becoming one
application:

    make native-suite-verify

This creates a baseline-x86-64, glibc-2.34 Linux archive under
`build/maintained/package/` containing three sibling executables, independent
usage documents, their complete trial contracts, checksums, and the license
notices from the selected GraalVM. There is no suite launcher, server,
database, or mandatory installation of all three commands. Each executable can
also still be built with its own `make native-*` target and installed
separately.

The [native distribution guide](distribution/README.md) defines the package
layout, installation and rebuild procedure, runtime platform dependencies, and
the responsibilities left to editors, Git, forges, CI, and project procedure.
[Research 0017](research/0017-native-suite-packaging.md) records the packaging,
removal-isolation, licensing, and reproducibility evidence. The Git tag
`native-suite-trial-0.1` identifies the source baseline; generated binaries and
archives remain ignored and disposable.

Work items now have a [YAML specification and schema](specification/0019-work-items-yaml-0.2.md),
with multiline body strings and explicit typed relationships. See the
[authoring workflow](distribution/work-items.md) and [YAML example](examples/work-items/yaml/task.yaml).
The older Markdown profile remains readable for compatibility.
