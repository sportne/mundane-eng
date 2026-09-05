# Research 0049: SARIF validation diagnostics

Task: [TC-1504](../roadmap/closed/task-1504-emit-sarif-validation-diagnostics.yaml).

The written [command decision](../specification/0017-sarif-validation-output.md)
selected OASIS SARIF 2.1.0 Errata 01, an explicit output selector/root, stable
compiled rule IDs, Unicode code-point coordinates and truthful point regions.
The interpreter has no diagnostic end spans; none are fabricated. Existing
source/command versions remain separate; the validator command contract gains
`+sarif-1`, and SARIF's independent format version comes from versions.properties.

The validator interprets the selected byte snapshot once and emits SARIF without
parsing its own human-readable output. Its serializer shares the requirements
compiler's deterministic JSON writer and known-valid UTF-8-prefix coordinate
conversion. Requirements remain usable without linker/plan/analyzer classes.
No runtime library was added. Default human-readable validator behavior and
compiled artifact golden bytes remain covered by their existing checks.

[Experiment 0032](../experiments/0032-sarif-diagnostics/README.md) supplies six
public-output goldens and an independent point-navigation consumer. Source errors,
fully parsed semantic violations and input failures have distinct completeness
and execution status. Unknown/duplicate options yield no artifact. Schema checks,
URI encoding and output failures are verified separately from validation validity.
No external-editor compatibility or hosted upload evidence is claimed.

The schema is copied unchanged from the
[OASIS standard](https://docs.oasis-open.org/sarif/sarif/v2.1.0/errata01/os/schemas/sarif-schema-2.1.0.json),
with a checksum and associated notices. The source specification defines
[code-point columns and execution success](https://docs.oasis-open.org/sarif/sarif/v2.1.0/errata01/os/sarif-v2.1.0-errata01-os-complete.html).

Recorded execution: `make native-validator compiled-verify version-verify` passed,
then `make sarif-verify native-suite-verify` passed with 17 JVM groups, six
schema-valid golden cases, JVM/native parity, exact source navigation and package
checks. The updated version-declaration check also passed against actual emitted
SARIF metadata. Java/GraalVM CE 21.0.2 on Ubuntu 24.04 x86-64.

The stream matrix distinguishes a closed Java PrintStream from an externally
closed OS descriptor that receives no writes. Quiet SARIF output cannot portably
detect external closure of unused stderr. Actual stdout delivery, stderr diagnostic
writes, prefix and flush failures are checked; this limit is part of the contract.
The existing package now includes the recovery/rule/SARIF command documentation.
