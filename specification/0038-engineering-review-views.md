# Derived engineering review

The independently versioned mundane-review command consumes a compiled change
inventory, explicit UTC evaluation time, caller-selected public JWKS and two
filters. It has no authored source format or stored-verdict input. The before/after
inventory selects every compiled revision; report data is derived on each run.

The baseline filter is either all or an inventoried configuration scope. It selects
matching configuration-bound artifacts and operational candidates. Requirements,
plans, architecture, corrective work and artifacts without an explicit configuration
remain shared context. The scenario filter is either all or an operations plan ID.
It filters operational plans, executions, incidents and candidates; shared engineering
context remains visible. These are explicit bounded filters, not prose matching or
a general view query language. Full inventory change coverage remains visible even
when the displayed operational subset is smaller.

The report composes context/interfaces, hazard/control ownership, procedure/run
facts, BOM/cables, budget calculations, software provenance/security, assurance and
operations readiness using the owning compiled adapters. Every section identifies
its selected revision and source origins; every owned analysis preserves the same
result as its CLI. Native signatures use caller trust and evaluation time, recorded
alongside a trust-set digest. No remote resource is fetched or executed.

A consumer's computed analysis describes its retained selected inputs. If that
consumer is stale against the after inventory, the report labels it stale and lists
reassessment work; an old locally-ready verdict cannot be read as readiness for
changed inputs. Prospective change paths and unknown coverage remain separate.
Unknown families are explicit, and missing selected resources fail. Unresolved work
includes domain findings, open obligations, blocked candidates and stale selections.

Markdown tables escape authored text and use deterministic source links only when
the retained source digest matches. Removing optional human engineering origins
does not prevent rendering; those links become unavailable. Native source resources
explicitly selected as baseline members remain required bytes even though no YAML
parser is on the report consumer's classpath. Reports use no external web assets,
scripts, diagram service or general authored view language.

The GCS workflow demonstrates software control ownership, selected build/run
support, blocked upgrade/anomaly candidates, host-cable change invalidation, and
baseline/startup filters. It compares CLI and report objects and tests unavailable
navigation, unknown extensions and reconstruction in another retained root. It is
fixture evidence, not release authorization or aviation qualification.

Output is bounded to 16 MiB. See [commands](../distribution/review.md) and
[worked evidence](../examples/ground-control-station/engineering/review.md).
