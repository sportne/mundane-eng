# Verification planning fixture

The YAML source in `source/plan.yaml` defines 57 coverage assertions for the vaccine
monitoring requirements. `fixtures/` contains the compiled plan and explicitly
selected baseline/current requirement snapshots used by boundary tests and the
[verification guide](../../distribution/verification.md). Regenerate the plan with
`mundane-plan --root experiments/0028-verification-contract experiments/0028-verification-contract/source`.
The requirement snapshots intentionally differ for RDS-002 and SYS-009.

`make plan-yaml-verify` checks source semantics, provenance and JVM/native parity.
`make test` checks linking, review analysis and delivery failures. Full verification
uses `make verify`. No TSV source or unused historical result dump is retained.
