# Equipment and connectivity 0.1

The [source schema](schema/equipment-yaml-0.1.json) and
[design decision](../examples/ground-control-station/design/equipment.md) define
parts, ratings, instances, cables, substitutions and their engineering limits.
`mundane-equipment` supports compile/check/analyze/bom/wiring/view using the common
compiled envelope, exact imports, source locations and exit statuses.

Part IDs are unique; instances select existing parts; each part owns unique ratings
and ports/pins. Referenced architecture allocations must identify hardware. The
selected baseline is preserved through the compiled import; a synthetic installation
requires a built or deployed configuration. This reference's part list is design
intent, distinct from procurement or physical observation.

The closed connector profiles are dc24-two-pin (power), ethernet-rj45 (network)
and earth-lug (ground). The model checks directions, contained voltage range,
complete pin mappings, matching pin roles, duplicate feeds and network port reuse.
Power fanout is allowed; multiple input routes require a later explicit model.
Every load/UPS must reach a source through protection. Power cycles are rejected.
Required ground ports must connect through ground cables to a source ground.
The source bonding assumption and actual protection ratings remain unverified.

Missing connections, unavailable grounding routes, unprotected paths and stale
reviewed datasheets are findings. Invalid identities, incompatible endpoints,
malformed connectivity and changed selected resource bytes reject compilation or
inspection. A path change alone never approves a replacement part. Substitutions
identify the affected instance, former part ID and authored reason.

BOM quantities count selected instances. Wiring derives from the same cables with
allocated diagram node IDs and source-linked connection tables. Compiled consumers
can operate without YAML source; navigation becomes unavailable when source bytes
are absent or changed. Datasheet resources remain necessary and hash checked.
Native CAD, ampacity, fuse sizing, fault-current and construction-ready electrical
validation remain outside this bounded model. Independent component versions live
in versions.properties; editor integration remains a later card.
