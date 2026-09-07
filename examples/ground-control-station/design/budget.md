# Quantities, budgets and reliability decision

TC-2414 selects YAML parameters with explicit units, lower/upper bounds, basis,
explanation and optional native evidence/selected equipment rating reference.
Manufacturer ratings, measurements and assumptions cannot silently substitute for
each other. Non-assumed inputs require pinned evidence; equipment references must
identify the selected instance/part/rating and enclose its declared value. The
reviewed equipment digest is separate from the currently imported revision.

A bounded expression graph supports sum, difference, product and ratio, with
explicit output units and named input IDs. Unit conversion uses power, time and data
dimensions, decimal SI prefixes, and eight bits per byte. Units are 1, W/kW,
Wh/kWh, s/ms/h, bps/Mbps, bit/MB/GB. There is no arbitrary code evaluation, symbolic
solver or unit-string interpreter. Cycles, unknown inputs, excessive graph depth,
dimension mismatch and division through zero reject the model. Decimal interval
arithmetic propagates uncertainty conservatively; no point estimate hides a range.

Checks compare worst/best bounds with a selected limit: pass, fail or indeterminate.
Margins retain units. A peak-power check also derives the required load instances
from equipment and detects omitted PEAK ratings among the calculation's actual
leaf inputs. Missing peak coverage makes the check unavailable. Stale equipment
reviews and changed assumptions remain visible alongside recalculated estimates.
Power/UPS runtime, network/latency, compute-capacity fractions and storage all have
worked examples. Compute fractions apply only to the explicitly stated workload.

Reliability is a bounded repairable series model: availability MTBF/(MTBF+repair).
Unique load members represent residual device failure; one explicit shared power
cause is included once. Common power is not also treated as an independent device.
Unknown independence yields unavailable results. Assumed independence produces a
conditional estimate; supported independence requires pinned evidence, retaining its
authored claim status. The model excludes transient recovery, duty cycles, scheduled
maintenance and unmodelled common causes. It does not calculate safety probabilities.

The first fixture uses 90 W peak, 120 Wh nameplate energy, 0.8 usable fraction and
0.9 efficiency: 86.4 Wh usable and 0.96 h runtime. A 200 W replacement host exceeds
the 120 W limit and falls below 0.5 h runtime. Independent Decimal reference checks
also derive 15 Mbps and 7.2 GB. Increasing shared-power repair time lowers the
conditional availability estimate. All source values remain visibly hypothetical.

`check-budget.py` produces `build/gcs-design/budget-reference.json` and exercises
mixed units, missing peak, exhausted margin and repair sensitivity. Implementation
will add interval boundary, formula-cycle, exact native evidence, stale import and
compiled-consumer checks. Calculation models depend on compiled equipment and
configuration; their YAML source reader remains separate. Native numeric resources
retain their own format through selected local resources; no spreadsheet engine or
scientific programming language is added. Editor/release integration stays in its
later cards.
