# Research 0033: YAML Source Representation Decision

Status: Decision recorded; successor specification and implementation remain future work

Date: 2026-09-04

Task: [TC-1105](../roadmap/closed/task-1105-compare-yaml-and-custom-requirement-source.yaml)

## Decision

Adopt a constrained YAML profile as the target for the next requirements source
specification. Preserve the requirement semantic model and human-authored IDs.
Proceed through specification, interpretation, formatting and migration cards;
this decision does not change the current 0.2 contract or production tools.

The [comparison](../experiments/0025-yaml-source-comparison/comparison.md) shows
that ordinary YAML can carry all selected requirement values, including multiple
requirements per file and ordered prose/math, while reusing parsing and structural
schema facilities. YAML is not inherently a simpler language for authors: this
candidate is more verbose and demands care with scalars. The practical advantage
is avoiding ownership of a custom surface grammar while retaining ownership of
requirements semantics, validation and source-safe tooling.

## Evidence and limits

- Eleven paired requirements match exactly at the normalized semantic boundary.
- Twenty-eight pressure cases record syntax, schema and domain outcomes, including
  an intentionally unequal unquoted-hash example that a schema cannot repair.
- Twelve matched workflow outcomes cover editing, wrapping, relationships,
  comments, requirement splitting, moves, reorder/combination and actual Git merges.
- The Python round-trip path and conservative Java line-ending path retain all
  thirteen tested comments and are idempotent. The two richer Java emitter paths
  lose comments; one also fails idempotence. Neither is selected as a formatter.
- SnakeYAML Engine 3.1.1 compiles with Java 21 and builds under GraalVM CE 21.0.2.
  Forty-three JVM/native parser/emitter comparisons agree. Native schema/domain
  validation and editor integration have not been tested.
- The unchanged maintained `make verify` suite passed locally with GraalVM CE
  21.0.2. Experiment golden replay passed separately. These checks do not establish
  exhaustive YAML conformance or independent usability evidence.

The [experiment README](../experiments/0025-yaml-source-comparison/README.md)
provides pinned setup/replay commands, fixture provenance and exact environment.
Expected maintenance savings are an inference from reusable infrastructure, not a
measured cost estimate. There was no external author session, performance study
or production YAML integration in this task.

## Alternatives considered

**Retain custom syntax:** it gives compact prose/math and avoids implicit YAML
scalar resolution. These are demonstrated benefits, and sunk implementation work
is not the reason to discount them. For the broader text-based engineering
ecosystem, they do not presently outweigh standard parsing/schema/editor reuse.

**Accept arbitrary YAML:** rejected. Default resolvers differ, duplicate-key
handling matters, and aliases/tags/streams add behavior the requirements model
does not need. The schema alone cannot express source selection, source-set
references, decoded character rules or safe comment-preserving edits.

**Defer the entire representation decision:** unnecessary after bounded semantic
and parser/native feasibility checks. Specific unresolved profile choices belong
in a written successor specification before implementation. A failed fidelity case
there must trigger an explicit revision of this decision/profile.

**Maintain two permanent authoring formats:** not selected. Temporary migration
support needs a bounded contract and retirement conditions. Compiled JSON remains
derived; neither YAML serialization nor a digest introduces another ID system.

## Work enabled

1. [TC-1106](../roadmap/closed/task-1106-specify-yaml-requirement-source-profile.yaml):
   settle the normative profile, scalar styles, version/discovery and migration
   contract using the [clause outline](../experiments/0025-yaml-source-comparison/specification-outline.md).
2. [TC-1107](../roadmap/closed/task-1107-interpret-and-validate-yaml-requirement-source.yaml):
   implement selected parsing/profile/domain behavior with source locations and
   Java/native evidence.
3. [TC-1108](../roadmap/closed/task-1108-format-yaml-source-without-content-loss.yaml):
   implement bounded source-preserving formatting; retain write/output safeguards.
4. [TC-1109](../roadmap/closed/task-1109-migrate-yaml-examples-and-conformance-material.yaml):
   provide reviewed migration, conformance material and maintained examples/docs.

TC-1103 can use the experimental adapter after its other prerequisite, TC-1102,
is complete; it must label the adapter provisional and avoid freezing experimental
source syntax into compiled contracts. TC-1302 now also depends on TC-1106 before
choosing attribute-schema placement. Attribute ownership and type scope remain
TC-1301 decisions. Requirement compilation, imports, linking and analyses remain
separate from authoring notation.

This supersedes the custom-source preference in
[Research 0007](0007-provisional-source-representation-decision.md) as a future
design direction, while preserving that record and current versioned contracts.
It also reopens the dependency-free implementation preference in
[Research 0011](0011-maintained-implementation-lineage.md) for the narrowly scoped
parser dependency; TC-1107 must record licensing, pinning and packaging effects.
No unrelated engineering metamodel or new artifact language follows from YAML.
