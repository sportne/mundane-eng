# Clean-Checkout CI Workflow

Status: Decided

Date: 2026-08-29

Roadmap task: [TC-0602](../roadmap/closed/task-0602-create-the-clean-checkout-ci-workflow.yaml)

## Question

Can the formatter, validator, and trace executable participate visibly and
independently in an ordinary clean-checkout CI workflow?

## Workflow

The maintained GitHub Actions example installs GraalVM Community Edition 21,
builds the three native images, and invokes each executable in its own named
step against the tracked 0.2 conformance corpus:

    make native-suite
    build/maintained/mundanereq-validate conformance/0.2/valid
    build/maintained/mundanereq-format --check conformance/0.2/valid
    build/maintained/mundanereq-trace parents LEAF conformance/0.2/valid

These are also the complete local equivalents. There is no workflow wrapper,
configuration language, persistent index, service, or generated report. The
only derived artifacts are disposable class files and native executables under
the ignored `build/` directory.

## Failure and independence evidence

`make ci-workflow-verify` repeats the successful invocations and deliberately
supplies each executable with one failure that only its named step owns:

- noncanonical valid source makes formatter check exit 1 with `Needs formatting`;
- malformed source makes validation exit 1 with `content-outside-record`;
- an absent query ID makes trace exit 2 with `missing-requirement`.

The verification copies only formatter and validator into a temporary
installation and repeats their successful checks with no trace executable
present. It also checks that the authoritative workflow source is unchanged.
Temporary failure fixtures and the reduced installation are deleted after the
test. The commands consume only tracked source paths and process memory after
the toolchain has been installed.

## Decision

Retain explicit commands. The observed sequence is short, attributes failures
to a single capability, and exposes each tool's independent contract. A wrapper
or project-specific workflow configuration would add indirection without
removing material repetition. The CI example is optional infrastructure; the
authoritative requirements remain understandable and reviewable when it is
absent.

The setup action is pinned to the reviewed v1.6.4 commit. Its documented
`graalvm-community` distribution and Java 21 selection provide `javac`, `java`,
and `native-image`; action installation is the workflow's only tool-download
phase.

## Sources

- [GraalVM setup action](https://github.com/graalvm/setup-graalvm)
- [Requirements workflow](../.github/workflows/requirements.yml)
