# Native requirement tools

`make package-native-suite` creates a Linux x86-64 archive containing the independent
`mundanereq-validate`, `mundanereq-format` and `mundanereq-trace` binaries. YAML 0.3
is the default; each command can explicitly select YAML 0.4 with attributes.

The archive includes checksums, version declarations, command/source contracts,
YAML schemas and dependency notices. Inspect its SHA256SUMS and sidecar checksum.
Extract it and place `bin/` on PATH. The documented glibc symbol ceiling is 2.34.
The build needs Java 21, GraalVM Native Image, GCC, Make and the prerequisites in
the [build guide](build-verification.md). Other tools have independent native targets.

Packaging is a distribution convenience; no engineering approval or long-term
compatibility promise follows from it. Source remains authoritative.

[Assurance commands](assurance.md) inspect exact evidence, signed reviews and time-scoped obligations.
