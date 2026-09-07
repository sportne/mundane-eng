# Configuration, baseline and change design — TC-2408

Accepted bounded design on 2026-09-07. The
[draft source schema](configuration.schema.json), five YAML manifests and
[design probe](check-configuration.py) define a worked configuration-management
slice. TC-2409 now provides the [maintained compiler/resolver](../../../distribution/configuration.md)
and published contracts; the earlier probe below records its design scope.
No installation, deployment or approval occurred when these examples were authored.

## Authoritative facts and examples

| Manifest | Meaning |
| --- | --- |
| [Simulation design](configuration-sim.yaml) | Intended simulation configuration and selected source/compiled/native revisions |
| [Proposed field design](configuration-field.yaml) | Distinct applicability and illustrative hardware assumptions; no field-use approval |
| [Synthetic assembly](configuration-built.yaml) | A separately identified built-state fixture referencing the exact intended baseline |
| [Synthetic deployment](configuration-deployed.yaml) | A separately identified deployed-state fixture referencing the exact built baseline |
| [Host substitution](configuration-replacement.yaml) | Proposed change of the host assumption from 120 to 200 W with the same local resource ID |

Configuration records own stage, applicability and slot selections. Baseline records
own the selected resource revisions and their source mappings. Equipment records will
own actual manufacturer/instance data, observations will own assembly/commissioning
evidence, and assurance records will own adequacy and authorization decisions.
These domains must not be hidden in a baseline's free-form approval field.

All manifests explicitly say `design-probe-only`. Stage and observation basis must
agree; built/deployed fixtures need a pinned predecessor. This bounded chain does
not establish that actual hardware was assembled or software deployed. The field
proposal reuses simulation requirements as design reference material; it is not a
completed field requirement set. OPEN-FIELD-LIMITS and the other reference-system
blocking decisions remain unresolved.

The [NASA configuration-management guidance](https://www.nasa.gov/reference/6-5-configuration-management/)
informs the distinction between identified configurations and controlled changes.
Its process is an engineering reference here, not an automatically applicable project
standard or a claim that these fixtures satisfy it.

## Revision and availability rules

Each member names an explicit artifact alias, kind, format, relative resource path,
SHA-256 revision, availability requirement and applicability. Seven selected resources
cover compiled requirements, architecture source, hardware assumptions, the actual
engineering-workflow script, tool version declarations, requirement source and attribute
source. Native Python and generated version JSON retain their canonical formats.
The illustrative hardware facts are YAML and make no manufacturer or measurement claim.

Compiled requirement source locations map explicitly to selected source members and
must match the hashes declared by the compiled artifact. A path match with different
bytes is not valid navigation to that revision. This avoids packaging a compiled
snapshot while silently linking it to a newer working copy.

Slot names and member aliases must be unambiguous. A selection must refer to a declared
member; unknown artifact formats and inapplicable selections fail explicitly. Missing
required resources block availability. Missing optional resources produce a distinct
unknown-availability finding, not a complete evidence claim. Resources are root-relative
and checked for traversal/symlink escape; analysis performs no implicit network fetch.

A complete selection is not evidence sufficiency. Tool version metadata is pinned,
but the examples do not pin actual platform-specific compiler binaries or every native
dependency. A release reproducibility policy must require those resources where needed.
Likewise, a hardware assumption file is not a manufacturer datasheet or a test report.
The baseline can faithfully identify an incomplete engineering configuration.

## Publishing and immutability decision

Authored configuration YAML remains editable working source. TC-2409 must materialize
its selected revision into a compiled, content-addressed snapshot with producer/source
provenance. Publishing must recheck inputs and fail without a successful complete
result on changes or output failure; it must not rewrite an already published revision.
Store it under a revision-derived location or an equivalent immutable artifact store.
Human baseline IDs remain stable names with distinct content revisions.

The fixture pins are actual hashes of current selected bytes. Prior-baseline references
pin the complete predecessor source document in this design probe. A future compiled
consumer must instead use the published compiled contract and its source inventory;
source and compiled digests are different types of revision and cannot be substituted.
There is no hidden YAML normalization or semantic hash in this decision.

Baseline retention must retain the exact manifest and its selected resources or explicit
availability records. Copying a file to another path with identical bytes changes its
locator, not its resource content revision. Losing a file does not erase its recorded
identity; the resolver must report it unavailable. Immutable storage, external retrieval
and access control are operational mechanisms, not properties proven by a hash alone.

## Comparing and interacting

From the root:

```sh
make gcs-design-verify
```

With seed outputs and the schema environment already built:

```sh
build/schema-check-venv/bin/python examples/ground-control-station/design/check-configuration.py
```

The probe stages a fresh temporary resource tree from explicit sources, checks all
five manifests, applies the [negative cases](configuration-cases.yaml), and writes
`build/gcs-design/configuration.md` plus input-pinned `configuration-evidence.json`.
The report shows each configuration's stage/applicability and every observed finding.
It is a disposable inspection view, not a second baseline representation.

The comparator joins explicit slot selections and compares member alias, kind, format,
content pin and applicability/availability policy. It separately reports locator-only,
configuration-stage and environment changes. Comparing the host substitution identifies
only the host slot revision, despite both assumption files keeping HOST-ASSUMPTION.
Comparing designed to built and built to deployed reports the stage changes. These
results identify what changed; they do not infer risk, approval or test validity.

The intended user loop is: edit a proposed selection; compile/resolve exact inputs;
inspect differences from the chosen baseline; query affected analyses/evidence;
record review dispositions against exact subjects; publish a new selected revision.
TC-2422/TC-2423 supply semantic impact and staleness. TC-2418 supplies review authority.
TC-2420 supplies release/operation policy. This probe does not implement those workflows.

## Review subjects and failure interpretations

A review reference must identify the exact baseline artifact revision as well as its
human ID. The probe pins the complete simulation manifest, then changes it while
keeping BL-SIM-DESIGN. The old review subject pin and built-state predecessor reference
both fail to resolve as the new revision. Even an added comment changes the raw source
pin; deciding whether a review can be carried forward is a separate recorded decision.
An editable reviewer label cannot establish identity or authorization.

The invalid-case ledger exercises duplicate slot, missing member, ambiguous alias,
unknown format, changed pin, unavailable required/optional resources, unsafe path,
inapplicability, inconsistent built-state claims, unknown approval field and missing
compiled-source mapping. Incomplete or unavailable inputs cannot become successful
release evidence just because a manifest is structurally valid.

## Implementation boundary and acceptance

TC-2409 should separate configuration source reading, compiled selection/model checks
and bounded resource resolution. It consumes versioned domain artifacts through adapters,
not their source parsers. Requirement/architecture source members are retained resources;
resolving their pins does not rerun domain semantics. The prototype reads known fixture
headers only as pressure checks; it is not a production universal artifact loader.

The bounded first contract needs these identity, applicability, source mapping,
availability and comparison semantics. General variant expressions, automatic asset
discovery, secret storage, approval enforcement and deployment orchestration remain
outside it. A domain requiring richer applicability must define that contract before
extending these fields. Future observed-instance records belong to their equipment/
evidence owners and are selected by the baseline, not duplicated into this schema.

Recorded acceptance on 2026-09-07: Draft 2020-12 self-validation, five stage/variant
manifests, thirteen cases, stage/substitution/locator comparisons, unchanged-ID changed
revision detection, exact source mappings, review/prior-baseline/member tampering checks
all passed. Repeated runs produced identical inspection/evidence outputs. Remaining
physical qualification, executable publication and trusted review mechanisms are
explicitly outside this design completion.

The combined `make gcs-design-verify` passed in the working checkout. A fresh staged
source export with no copied build outputs then passed the full `make verify` gate,
including all 14 maintained JVM groups, nine Node tests, native package and installed
editor checks, the seed, and all three local design probes. This independently
reproduced the actual resource pins and design outputs. Later edits only improve
navigation documentation and close the task cards; the final backlog and schema
checks cover those metadata changes.

## Maintained implementation

TC-2409 implements the independently versioned [configuration contract](../../../specification/0029-configuration-baselines.md)
and [native commands](../../../distribution/configuration.md). Maintained fixtures
are under `../engineering/` and select compiled architecture and compiled predecessor
baselines. The original source-pin probe above retains its dated design scope; it
is not the current compiled contract. Publication retains exact inputs for a new
root, checks existing publications and never repairs them by overwriting a revision.
