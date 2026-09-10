# Engineering review commands

Build and exercise the GCS views with make review-verify using the
[maintained environment](build-verification.md). Commands:

    build/maintained/mundane-review analyze --root build/gcs-review change.json 2026-09-08T12:00:00Z trust.jwks.json all all
    build/maintained/mundane-review view --root build/gcs-review change.json 2026-09-08T12:00:00Z trust.jwks.json configuration-software PLAN-STARTUP
    build/maintained/mundane-review view --root build/gcs-review change-substitution.json 2026-09-08T12:00:00Z trust.jwks.json all all

The last two arguments select an inventoried configuration scope (or all) and
an operations plan ID (or all). Operational records are filtered; shared context
and full change coverage remain visible. Time and caller public keys are explicit.
The command accepts compiled change inventories, not stored verdicts or an authored
view language. No YAML parser, network fetch or external report asset is required.

JSON and Markdown output identify selected revisions, source origins, domain-owned
analyses, direct staleness, prospective paths and unresolved work. Matching source
bytes enable navigation; missing/changed engineering origins disable those links.
Native resources explicitly selected in a configuration remain required bytes.

Input contracts/pins and oversized output return 1; unavailable resources/output
return 2. A valid report with blocked or unknown findings returns 0. Output is
limited to 16 MiB; retained input bounds are those of the domain snapshot boundary.
The report is not release authorization. See the
[contract](../specification/0038-engineering-review-views.md) and
[worked evidence](../examples/ground-control-station/engineering/review.md).
