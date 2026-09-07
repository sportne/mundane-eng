# Equipment and wiring design decision

TC-2412 selects one YAML source for part definitions, selected instances, catalog
ratings, ports/pin roles and cable conductor mappings. BOM quantities come from
instances; diagrams come from cables. Connection facts are never reauthored for a
second view. Stable IDs are local, and allocations explicitly import architecture
hardware components. The source selects an exact configuration baseline.

Each part retains a local SHA-256-selected datasheet plus the revision reviewed for
its ratings. A changed file fails identity checking; an explicitly selected newer
file with an old review produces a stale-review finding. Manufacturer rating,
measurement and assumption bases stay distinct. This fixture uses a synthetic
catalog and only assumptions, visibly labelled as unsuitable for procurement.
Substitutions record the installed/design instance, previous part ID and reason.
Previous parts need not stay in the active BOM; retained baseline history owns them.

The source schema is closed. Ports own power/network/ground signal, direction,
connector profile, voltage range, required connection and pin roles. Cable mappings
must preserve pin role, cover each required pin and identify real endpoints. A
single input cannot have competing feeds. Output fanout is a connectivity statement,
not an ampacity result. Declared part roles identify source, protection, UPS and
load. Every UPS/load power route must reach a source through a protection instance;
a shared feed is explicitly visible. Ground ports require a route to the declared
source ground. Power cycles, incompatible connectors/ranges and reversed direction
are rejected. Missing required connections and stale reviews are analysis findings.

This topology check does not size fuses or wires, model mains installations, prove
fault-current capability, or certify grounding/electrical safety. Source EARTH is an
explicit unverified bonding assumption. Port profiles are a bounded GCS vocabulary;
full ECAD/PCB/mechanical models stay in their native format behind future explicit
adapters. No CAD format is falsely claimed supported by this implementation.

The planned equipment model/CLI depends on compiled architecture/configuration and
shared source infrastructure. Compile/check/analyze/bom/wiring/view outputs use
common provenance and parser-free consumers. Design intent and synthetic installation
are distinct. Instance allocation must resolve to a hardware component; a selected
configuration's stage determines whether installation wording is applicable. Editor
support and integrated review UI remain their existing later cards.

Run `build/schema-check-venv/bin/python examples/ground-control-station/design/check-equipment.py`.
It independently validates source syntax and derives `build/gcs-design/equipment.md`,
then exercises incompatible voltage/connector, disconnected protection and a
substitution with stale supporting evidence. Native implementation will add pin-role,
ground-route, duplicate feed and source-navigation checks against this decision.
