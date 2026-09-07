# Compile plans, resolve imports and inspect verification coverage

Three independently installed commands implement the first engineering companion
workflow. The plan compiler reads YAML. The linker and analyzer consume compiled JSON
without a requirements parser or YAML library at runtime. Build with Java 21/GraalVM Native Image:

```sh
make native-plan native-link native-verification
```

The checked-in pilot contract fixture provides a complete invocation:

```sh
build/maintained/mundane-plan --root experiments/0028-verification-contract \
  experiments/0028-verification-contract/source > build/compiled-plan.json
build/maintained/mundane-link --root experiments/0028-verification-contract \
  --plan experiments/0028-verification-contract/fixtures/plan.json \
  experiments/0028-verification-contract/fixtures/imports.json > build/linked.json
build/maintained/mundane-verify --root experiments/0028-verification-contract \
  --plan experiments/0028-verification-contract/fixtures/plan.json \
  experiments/0028-verification-contract/fixtures/imports.json > build/verification.json
```

The final command intentionally returns 1: RDS-002 and SYS-009 have changed since
the selected baseline. It produces a complete analysis with 57 planned coverage
assertions and two review-stale rows. The plan compilation reproduces the compiled
plan fixture used by the next commands. For your own workflow, place generated
artifacts under an explicit workspace root and make import paths relative to that
root; retain original source revisions for pinned baselines.

Use --context to select an exact plan context. Analysis exit 0 means complete with
no stale/uncovered findings; 1 means complete with findings; 2 means analysis was
prevented or output failed. The linker uses 1 for invalid linking and 2 for operational
failure. The plan compiler uses 1 for source nonconformance and 2 for operational or
invocation failure. Inspect exit status before consuming any redirected artifact;
an output failure can leave a prefix. Help/version are standalone options.

[Local imports](../specification/0014-local-artifact-imports-0.1.md) define scope,
pins, snapshot checks and provenance. [Verification planning](../specification/0015-verification-planning-0.1.md)
defines the independently selected YAML source, compiled plan, coverage and review
semantics. It does not execute activities, store evidence or declare requirements
satisfied. Other engineering authoring formats remain independent decisions.

These are separate native targets, outside the historical three-command trial archive.
Requirements validate/format/trace/compile commands remain independently usable.
`make plan-yaml-verify` checks public plan compilation; `make test` and the
attribute/impact workflow targets check linking and analysis. All are included in
`make verify`. No release publication is involved.
