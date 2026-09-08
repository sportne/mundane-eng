# Assurance commands

Build with the [maintained prerequisites](build-verification.md):

```sh
make assurance-verify
build/maintained/mundane-assurance analyze --root build/gcs-assurance assurance.json 2026-09-08T12:00:00Z trust.jwks.json
build/maintained/mundane-assurance view --root build/gcs-assurance waived.json 2026-09-08T12:00:00Z trust.jwks.json
build/maintained/mundane-assurance query --root build/gcs-assurance assurance.json 2026-09-08T12:00:00Z trust.jwks.json CLAIM-STATE
```

`compile --root ROOT --imports SELECTION.json SOURCE.yaml` emits compiled JSON.
`check` and `subjects` take an artifact; `analyze` and `view` additionally take
explicit evaluation time and caller-selected public JWKS path; `query` adds a
claim ID. Output/diagnostics use stdout/stderr and common 0/1/2 semantics. A computed
blocked report is successful execution, not a command failure.

The [contract](../specification/0035-assurance-and-review-readiness.md) defines the
DSSE/Ed25519/JWK profile. `subjects` supplies exact hashes for externally signed
review records. The tool never generates production identities or grants release
authority. The fixture signer uses an ephemeral private key outside the project;
only its public trust selection and signed envelopes remain in generated evidence.

`build/gcs-assurance/assurance.md` navigates the GCS claims and open field-validation
obligation. `waived-analysis.json` demonstrates a time-limited synthetic waiver,
with `authorization: none`; `disputed.json` retains a contradictory signed review.
Rebuilding the same selected source/import/native bytes is deterministic. Freshly
signing with a different ephemeral key intentionally changes the review artifact.
