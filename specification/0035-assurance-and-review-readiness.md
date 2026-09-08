# Assurance and review readiness 0.1

Owner: `assurance-model`; source: `assurance`. Author YAML using the closed
[schema](schema/assurance-yaml-0.1.json). The independently versioned compiled
`mundane-assurance-0.1` envelope follows the existing domain provenance contract.
No existing domain format changes. See the
[GCS decision](../examples/ground-control-station/design/assurance.md) for rationale
and standards profiles.

Claims form a reachable directed acyclic graph, at most 1,000 records per collection
and 64 support levels. Typed imports resolve requirements, baseline, safety controls,
procedures, evidence and software. Procedure, safety and software selections must
name the same exact configuration artifact. Leaves recompute every selected run;
aggregates require all children. Missing/nonpassing runs are unsupported, changed
procedure revisions stale, and passing plus failing observations disputed. Stored
verdicts cannot supply support. Native evidence input pins and adapter identities
remain checked by their owner.

Every claim needs at least one current adequate review. Any current adverse review
blocks readiness; older entries remain visible. Reviews/waivers bind SHA-256 of
canonical project JSON containing values without reviews/waivers and scope-sorted
exact imports, plus the configuration artifact SHA-256. `subjects` returns these
values. There is no signature over its own envelope pointer.

The bounded native signature profile is DSSE with payload type
`application/vnd.mundane.review+json`, Ed25519, original payload bytes and standard
PAE. Decoded payload must equal the authored record excluding `signature`.
Caller-selected public JWK Set keys require unique `kid`, `kty: OKP`, `crv: Ed25519`,
a 32-byte `x`, no private `d`, and `use: sig` / `key_ops: [verify]` if present.
Unknown JSON extension fields in the native envelope/key remain native-owned.
No case-selected trust key, network discovery, PKI or revocation support is implied.
The matching key ID must equal the reviewer, required role must match and reviewer
must differ from authored authors. This is an explicit local policy, not proof of
personhood, competence or organizational authority.

A record applies only at `issuedAt <= evaluationTime < expiresAt`. Time is an
explicit ISO instant supplied by the caller. Reports retain stale, expired,
identity-unverified and role-policy-unmet records. Open waivable obligations may be
waived by a current signed record; waived is distinct from fulfilled. Fulfilment
requires passing selected evidence for the exact configuration. Not-applicable
needs rationale. Waivers never override absent/failed claim evidence or objections.

`localReadiness` requires every claim's support and review checks plus no open
obligation. `authorization` is always `none`. Report trust-set digest, exact subjects,
records and limitations. A local ready result is not certification or release
acceptance. Structural/pin errors return 1, operational missing-resource errors 2;
a successfully computed blocked report returns 0. YAML-free compiled consumers are
supported. Views navigate revision-checked source points; unavailable sources do
not invent a current link. Editor assistance is tracked separately.
