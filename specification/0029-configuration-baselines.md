# Configuration baselines 0.1

The [source schema](schema/configuration-yaml-0.1.json) and `Configuration` model
implement the accepted bounded baseline design. Source/compiled/CLI identifiers
are independent declarations in `versions.properties`. The common
[domain envelope](0028-architecture-and-domain-boundary.md) supplies source locations,
exact source hashes, compiler metadata and explicit imports. Existing import and
requirement contracts remain unchanged.

## Selection and applicability

A configuration owns ID, stage, environment, observation basis and slot selections.
A baseline owns ID, member aliases and source mappings. Aliases and slots are unique;
selected members must exist and apply to the configuration environment. The first
contract supports simulation and field-proposed applicability, with design intent,
synthetic assembly and synthetic deployment. These records cannot establish actual
physical installation. The purpose is explicitly simulation-engineering or design-probe-only.

Members select compiled requirements, compiled architecture, or a closed native
format adapter (illustrative equipment YAML, Python source, version JSON, retained
requirement/attribute/architecture YAML, or plain text). Each declares path, SHA-256,
kind, format, required availability and applicability. Native files are retained bytes;
they are not interpreted as domain facts. Compiled members use their owner's validators.

Source mappings join a compiled artifact alias and its source path to a selected
member alias. Every available compiled source (including attribute declarations)
requires exactly one mapping with matching bytes. Extraneous or conflicting mappings
are rejected. An unavailable optional artifact leaves its mappings unverifiable;
it cannot supply successful availability evidence.

## Resolution, predecessors and comparison

Only an absent optional path yields optional-resource-unavailable. Changed bytes,
invalid formats, path escapes, unreadable inputs and required missing resources fail.
Resolution is local and bounded; all selected reads are captured and rechecked,
including nested imports. Recursion is limited to 16 and aggregate reads to 128 MiB.

Built states reference the exact **compiled** designed baseline; deployed states
reference the compiled built baseline. Predecessors must have the correct stage,
identity and applicability. Proposed changes also pin their compiled predecessor.
The former draft's source-document predecessor pins are not compiled revision pins.

Comparison joins slot selections. Member alias/kind/format/content/availability or
applicability changes are distinguished from locator-only changes. Configuration
stage, environment and observation basis changes are reported separately. This is a
bounded selection comparison, not risk assessment or generic semantic invalidation.

## Retained publication

`publish` validates the baseline and requires all members, including optional ones,
to be available. It retains the compiled input, authored source and captured resources
in `STORE/SHA256/root/` using their original root-relative paths. The directory name
is the hash of the canonical compiled envelope. The returned receipt names that root
and the original compiled input path. It does not assert authorization.

A local exclusive writer lock serializes publication of the same revision. Files
are staged in a temporary directory; inputs are rechecked before an atomic same-filesystem
move. Existing publication bytes are checked and never overwritten. A damaged existing
publication is an error. This is a local cooperative filesystem protocol, not a remote
immutable object store, authenticated approval service or protection against an external
privileged process changing files after the last check. Interrupted temporary/lock files
may require operator recovery after establishing no writer remains.

A retained root can be copied to a clean directory and resolved or recompiled without
the original checkout's resources. Schema validation, reproducibility and availability
remain distinct from the adequacy of evidence and permission to deploy.
