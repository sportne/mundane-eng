# Research 0050: Complete workflow regression corpus

Task: [TC-1501](../roadmap/closed/task-1501-extend-artifact-workflow-regression-corpora.yaml).

## Decision and scope

Extend the complete edit/rebuild loop with a bounded maintained corpus, while
retaining implementation-specific regressions in their owning suites. The prior
checks already covered many individual failure modes. The missing evidence was
source-to-report reproduction across both requirements representations and whether
the safety assertions detect deliberate weakening of implementation rules.

[Experiment 0033](../experiments/0033-workflow-regressions/README.md) records the
inventory and exact gate. Five existing source sets provide small and medium cases;
12 replayable seeds run in both source modes. No project schemas are assumed.
The legacy 0.1 fixtures run in compatible custom-0.2 mode; no new 0.1 selector or
stability promise is introduced. Existing YAML, syntax, diagnostic, formatting,
compiled-output and SARIF goldens remain in the complete verification gate.

The loop checks explicit pins, contexts, incomplete/unknown artifacts, comment
edits and file moves, normative changes, ID corrections and explicit new baseline
selection. Every analyzed assertion points to the authored TSV row, selected
artifact digests match actual inputs, and displayed records trace to source.
The renderer is still the bounded experimental view; no publishing framework or
new authoritative metadata source was added. Existing pilot source is reused;
optional specification dogfooding was unnecessary to fill this coverage gap.

## Recorded evidence

Five reports have reviewed complete-output hashes and expected coverage counts.
Each is deleted and rebuilt from an immutable committed input archive. The initial
recorded input revision is `136364b`; reruns record their actual HEAD separately.
All 12 seeds pass in custom and YAML forms. An ID correction is not an implicit
alias: even after correcting plan references, selecting the old baseline still
fails until an explicit new baseline is chosen.

All six isolated Java mutants compile and are killed by public behavior. The
[mutation record](../experiments/0033-workflow-regressions/results/mutations.json)
contains expected and observed results for parser, validator, resolver, compiler
publication and analyzer review-basis mutations. Compilation failures or crashes
are not counted as detection. No unexpected generated-source failure was found;
the replay protocol retains future failures for reduction into owning regressions.

The seeded witnesses are synthetic, and the extra review plan is a regression
fixture. These checks establish no human feedback, engineering approval, external
ReqIF interoperability or platform-wide source-format policy.

The first clean full-gate run caught an inventory omission: the new recovery
fixtures were outside the conventional invalid directories, and the new valid
SARIF example was absent from the formatter's maintained source-set list. The
fixtures were classified under invalid/custom and invalid/yaml without changing
their bytes, and the valid SARIF source was added to the owning formatter suite.
The completeness assertion is retained; invalid-source behavior remains covered
by the recovery and SARIF suites.

## Full-gate completion

A new clean detached checkout of candidate
`3a13bf1c9c6460310f527108b9c7cc557ffa55b4`, with no prior build output, ran
`scripts/run-ci-verification.sh` successfully. The wrapper invoked the complete
`make verify` gate and both deliberate-failure checks, finishing with exit 0 in
approximately 196.49 seconds. The source tree remained clean afterward.
[Saved full-gate output](../experiments/0033-workflow-regressions/results/clean-verify.log)
and [environment](../experiments/0033-workflow-regressions/results/clean-environment.txt)
retain actual commands and versions, with terminal escapes/trailing whitespace
removed. The local evidence branch retains both verification candidates. The final
completion commit adds documentation and task status to the verified implementation.

Seventeen JVM groups passed. Formatter coverage is 31 source sets and 65 files;
compiled, YAML/schema, SARIF, artifact, plan, report, package and executable
isolation gates all passed. Both seeded source modes and all five clean corpus
rebuilds passed. Six non-equivalent mutants were detected in 8.81 seconds. The
invalid-example injection failed at test in 1.73 seconds and the schema injection
failed at yaml-schema-verify in 0.08 seconds, with exact input restoration. These
are recorded run timings, not performance thresholds or optimization claims.

Environment: Ubuntu 24.04 x86-64/WSL2, GraalVM CE/Javac/Native Image 21.0.2,
Python 3.12.3, Ruby 3.2.3, GCC 13.3.0 and glibc 2.39. This card ran the same
wrapper used by hosted CI locally; no new hosted run or push was needed to execute
the batch, and none is claimed here.
