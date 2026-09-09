# GCS semantic change decision

TC-2422 compares two explicit inventories of compiled artifact revisions. Logical
scope identifies the artifact across selections; path, kind, format and SHA-256
identify its actual bytes. Human selections use YAML. The compiled change envelope
retains those inputs; no source parser belongs in the comparison consumer.

Each domain owns a finite mapping from its value fields to change classifications.
Requirements, plans and work items use their existing serialized adapters. Newer
domains expose the same bounded policy through their own model. Unknown fields or
unsupported families remain unknown, never unchanged or supported by inference.
Compare normalized values separately from source locations, compiler metadata and
exact import selection. Lists preserve authored order unless the owning domain
already normalizes it. Human IDs do not establish revision equality.

Explicit compiled imports form artifact-level prospective edges from input to
consumer. This is a deliberately bounded granularity: no claim of field-level
independence within one artifact. Native resource selections remain owned values;
changes to a binary, BOM, scan, datasheet or backup affect their selecting owner.
The graph does not infer relationships from matching names or prose. Unselected
imports remain visible as incomplete comparison coverage. The existing prospective
mundane-impact contract remains independently available and unchanged.

## Meaning, revision and decisions

Report three separate things:

- A changed normalized value: domain classifications and exact JSON pointers.
- An exact selected dependency mismatch: the consumer still pins a different
  revision than the after inventory selects. This is computed stale support, even
  when the changed input has the same ID or only different source presentation.
- Prospective reachability: explain input-to-consumer paths that may need review.
  Reachability never proves stale evidence, satisfaction or accepted disposition.

A consumer is not automatically stale merely because some upstream node is reachable.
Only its explicit selected pin mismatch establishes that direct stale state. Changes
to the consumer itself require reassessment; a newly compiled consumer does not
magically inherit old test or review adequacy. Existing domain evaluators still own
observed results, current signatures, waivers and release readiness. Keep their
assessments distinct from the change report. Unknown changes are conservative
review findings. Neither comparison nor a connected graph authorizes a release.

## Worked matrix

[change-cases.yaml](change-cases.yaml) specifies telemetry timing, host power, cable,
software/dependency, procedure and reviewer changes, plus formatting and unknown
families. The native implementation must compile real GCS changes, preserve old
selected bytes, compare inventories and test the expected classifications and paths.
It must demonstrate an exact downstream pin mismatch, an unaffected independent
artifact, a rebuilt consumer, and retained old evidence. Missing/tampered resources,
duplicate scopes, incompatible kinds, bounds and unsafe paths reject input.

## Representation and interaction

The closed draft source contains id, before and after selections. New independently
versioned mundane-change compile/check/analyze/query/view commands consume it.
Queries select a logical scope and bounded traversal depth; deterministic explanations
include source origins and unavailable navigation when sources are absent. Analysis
is reproducible without YAML. Source-linked engineering views will compose these
results with existing native domain assessments under explicit evaluation time and
caller-selected public keys. Editor work will present working-copy diagnostics
separately from exact compiled revision checks. No universal authored view language,
new native provenance format or automatic acceptance is introduced.
