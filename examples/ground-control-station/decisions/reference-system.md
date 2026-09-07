# Reference-system decision — TC-2401

Accepted on 2026-09-07 for the engineering design probe. The authoritative structured
brief is [reference-system.yaml](reference-system.yaml); its local profile is example
planning data, not a maintained compiler input. Role names identify responsibility,
not the identity or approval of an appointed individual.

```text
operator/observer -> UI -> GCS intent/state service -> link adapter -> synthetic autopilot
                          |                          (in-memory events)
                          +-> event recorder
purchased server + switch + link equipment <- UPS <- external mains
```

The operator service owns command eligibility; the adapter owns interpretation of
external messages; the recorder owns observation completeness. Purchased hardware
and wiring are design records. The aircraft/autopilot is outside the GCS boundary.
The YAML scenario matrix assigns one accountable role and contributing roles for
all twelve scenarios in the original plan, including combined failures and changes.

## Decisions and rationale

Select a synthetic GCS-AP-SIM-0.1 profile, using MAVLink 2 common messages as interface
vocabulary. This pins the example's application assumptions without pretending to
select or qualify a real autopilot. The eventual simulator must not open an aircraft
transport. A real protocol adapter requires a separate concrete stack/dialect revision
and conformance evidence under OPEN-FAILSAFE.

A command acknowledgement and achieved vehicle state are separate observations;
the [MAVLink command protocol](https://mavlink.io/en/services/command.html) explicitly
distinguishes accepted commands from completed actions. Fixture session/request IDs
are application metadata and must not be described as protocol-provided guarantees.
Retry behavior applies only within a validated session and to requests whose
idempotence is part of the selected profile.

[Heartbeat guidance](https://mavlink.io/en/services/heartbeat.html) leaves timing
choices to the communication channel. The brief therefore makes heartbeat and
telemetry-age thresholds separate project choices, with explicit simulated rates
and rationale. They are not manufacturer ratings or operational safety limits.
Clock uncertainty contributes to age's upper bound; unknown timing prevents a
freshness claim. The state evaluator gates new intent immediately on invalidation;
the display has a separately bounded update interval.

[Message signing](https://mavlink.io/en/guide/message_signing.html) informs future
trust assumptions. It does not supply this example with operator identity, permission
management or encryption. Those remain an owned unresolved integration decision.

## Engineering obligations and tailoring

The selected obligations are project policies for traceability, separate review,
configuration provenance, bounded delivery and security. No product jurisdiction,
aircraft category or regulatory approval has been supplied. The example therefore
makes no automatic aviation or software safety-standard compliance claim.
Before a physical deployment, OPEN-FIELD-LIMITS requires an applicability decision.
NASA configuration-management guidance is design inspiration for later baseline
work, not a NASA requirement imposed on this civilian fixture.

Unresolved field limits, real failsafe behavior, actual hardware and trusted identity
have named role owners and explicitly block the corresponding claim or activity.
They do not block authoring the simulation-only engineering example. Even a future
passing simulation cannot close those decisions by implication.

## Acceptance review and downstream use

Reviewed the brief against S01–S12: each has exactly one accountable role, explicit
expected behavior and supporting owners. All eight numerical limits are accepted
only for simulation with rationale; all four unresolved decisions have an owner and
blocking scope. No silent defaults or implied field approvals are present.

Reproduce the structural review with the repository's YAML fixture reader:

```sh
python3 - <<'PY'
import sys
sys.path.insert(0, 'scripts')
import source_yaml
from pathlib import Path
b = source_yaml.loads(Path('examples/ground-control-station/decisions/reference-system.yaml').read_text())
assert {s['id'] for s in b['scenarios']} == {f'S{i:02d}' for i in range(1, 13)}
assert all(s['accountable'] in b['roles'] and all(r in b['roles'] for r in s['contributors']) for s in b['scenarios'])
assert all(x['owner'] in b['roles'] for group in ['limits', 'obligations', 'unresolved'] for x in b[group])
assert all(x['status'] == 'accepted-for-simulation-only' and x['rationale'] for x in b['limits'])
print('PASS bounded reference-system review')
PY
```

The [seed](../seed/README.md) derives supported requirements/plans/work items from
this brief. The [ownership decision](../design/ownership.md) records additional
artifact boundaries; the [architecture design](../design/architecture.md) defines
concrete modes and interface records. Those designs must preserve these responsibility and claim limits.
