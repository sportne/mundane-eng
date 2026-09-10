# Semantic change and selected-revision staleness

The experimental change component consumes explicit before/after inventories using
[change-yaml-0.1.json](schema/change-yaml-0.1.json). Each side selects 1–100
compiled artifacts by logical scope, kind, format, root-relative path and SHA-256.
Retain both revisions and all their selected resources. No directory discovery or
implicit latest revision is used. Scope cannot change family; paths cannot alias
different scopes. Duplicate, malformed or mismatched selections fail validation.

Each domain owns a finite top-level classification of its compiled engineering
values. Requirements include attribute values/declarations; plans include planned
coverage; work includes corrective records. Array order is meaningful. Differences
retain normalized values, classifications and both source origins. Imported pins
are compared separately from values. Equal values and imports with changed source
provenance are presentation-only; this does not make compiled bytes identical.

The after-inventory graph follows explicit owner dependencies. Configuration also
owns previous-baseline and compiled-member dependencies; operations owns corrective
work selections. An exact selected-input pin mismatch makes its direct consumer
stale against that inventory. Reachability is a prospective review path, not proof
of stale downstream evidence or acceptance. A changed consumer requires reassessment;
an unchanged consumer whose own pins remain selected has no direct staleness, even
if a more distant input changed. Domain readiness commands remain authoritative for
their own analyses, signatures and explicit evaluation time.

Unknown families are represented with unknown support. Omitted dependencies and
unclassified fields make coverage incomplete. Removed selections remain visible.
Queries return deterministic shortest paths, cycle-safe, bounded to 1–64 edges.
The consumer uses compiled adapters without YAML source readers. Explicit native
source resources selected by a configuration still require their retained bytes.

Reports link only to a matching pinned source revision and otherwise identify
unavailable navigation. Contracts/pins fail with exit 1, unavailable resources with
2; a valid analysis containing stale or unknown states returns 0. Results establish
no engineering or release authorization. Existing mundane-impact behavior is
unchanged. This artifact-level model does not infer dependency meaning from prose,
compute whole-program effects or propagate accepted dispositions automatically.

See the [worked design](../examples/ground-control-station/design/change.md),
[native evidence](../examples/ground-control-station/engineering/change.md) and
[commands](../distribution/change.md).
