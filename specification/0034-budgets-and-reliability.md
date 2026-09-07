# Budgets and reliability 0.1

The [YAML schema](schema/budget-yaml-0.1.json) and
[design decision](../examples/ground-control-station/design/budget.md) define the
bounded calculation profile. The budget model imports compiled equipment and its
exact configuration. The reviewed equipment digest is a separate authored claim.
Equipment analysis findings are propagated with their subjects.
Changes produce a stale-review finding; changing an input requires recalculation.

`mundane-budget compile/check/calculate/analyze/view/compare` uses the common
source/import/output contracts and 0/1/2 exit statuses. `compare` accepts two
compiled inputs and reports whether the old result applies to the new inputs.
Both revisions' pinned resources must remain available. It is a bounded input/result
comparison; broader cross-domain semantic impact remains TC-2422/TC-2423.

## Quantities and evaluation

Parameters have nonnegative finite lower/upper bounds, units, evidence basis and
explanation. Fraction parameters require unit 1 and bounds within zero and one;
this prevents efficiency/usable-energy fractions from exceeding one. Measurements/manufacturer ratings require selected resource evidence.
Equipment rating references must resolve within the selected equipment, match basis
and dimensions, and lie inside the parameter interval. This verifies correspondence,
not the truth or applicability of the manufacturer/measurement claim.

Supported units are 1, W, kW, Wh, kWh, s, ms, h, Mbps, bps, GB, MB, bit. Data units
use decimal prefixes and eight bits per byte. Quantities are normalized to power,
time and data dimensions. Formulas use sum/product (2–16 operands) and
difference/ratio (exactly two). Output units must have the calculated dimensions.
At most 1,000 quantities, depth 64 and dimension exponents of magnitude 8 are
accepted. Magnitudes above 1e100, cyclic/missing inputs, reversed intervals and
divisor intervals containing zero are rejected. No expression string is executed.

Arithmetic uses 34-digit decimal precision with lower bounds rounded toward
negative infinity and upper bounds toward positive infinity. Repeated/correlated
inputs may widen intervals; there is no independence assumption in interval
arithmetic. Limits compare conservative margins: wholly nonnegative is pass,
wholly negative is fail, overlapping zero is indeterminate. Missing PEAK rating
coverage for any selected load makes a peak-power check unavailable. Results expose
all input bases; passing an estimate does not turn assumptions into measurements.

## Conditional reliability

Steady-state repairable availability is MTBF/(MTBF+repair), monotone over input
intervals. Time units are required; MTBF must be positive and repair nonnegative.
All selected load instances appear exactly once. Source/protection/UPS instances
cannot be duplicated as independent loads. One explicit shared power cause is
included once, separately from the residual device failure model.

Unknown independence produces unavailable output. Assumed independence produces a
conditional estimate; supported independence additionally requires pinned evidence
but remains an authored support claim. Model assumptions and limits remain in the
source, and no numerical safety acceptance or release approval is derived.
