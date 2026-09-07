# Safety analysis 0.1

The [published schema](schema/safety-yaml-0.1.json) and
[worked design](../examples/ground-control-station/design/safety.md) define the
bounded safety source/model. The domain envelope, source profile, root/pin checks,
checked output and independent model/source component boundaries follow the
[architecture contract](0028-architecture-and-domain-boundary.md).

Safety owns causes, hazards, controls, assumptions, qualitative severity definitions,
FMEA records and fault-tree events. Compiled requirements, verification-plan activities,
architecture modes/components and configuration baselines are selected explicitly.
The current release accepts no probability field, quantitative failure model or
accepted-risk status. A syntactically valid model can retain major evidence gaps.

Compilation rejects unknown fields, unsupported formats, duplicate IDs, missing or
wrong-kind references, undefined severity, invalid baseline selection, malformed
AND/OR/basic gates, duplicate gate inputs, unreachable events and cycles. Fault-tree
traversal is bounded to depth 128. A shared cause may legitimately appear in multiple
failure effects; it is not treated as independent probability evidence.

Analysis preserves one finding per relevant subject: missing-control, orphan-control,
control-hazard-mismatch, unverified-assumption, residual-risk-unresolved,
residual-risk-review-required and review-stale. ReviewedAgainst records exact selected
revisions separately from current imports. Selection changes do not rewrite review
pins. These findings establish neither risk acceptance nor deployment authorization.

`query` joins one hazard to its controls, requirement allocations and planned activities.
`view` renders those records, assumptions, FMEA and a derived fault-tree diagram with
revision-checked source links. New-domain editor integration and assurance decisions
remain owned by later cards. Compiled consumers run without any YAML reader.
